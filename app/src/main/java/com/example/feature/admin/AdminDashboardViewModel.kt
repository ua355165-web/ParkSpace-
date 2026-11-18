package com.example.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.config.UserRole
import com.example.core.di.ParkSpaceAppContainer
import com.example.core.domain.model.AdminAuditLogEntry
import com.example.core.domain.model.AdminDisputeQueueItem
import com.example.core.domain.model.AdminListingQueueItem
import com.example.core.domain.model.DemoCommissionRule
import com.example.core.domain.model.DisputeStatus
import com.example.core.domain.model.HostKycQueueItem
import com.example.core.domain.model.PlatformAnalyticsSnapshot
import com.example.core.domain.model.UserProfile
import com.example.core.domain.repository.AdminMutationResult
import com.example.core.domain.repository.AdminRepository
import com.example.demo.sandbox.DemoSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AdminConsoleTab(val label: String) {
  KYC_QUEUE("Host KYC"),
  LISTINGS("Listings"),
  DISPUTES("Disputes"),
  ANALYTICS("Analytics"),
  CONFIG_AND_LOGS("Rules & Logs"),
}

data class AdminConsoleUiState(
  val activeTab: AdminConsoleTab = AdminConsoleTab.KYC_QUEUE,
  val isAuthorizedAdmin: Boolean = false,
  val adminUser: UserProfile? = null,
  val hostKycQueue: List<HostKycQueueItem> = emptyList(),
  val listingQueue: List<AdminListingQueueItem> = emptyList(),
  val disputesQueue: List<AdminDisputeQueueItem> = emptyList(),
  val analytics: PlatformAnalyticsSnapshot? = null,
  val commissionRule: DemoCommissionRule = DemoCommissionRule.DEFAULT,
  val auditLogs: List<AdminAuditLogEntry> = emptyList(),
  val isSubmitting: Boolean = false,
  val actionFeedback: String? = null,
  val errorMessage: String? = null,
)

class AdminDashboardViewModel(
  private val adminRepository: AdminRepository,
  private val demoSessionManager: DemoSessionManager,
  private val nowProvider: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

  private val _activeTab = MutableStateFlow(AdminConsoleTab.KYC_QUEUE)
  private val _isSubmitting = MutableStateFlow(false)
  private val _actionFeedback = MutableStateFlow<String?>(null)
  private val _errorMessage = MutableStateFlow<String?>(null)

  private val queuesFlow =
    combine(
      adminRepository.observeHostKycQueue(),
      adminRepository.observeListingModerationQueue(),
      adminRepository.observeAllDisputesQueue(),
    ) { kyc, listings, disputes ->
      Triple(kyc, listings, disputes)
    }

  private val platformFlow =
    combine(
      adminRepository.observePlatformAnalytics(),
      adminRepository.observeActiveCommissionRule(),
      adminRepository.observeAuditLogs(),
    ) { analytics, rule, logs ->
      Triple(analytics, rule, logs)
    }

  private val uiMetaFlow =
    combine(_activeTab, _isSubmitting, _actionFeedback, _errorMessage) { tab, busy, feedback, err ->
      UiMeta(tab, busy, feedback, err)
    }

  val uiState: StateFlow<AdminConsoleUiState> =
    combine(
      demoSessionManager.session,
      queuesFlow,
      platformFlow,
      uiMetaFlow,
    ) { session, (kyc, listings, disputes), (analytics, rule, logs), meta ->
      val authorized =
        session.activeRole == UserRole.SUPER_ADMIN &&
          session.isAdminSandboxUnlocked &&
          session.activeUser?.id == DemoSessionManager.PRIVATE_DEMO_ADMIN_ID

      AdminConsoleUiState(
        activeTab = meta.tab,
        isAuthorizedAdmin = authorized,
        adminUser = if (authorized) session.activeUser else null,
        hostKycQueue = if (authorized) kyc else emptyList(),
        listingQueue = if (authorized) listings else emptyList(),
        disputesQueue = if (authorized) disputes else emptyList(),
        analytics = if (authorized) analytics else null,
        commissionRule = rule,
        auditLogs = if (authorized) logs else emptyList(),
        isSubmitting = meta.isSubmitting,
        actionFeedback = meta.actionFeedback,
        errorMessage = meta.errorMessage,
      )
    }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AdminConsoleUiState(),
      )

  fun selectTab(tab: AdminConsoleTab) {
    _activeTab.value = tab
  }

  fun dismissFeedback() {
    _actionFeedback.value = null
    _errorMessage.value = null
  }

  fun approveHostKyc(hostUserId: String, note: String) {
    executeAdminMutation { adminId ->
      adminRepository.approveHostKyc(adminId, hostUserId, note, nowProvider())
    }
  }

  fun rejectHostKyc(hostUserId: String, reason: String) {
    executeAdminMutation { adminId ->
      adminRepository.rejectHostKyc(adminId, hostUserId, reason, nowProvider())
    }
  }

  fun approveListing(spaceId: String, note: String) {
    executeAdminMutation { adminId ->
      adminRepository.approveListing(adminId, spaceId, note, nowProvider())
    }
  }

  fun rejectListing(spaceId: String, reason: String) {
    executeAdminMutation { adminId ->
      adminRepository.rejectListing(adminId, spaceId, reason, nowProvider())
    }
  }

  fun updateDisputeStatus(disputeId: String, targetStatus: DisputeStatus, note: String) {
    executeAdminMutation { adminId ->
      adminRepository.updateDisputeStatusAsAdmin(
        actorAdminId = adminId,
        disputeId = disputeId,
        targetStatus = targetStatus,
        note = note,
        nowEpochMs = nowProvider(),
      )
    }
  }

  fun updateCommissionRule(
    percentInput: String,
    minFeeInput: String,
    maxCapInput: String,
    noteInput: String,
  ) {
    val pct = percentInput.trim().toIntOrNull()
    val minFee = minFeeInput.trim().toIntOrNull()
    val maxCap = maxCapInput.trim().toIntOrNull()
    if (pct == null || minFee == null || maxCap == null) {
      _errorMessage.value = "Enter valid whole-number values for commission %, min fee, and max cap."
      _actionFeedback.value = null
      return
    }
    executeAdminMutation { adminId ->
      adminRepository.updateCommissionRule(
        actorAdminId = adminId,
        commissionPercent = pct,
        minFeePkr = minFee,
        maxFeePkr = maxCap,
        note = noteInput,
        nowEpochMs = nowProvider(),
      )
    }
  }

  private fun executeAdminMutation(block: suspend (adminId: String) -> AdminMutationResult) {
    val adminId = demoSessionManager.session.value.activeUser?.id.orEmpty()
    viewModelScope.launch {
      _isSubmitting.value = true
      _actionFeedback.value = null
      _errorMessage.value = null
      when (val result = block(adminId)) {
        is AdminMutationResult.Success -> _actionFeedback.value = result.message
        is AdminMutationResult.Rejected -> _errorMessage.value = result.reason
      }
      _isSubmitting.value = false
    }
  }

  private data class UiMeta(
    val tab: AdminConsoleTab,
    val isSubmitting: Boolean,
    val actionFeedback: String?,
    val errorMessage: String?,
  )

  class Factory(
    private val container: ParkSpaceAppContainer,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      require(modelClass.isAssignableFrom(AdminDashboardViewModel::class.java)) {
        "Unknown ViewModel class: ${modelClass.name}"
      }
      return AdminDashboardViewModel(
        adminRepository = container.adminRepository,
        demoSessionManager = container.demoSessionManager,
      ) as T
    }
  }
}
