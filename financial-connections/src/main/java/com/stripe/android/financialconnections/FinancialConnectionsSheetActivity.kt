package com.stripe.android.financialconnections

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.withState
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.FinishWithResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.OpenNativeAuthFlow
import com.stripe.android.financialconnections.features.common.LoadingSpinner
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.utils.argsOrNull
import com.stripe.android.financialconnections.utils.viewModelLazy

internal class FinancialConnectionsSheetActivity : AppCompatActivity(), MavericksView {

    val viewModel: FinancialConnectionsSheetViewModel by viewModelLazy()

    val args by argsOrNull<FinancialConnectionsSheetActivityArgs>()

    private val startBrowserForResult = registerForActivityResult(StartActivityForResult()) {
        viewModel.onBrowserActivityResult()
    }

    private val startNativeAuthFlowForResult = registerForActivityResult(StartActivityForResult()) {
        viewModel.onNativeAuthFlowResult(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (args == null) {
            finish()
        } else {
            viewModel.onEach { postInvalidate() }
            if (savedInstanceState != null) viewModel.onActivityRecreated()
        }

        onBackPressedDispatcher.addCallback {
            finishWithResult(FinancialConnectionsSheetActivityResult.Canceled)
        }
        setContent { Loading() }
    }

    @Composable
    private fun Loading() {
        FinancialConnectionsTheme {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingSpinner()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    /**
     * Handles new intents in the form of the redirect from the custom tab hosted auth flow
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        viewModel.handleOnNewIntent(intent)
    }

    /**
     * handle state changes here.
     */
    override fun invalidate() {
        withState(viewModel) { state ->
            state.viewEffect?.let { viewEffect ->
                when (viewEffect) {
                    is OpenAuthFlowWithUrl -> startBrowserForResult.launch(
                        CreateBrowserIntentForUrl(
                            context = this,
                            uri = Uri.parse(viewEffect.url)
                        )
                    )

                    is FinishWithResult -> finishWithResult(
                        viewEffect.result
                    )

                    is OpenNativeAuthFlow -> startNativeAuthFlowForResult.launch(
                        Intent(
                            this,
                            FinancialConnectionsSheetNativeActivity::class.java
                        ).also {
                            it.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                            it.putExtra(
                                Mavericks.KEY_ARG,
                                FinancialConnectionsSheetNativeActivityArgs(
                                    initialSyncResponse = viewEffect.initialSyncResponse,
                                    configuration = viewEffect.configuration
                                )
                            )
                        }
                    )
                }
                viewModel.onViewEffectLaunched()
            }
        }
    }

    private fun finishWithResult(result: FinancialConnectionsSheetActivityResult) {
        setResult(RESULT_OK, Intent().putExtras(result.toBundle()))
        finish()
    }
}
