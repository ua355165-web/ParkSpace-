package com.example.demo.sandbox

import android.content.Context
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.domain.model.UserProfile
import com.example.core.domain.repository.UserProfileRepository
import com.example.demo.seed.DemoSeedDataset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PersistedDemoSessionSnapshot(
  val activeUserId: String? = null,
  val isAdminSandboxUnlocked: Boolean = false,
  val selectedCity: SupportedCity = SupportedCity.LAHORE,
)

interface DemoSessionPersistence {
  fun readSnapshot(): PersistedDemoSessionSnapshot

  fun writeSnapshot(snapshot: PersistedDemoSessionSnapshot)
}

class InMemoryDemoSessionPersistence : DemoSessionPersistence {
  private var current = PersistedDemoSessionSnapshot()

  override fun readSnapshot(): PersistedDemoSessionSnapshot = current

  override fun writeSnapshot(snapshot: PersistedDemoSessionSnapshot) {
    current = snapshot
  }
}

class SharedPrefsDemoSessionPersistence(context: Context) : DemoSessionPersistence {
  private val prefs =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  override fun readSnapshot(): PersistedDemoSessionSnapshot {
    val activeUserId = prefs.getString(KEY_ACTIVE_USER_ID, null)?.takeIf { it.isNotBlank() }
    val adminUnlocked = prefs.getBoolean(KEY_ADMIN_UNLOCKED, false)
    val cityName = prefs.getString(KEY_SELECTED_CITY, SupportedCity.LAHORE.name)
    val city =
      SupportedCity.entries.firstOrNull { it.name == cityName } ?: SupportedCity.LAHORE
    return PersistedDemoSessionSnapshot(
      activeUserId = activeUserId,
      isAdminSandboxUnlocked = adminUnlocked,
      selectedCity = city,
    )
  }

  override fun writeSnapshot(snapshot: PersistedDemoSessionSnapshot) {
    prefs
      .edit()
      .putString(KEY_ACTIVE_USER_ID, snapshot.activeUserId)
      .putBoolean(KEY_ADMIN_UNLOCKED, snapshot.isAdminSandboxUnlocked)
      .putString(KEY_SELECTED_CITY, snapshot.selectedCity.name)
      .apply()
  }

  private companion object {
    const val PREFS_NAME = "parkspace_demo_session"
    const val KEY_ACTIVE_USER_ID = "active_user_id"
    const val KEY_ADMIN_UNLOCKED = "admin_sandbox_unlocked"
    const val KEY_SELECTED_CITY = "selected_city"
  }
}

data class DemoSessionState(
  val activeUser: UserProfile? = null,
  val isAdminSandboxUnlocked: Boolean = false,
  val selectedCity: SupportedCity = SupportedCity.LAHORE,
  val isLocalTestingSandboxOnly: Boolean = true,
) {
  val activeRole: UserRole?
    get() = activeUser?.role

  val isSignedIn: Boolean
    get() = activeUser != null
}

sealed interface DemoSandboxActionResult {
  data object Success : DemoSandboxActionResult

  data class Rejected(val reason: String) : DemoSandboxActionResult
}

/**
 * Local Demo session manager supporting user-created Demo accounts, session persistence across
 * restarts, and explicit Private Demo Admin Sandbox isolation.
 */
