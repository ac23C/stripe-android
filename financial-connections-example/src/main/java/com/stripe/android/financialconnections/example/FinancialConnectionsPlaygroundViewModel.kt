package com.stripe.android.financialconnections.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.Stripe
import com.stripe.android.confirmPaymentIntent
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetForTokenResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.example.data.BackendRepository
import com.stripe.android.financialconnections.example.data.Settings
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FinancialConnectionsPlaygroundViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val settings = Settings(application)
    private val repository = BackendRepository(settings)
    private val _state = MutableStateFlow(FinancialConnectionsPlaygroundState())
    val state: StateFlow<FinancialConnectionsPlaygroundState> = _state

    private val _viewEffect = MutableSharedFlow<FinancialConnectionsPlaygroundViewEffect?>()
    val viewEffect: SharedFlow<FinancialConnectionsPlaygroundViewEffect?> = _viewEffect

    init {
        _state.update { it.copy(backendUrl = settings.backendUrl) }
    }

    fun startFinancialConnectionsSession(
        merchant: Merchant,
        flow: Flow,
        keys: Pair<String, String>
    ) {
        _state.update { it.copy(status = emptyList()) }
        when (flow) {
            Flow.Data -> startForData(merchant, keys)
            Flow.Token -> startForToken(merchant, keys)
            Flow.PaymentIntent -> startWithPaymentIntent(merchant, keys)
        }
    }

    private fun startWithPaymentIntent(merchant: Merchant, keys: Pair<String, String>) {
        viewModelScope.launch {
            showLoadingWithMessage("Fetching link account session from example backend!")
            kotlin.runCatching {
                repository.createPaymentIntent(
                    country = "US",
                    flow = merchant.flow,
                    keys = keys
                )
            }
                // Success creating session: open the financial connections sheet with received secret
                .onSuccess {
                    _state.update { current ->
                        current.copy(
                            publishableKey = it.publishableKey,
                            loading = true,
                            status = current.status + buildString {
                                append("Payment Intent created: ${it.intentSecret}")
                                appendLine()
                                append("Opening FinancialConnectionsSheet.")
                            }
                        )
                    }
                    _viewEffect.emit(
                        FinancialConnectionsPlaygroundViewEffect.OpenForPaymentIntent(
                            paymentIntentSecret = it.intentSecret,
                            publishableKey = it.publishableKey
                        )
                    )
                }
                // Error retrieving session: display error.
                .onFailure(::showError)
        }
    }

    private fun startForData(merchant: Merchant, keys: Pair<String, String>) {
        viewModelScope.launch {
            showLoadingWithMessage("Fetching link account session from example backend!")
            kotlin.runCatching {
                repository.createLinkAccountSession(
                    flow = merchant.flow,
                    keys = keys
                )
            }
                // Success creating session: open the financial connections sheet with received secret
                .onSuccess {
                    showLoadingWithMessage("Session created, opening FinancialConnectionsSheet.")
                    _state.update { current -> current.copy(publishableKey = it.publishableKey) }
                    _viewEffect.emit(
                        FinancialConnectionsPlaygroundViewEffect.OpenForData(
                            configuration = FinancialConnectionsSheet.Configuration(
                                it.clientSecret,
                                it.publishableKey
                            )
                        )
                    )
                }
                // Error retrieving session: display error.
                .onFailure(::showError)
        }
    }

    private fun startForToken(merchant: Merchant, keys: Pair<String, String>) {
        viewModelScope.launch {
            showLoadingWithMessage("Fetching link account session from example backend!")
            kotlin.runCatching {
                repository.createLinkAccountSessionForToken(
                    flow = merchant.flow,
                    keys = keys
                )
            }
                // Success creating session: open the financial connections sheet with received secret
                .onSuccess {
                    showLoadingWithMessage("Session created, opening FinancialConnectionsSheet.")
                    _state.update { current -> current.copy(publishableKey = it.publishableKey) }
                    _viewEffect.emit(
                        FinancialConnectionsPlaygroundViewEffect.OpenForToken(
                            configuration = FinancialConnectionsSheet.Configuration(
                                it.clientSecret,
                                it.publishableKey
                            )
                        )
                    )
                }
                // Error retrieving session: display error.
                .onFailure(::showError)
        }
    }

    private fun showError(error: Throwable) {
        _state.update {
            it.copy(
                loading = false,
                status = it.status + "Error starting linked account session: $error"
            )
        }
    }

    private fun showLoadingWithMessage(message: String) {
        _state.update {
            it.copy(
                loading = true,
                status = it.status + message
            )
        }
    }

    fun onFinancialConnectionsSheetForTokenResult(result: FinancialConnectionsSheetForTokenResult) {
        val statusText = when (result) {
            is FinancialConnectionsSheetForTokenResult.Completed -> {
                "Completed!\n" +
                    "Session: ${result.financialConnectionsSession}\n" +
                    "Token: ${result.token}\n"
            }

            is FinancialConnectionsSheetForTokenResult.Failed -> "Failed! ${result.error}"
            is FinancialConnectionsSheetForTokenResult.Canceled -> "Cancelled!"
        }
        _state.update { it.copy(loading = false, status = it.status + statusText) }
    }

    fun onFinancialConnectionsSheetResult(result: FinancialConnectionsSheetResult) {
        val statusText = when (result) {
            is FinancialConnectionsSheetResult.Completed -> {
                "Completed!" + result.financialConnectionsSession.toString()
            }

            is FinancialConnectionsSheetResult.Failed -> "Failed! ${result.error}"
            is FinancialConnectionsSheetResult.Canceled -> "Cancelled!"
        }
        _state.update { it.copy(loading = false, status = it.status + statusText) }
    }

    fun onCollectBankAccountLauncherResult(result: CollectBankAccountResult) {
        _state.update { it.copy(status = it.status + "Session attached! Confirming Intent") }
        viewModelScope.launch {
            val statusText = when (result) {
                is CollectBankAccountResult.Completed -> {
                    val confirmedIntent = stripe(
                        _state.value.publishableKey!!
                    ).confirmPaymentIntent(
                        ConfirmPaymentIntentParams.create(
                            clientSecret = requireNotNull(result.response.intent.clientSecret),
                            paymentMethodType = PaymentMethod.Type.USBankAccount
                        )
                    )
                    "Intent Confirmed!: $confirmedIntent"
                }

                is CollectBankAccountResult.Failed -> "Failed! ${result.error}"
                is CollectBankAccountResult.Cancelled -> "Cancelled!"
            }
            _state.update { it.copy(loading = false, status = it.status + statusText) }
        }
    }

    private fun stripe(publishableKey: String) = Stripe(
        getApplication(),
        publishableKey,
        null,
        true,
        emptySet()
    )
}

enum class Merchant(val flow: String) {
    Test("testmode"), Live("mx"), App2App("app2app"), Other("other")
}

enum class Flow {
    Data, Token, PaymentIntent
}

enum class NativeOverride {
    None, Native, Web
}

sealed class FinancialConnectionsPlaygroundViewEffect {
    data class OpenForData(
        val configuration: FinancialConnectionsSheet.Configuration
    ) : FinancialConnectionsPlaygroundViewEffect()

    data class OpenForToken(
        val configuration: FinancialConnectionsSheet.Configuration
    ) : FinancialConnectionsPlaygroundViewEffect()

    data class OpenForPaymentIntent(
        val paymentIntentSecret: String,
        val publishableKey: String
    ) : FinancialConnectionsPlaygroundViewEffect()
}

data class FinancialConnectionsPlaygroundState(
    val backendUrl: String = "",
    val loading: Boolean = false,
    val publishableKey: String? = null,
    val status: List<String> = emptyList()
)
