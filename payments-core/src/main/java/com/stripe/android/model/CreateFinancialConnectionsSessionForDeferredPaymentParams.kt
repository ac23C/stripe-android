package com.stripe.android.model

internal data class CreateFinancialConnectionsSessionForDeferredPaymentParams(
    val uniqueId: String,
    val initialInstitution: String? = null,
    val manualEntryOnly: Boolean? = null,
    val searchSession: String? = null,
    val verificationMethod: VerificationMethodParam? = null,
    val customer: String? = null,
    val onBehalfOf: String? = null,

    // PaymentIntent only params
    val amount: Int? = null,
    val currency: String? = null,
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            PARAM_UNIQUE_ID to uniqueId,
            PARAM_INITIAL_INSTITUTION to initialInstitution,
            PARAM_MANUAL_ENTRY_ONLY to manualEntryOnly,
            PARAM_SEARCH_SESSION to searchSession,
            PARAM_VERIFICATION_METHOD to verificationMethod?.value,
            PARAM_CUSTOMER to customer,
            PARAM_ON_BEHALF_OF to onBehalfOf,
            PARAM_AMOUNT to amount,
            PARAM_CURRENCY to currency
        )
    }

    companion object {
        private const val PARAM_UNIQUE_ID = "unique_id"
        private const val PARAM_INITIAL_INSTITUTION = "initial_institution"
        private const val PARAM_MANUAL_ENTRY_ONLY = "manual_entry_only"
        private const val PARAM_SEARCH_SESSION = "search_session"
        private const val PARAM_VERIFICATION_METHOD = "verification_method"
        private const val PARAM_CUSTOMER = "customer"
        private const val PARAM_ON_BEHALF_OF = "on_behalf_of"
        private const val PARAM_AMOUNT = "amount"
        private const val PARAM_CURRENCY = "currency"
    }
}

internal enum class VerificationMethodParam(val value: String) {
    Automatic("automatic"),
    Skip("skip"),
    Microdeposits("microdeposits"),
    Instant("instant"),
    InstantOrSkip("instant_or_skip")
}