class DemoSessionManager(
  private val userRepository: UserProfileRepository,
  private val persistence: DemoSessionPersistence = InMemoryDemoSessionPersistence(),
) {
  private val _session =
    MutableStateFlow(
      DemoSessionState(selectedCity = persistence.readSnapshot().selectedCity)
    )
  val session: StateFlow<DemoSessionState> = _session.asStateFlow()

  fun selectCity(city: SupportedCity) {
    _session.update { current ->
      val next = current.copy(selectedCity = city)
      persistState(next)
      next
    }
  }

  /**
   * Restores a previously signed-in user session across app restart.
   * Never silently logs into any preset Customer or Host account.
   */
  suspend fun restorePersistedSession() {
    val snapshot = persistence.readSnapshot()
    val savedUserId = snapshot.activeUserId
    if (savedUserId.isNullOrBlank()) {
      _session.update {
        it.copy(
          activeUser = null,
          isAdminSandboxUnlocked = false,
          selectedCity = snapshot.selectedCity,
        )
      }
      return
    }

    val user =
      if (savedUserId == PRIVATE_DEMO_ADMIN_ID && snapshot.isAdminSandboxUnlocked) {
        DemoSeedDataset.privateDemoAdminUser
      } else {
        userRepository.getUserById(savedUserId)
      }
    val allowedRestore =
      user != null &&
        user.isDemoAccount &&
        ((user.role == UserRole.SUPER_ADMIN && snapshot.isAdminSandboxUnlocked) ||
          (user.role != UserRole.SUPER_ADMIN && user.isSelfRegisteredDemoAccount))

    if (!allowedRestore || user == null) {
      val cleared =
        DemoSessionState(
          activeUser = null,
          isAdminSandboxUnlocked = false,
          selectedCity = snapshot.selectedCity,
          isLocalTestingSandboxOnly = true,
        )
      _session.value = cleared
      persistState(cleared)
      return
    }

    _session.value =
      DemoSessionState(
        activeUser = user,
        isAdminSandboxUnlocked = user.role == UserRole.SUPER_ADMIN && snapshot.isAdminSandboxUnlocked,
        selectedCity = snapshot.selectedCity,
        isLocalTestingSandboxOnly = true,
      )
  }

  /**
   * Activates a freshly registered or credential-verified local Demo account.
   */
  fun activateAuthenticatedUser(user: UserProfile): DemoSandboxActionResult {
    if (!user.isDemoAccount || !user.isSelfRegisteredDemoAccount) {
      return DemoSandboxActionResult.Rejected(
        "Only registered local Demo accounts can sign in."
      )
    }
    if (user.role == UserRole.SUPER_ADMIN) {
      return DemoSandboxActionResult.Rejected(
        "Private Super Admin cannot authenticate via public Demo login."
      )
    }
    val next =
      DemoSessionState(
        activeUser = user,
        isAdminSandboxUnlocked = false,
        selectedCity = user.city,
        isLocalTestingSandboxOnly = true,
      )
    _session.value = next
    persistState(next)
    return DemoSandboxActionResult.Success
  }

  /**
   * Internal helper preserved for Milestone 1-3 unit/Robolectric inventory tests.
   */
  suspend fun switchToDemoUser(userId: String): DemoSandboxActionResult {
    val user =
      userRepository.getUserById(userId)
        ?: return DemoSandboxActionResult.Rejected("Demo account '$userId' not found.")

    if (!user.isDemoAccount) {
      return DemoSandboxActionResult.Rejected("Only local Demo accounts can be used in Demo Mode.")
    }

    if (user.role == UserRole.SUPER_ADMIN) {
      return DemoSandboxActionResult.Rejected(
        "Private Demo Admin requires explicit sandbox gate acknowledgement."
      )
    }

    val next =
      DemoSessionState(
        activeUser = user,
        isAdminSandboxUnlocked = false,
        selectedCity = user.city,
        isLocalTestingSandboxOnly = true,
      )
    _session.value = next
    persistState(next)
    return DemoSandboxActionResult.Success
  }

  /**
   * Unlocks the Private Demo Admin Sandbox ONLY when explicitly acknowledged
   * as a local testing tool. Public registration of SUPER_ADMIN is always forbidden.
   */
  suspend fun unlockPrivateDemoAdminSandbox(
    acknowledgedLocalTestingOnly: Boolean,
  ): DemoSandboxActionResult {
    if (!acknowledgedLocalTestingOnly) {
      return DemoSandboxActionResult.Rejected(
        "You must acknowledge that Demo Admin Sandbox is for local testing only."
      )
    }

    val adminUser = DemoSeedDataset.privateDemoAdminUser

    _session.update { current ->
      val next =
        current.copy(
          activeUser = adminUser,
          isAdminSandboxUnlocked = true,
          isLocalTestingSandboxOnly = true,
        )
      persistState(next)
      next
    }
    return DemoSandboxActionResult.Success
  }

  /**
   * Enforces that SUPER_ADMIN can never be created via public registration.
   */
  fun validatePublicRegistrationRole(requestedRole: UserRole): DemoSandboxActionResult {
    if (!requestedRole.allowsPublicRegistration || requestedRole == UserRole.SUPER_ADMIN) {
      return DemoSandboxActionResult.Rejected(
        "Private Super Admin accounts cannot be created via public registration."
      )
    }
    return DemoSandboxActionResult.Success
  }

  fun signOutDemoSandbox() {
    _session.update { current ->
      val next =
        DemoSessionState(
          activeUser = null,
          isAdminSandboxUnlocked = false,
          selectedCity = current.selectedCity,
          isLocalTestingSandboxOnly = true,
        )
      persistState(next)
      next
    }
  }

  private fun persistState(state: DemoSessionState) {
    persistence.writeSnapshot(
      PersistedDemoSessionSnapshot(
        activeUserId = state.activeUser?.id,
        isAdminSandboxUnlocked = state.isAdminSandboxUnlocked,
        selectedCity = state.selectedCity,
      )
    )
  }

  companion object {
    const val PRIVATE_DEMO_ADMIN_ID = "demo-admin-private"
  }
}
