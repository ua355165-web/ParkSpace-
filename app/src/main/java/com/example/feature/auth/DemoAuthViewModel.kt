package com.example.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.di.ParkSpaceAppContainer
import com.example.core.domain.model.UserProfile
import com.example.core.domain.repository.DemoAuthRepository
import com.example.core.domain.repository.DemoAuthResult
import com.example.core.domain.repository.DemoRegisterRequest
import com.example.core.domain.repository.DemoSignInRequest
import com.example.demo.sandbox.DemoSandboxActionResult
import com.example.demo.sandbox.DemoSessionManager
import com.example.feature.auth.domain.DemoAuthValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthFormMode {
  SIGN_IN,
  REGISTER,
}

data class DemoAuthUiState(
  val role: UserRole,
  val mode: AuthFormMode,
  val fullName: String = "",
  val email: String = "",
  val password: String = "",
  val confirmPassword: String = "",
  val businessAlias: String = "",
  val selectedCity: SupportedCity = SupportedCity.LAHORE,
  val isSubmitting: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null,
  val signedInUserForDualRole: UserProfile? = null,
  val companionRoleAlreadyExists: Boolean = false,
  val dualRoleBusinessAlias: String = "",
)

class DemoAuthViewModel(
  private val role: UserRole,
  startWithRegister: Boolean,
  private val authRepository: DemoAuthRepository,
  private val sessionManager: DemoSessionManager,
) : ViewModel() {

  private val _uiState =
    MutableStateFlow(
      initialUiState(
        role = role,
        startWithRegister = startWithRegister,
        sessionManager = sessionManager,
      )
    )
  val uiState: StateFlow<DemoAuthUiState> = _uiState.asStateFlow()

  init {
    refreshDualRoleAvailability()
  }

  private fun refreshDualRoleAvailability() {
    val active = sessionManager.session.value.activeUser
    if (
      active == null ||
        active.role == UserRole.SUPER_ADMIN ||
        active.role == role ||
        !active.isSelfRegisteredDemoAccount
    ) {
      _uiState.update {
        it.copy(signedInUserForDualRole = null, companionRoleAlreadyExists = false)
      }
      return
    }
    viewModelScope.launch {
      val exists = authRepository.hasAccountForEmailAndRole(active.emailMasked, role)
      _uiState.update {
        it.copy(
          signedInUserForDualRole = active,
          companionRoleAlreadyExists = exists,
        )
      }
    }
  }

  fun setMode(mode: AuthFormMode) {
    _uiState.update { it.copy(mode = mode, errorMessage = null, successMessage = null) }
  }

  fun onFullNameChange(value: String) {
    _uiState.update { it.copy(fullName = value, errorMessage = null, successMessage = null) }
  }

  fun onEmailChange(value: String) {
    _uiState.update { it.copy(email = value, errorMessage = null, successMessage = null) }
  }

  fun onPasswordChange(value: String) {
    _uiState.update { it.copy(password = value, errorMessage = null, successMessage = null) }
  }

  fun onConfirmPasswordChange(value: String) {
    _uiState.update { it.copy(confirmPassword = value, errorMessage = null, successMessage = null) }
  }

  fun onBusinessAliasChange(value: String) {
    _uiState.update { it.copy(businessAlias = value, errorMessage = null, successMessage = null) }
  }

  fun onDualRoleBusinessAliasChange(value: String) {
    _uiState.update {
      it.copy(dualRoleBusinessAlias = value, errorMessage = null, successMessage = null)
    }
  }

  fun onSelectCity(city: SupportedCity) {
    _uiState.update { it.copy(selectedCity = city, errorMessage = null) }
  }

  fun activateOrSwitchCompanionRole(onAuthenticated: (UserRole) -> Unit) {
    val current = _uiState.value
    val activeUser = current.signedInUserForDualRole ?: sessionManager.session.value.activeUser
    if (current.isSubmitting || activeUser == null) return

    _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
    viewModelScope.launch {
      val result =
        authRepository.activateOrSwitchCompanionRole(
          currentSignedInUserId = activeUser.id,
          targetRole = current.role,
          businessAlias = current.dualRoleBusinessAlias,
        )
      handleAuthResult(result, onAuthenticated)
    }
  }

  fun submit(onAuthenticated: (UserRole) -> Unit) {
    val current = _uiState.value
    if (current.isSubmitting) return

    if (!current.role.allowsPublicRegistration || current.role == UserRole.SUPER_ADMIN) {
      _uiState.update {
        it.copy(errorMessage = "Private Super Admin accounts cannot use public sign-in or signup.")
      }
      return
    }

    val precheckError =
      when (current.mode) {
        AuthFormMode.SIGN_IN ->
          DemoAuthValidator.validateSignIn(current.role, current.email, current.password)
        AuthFormMode.REGISTER ->
          DemoAuthValidator.validateRegistration(
            role = current.role,
            fullName = current.fullName,
            email = current.email,
            password = current.password,
            confirmPassword = current.confirmPassword,
          )
      }
    if (precheckError != null) {
      _uiState.update { it.copy(errorMessage = precheckError, successMessage = null) }
      return
    }

    _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
    viewModelScope.launch {
      val result =
        when (current.mode) {
          AuthFormMode.SIGN_IN ->
            authRepository.signInLocalAccount(
              DemoSignInRequest(
                role = current.role,
                email = current.email,
                password = current.password,
              )
            )
          AuthFormMode.REGISTER ->
            authRepository.registerLocalAccount(
              DemoRegisterRequest(
                role = current.role,
                fullName = current.fullName,
                email = current.email,
                password = current.password,
                confirmPassword = current.confirmPassword,
                preferredCity = current.selectedCity,
                businessAlias = current.businessAlias,
              )
            )
        }

      handleAuthResult(result, onAuthenticated)
    }
  }

  private fun handleAuthResult(
    result: DemoAuthResult,
    onAuthenticated: (UserRole) -> Unit,
  ) {
    when (result) {
      is DemoAuthResult.Rejected -> {
        _uiState.update {
          it.copy(isSubmitting = false, errorMessage = result.reason, successMessage = null)
        }
      }
      is DemoAuthResult.Success -> {
        when (val activation = sessionManager.activateAuthenticatedUser(result.user)) {
          is DemoSandboxActionResult.Rejected -> {
            _uiState.update {
              it.copy(isSubmitting = false, errorMessage = activation.reason, successMessage = null)
            }
          }
          is DemoSandboxActionResult.Success -> {
            _uiState.update {
              it.copy(
                isSubmitting = false,
                errorMessage = null,
                successMessage = result.message,
              )
            }
            onAuthenticated(result.user.role)
          }
        }
      }
    }
  }

  companion object {
    private fun initialUiState(
      role: UserRole,
      startWithRegister: Boolean,
      sessionManager: DemoSessionManager,
    ): DemoAuthUiState {
      val active = sessionManager.session.value.activeUser
      val isCompanionContext =
        active != null &&
          active.role != UserRole.SUPER_ADMIN &&
          active.role != role &&
          active.isSelfRegisteredDemoAccount
      return DemoAuthUiState(
        role = role,
        mode = if (startWithRegister) AuthFormMode.REGISTER else AuthFormMode.SIGN_IN,
        fullName = if (isCompanionContext) active.fullName else "",
        email = if (isCompanionContext) active.emailMasked else "",
        selectedCity = active?.city ?: sessionManager.session.value.selectedCity,
        signedInUserForDualRole = if (isCompanionContext) active else null,
      )
    }
  }

  class Factory(
    private val role: UserRole,
    private val startWithRegister: Boolean,
    private val container: ParkSpaceAppContainer,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      require(modelClass.isAssignableFrom(DemoAuthViewModel::class.java)) {
        "Unknown ViewModel class: ${modelClass.name}"
      }
      return DemoAuthViewModel(
        role = role,
        startWithRegister = startWithRegister,
        authRepository = container.demoAuthRepository,
        sessionManager = container.demoSessionManager,
      ) as T
    }
  }
}
