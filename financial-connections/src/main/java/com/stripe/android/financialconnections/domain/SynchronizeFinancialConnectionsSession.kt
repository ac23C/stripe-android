package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject

/**
 * Fetches the [FinancialConnectionsSessionManifest] from the Stripe API to get the hosted auth flow URL
 * as well as the success and cancel callback URLs to verify.
 */
internal class SynchronizeFinancialConnectionsSession @Inject constructor(
    private val financialConnectionsRepository: FinancialConnectionsManifestRepository
) {

    suspend operator fun invoke(
        clientSecret: String,
        applicationId: String
    ): SynchronizeSessionResponse {
        return financialConnectionsRepository.synchronizeFinancialConnectionsSession(
            clientSecret = clientSecret,
            applicationId = applicationId
        )
    }
}
