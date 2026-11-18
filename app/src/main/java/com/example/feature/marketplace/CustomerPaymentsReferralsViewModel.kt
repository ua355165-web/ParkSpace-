package com.example.feature.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.di.ParkSpaceAppContainer
import com.example.core.domain.model.DemoPaymentTransaction
import com.example.core.domain.model.DemoReferralRecord
import com.example.core.domain.repository.PaymentTransactionRepository
import com.example.core.domain.repository.ReferralMutationResult
import com.example.core.domain.repository.ReferralRepository
import com.example.core.domain.repository.UserProfileRepository
import com.example.demo.sandbox.DemoSessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CustomerPaymentsReferralsUiState(
  val myReferralCode: String = "",
  val availableDemoCreditPkr: Int = 0,
  val appliedReferral: DemoReferralRecord? = null,
  val referralsSent: List<DemoReferralRecord> = emptyList(),
  val transactions: List<DemoPaymentTransaction> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerPaymentsReferralsViewModel(
  private val paymentTransactionRepository: PaymentTransactionRepository,
  private val referralRepository: ReferralRepository,
  private val userProfileRepository: UserProfileRepository,
  private val demoSessionManager: DemoSessionManager,
) : ViewModel() {

  private val _feedback = MutableStateFlow<String?>(null)
  val feedback: StateFlow<String?> = _feedback.asStateFlow()

  val uiState: StateFlow<CustomerPaymentsReferralsUiState> =
    demoSessionManager.session
      .flatMapLatest { session ->
        val userId = session.activeUser?.id
        if (userId == null) {
          flowOf(CustomerPaymentsReferralsUiState())
        } else {
          combine(
            userProfileRepository.observeCustomerProfile(userId),
            paymentTransactionRepository.observeAvailableDemoCreditPkr(userId),
            referralRepository.observeReferralAsReferred(userId),
            referralRepository.observeReferralsByReferrer(userId),
            paymentTransactionRepository.observeTransactionsForCustomer(userId),
          ) { profile, creditPkr, applied, sent, txs ->
            CustomerPaymentsReferralsUiState(
              myReferralCode = profile?.referralCode.orEmpty(),
              availableDemoCreditPkr = creditPkr,
              appliedReferral = applied,
              referralsSent = sent,
              transactions = txs,
            )
          }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = CustomerPaymentsReferralsUiState(),
      )

  fun redeemReferralCode(code: String) {
    val userId = demoSessionManager.session.value.activeUser?.id ?: return
    viewModelScope.launch {
      when (val res = referralRepository.redeemReferralCode(userId, code)) {
        is ReferralMutationResult.Success -> _feedback.value = res.message
        is ReferralMutationResult.Rejected -> _feedback.value = res.reason
      }
    }
  }

  fun dismissFeedback() {
    _feedback.value = null
  }

  class Factory(private val container: ParkSpaceAppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      CustomerPaymentsReferralsViewModel(
        paymentTransactionRepository = container.paymentTransactionRepository,
        referralRepository = container.referralRepository,
        userProfileRepository = container.userProfileRepository,
        demoSessionManager = container.demoSessionManager,
      ) as T
  }
}
