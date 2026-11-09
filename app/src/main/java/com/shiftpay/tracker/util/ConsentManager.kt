package com.shiftpay.tracker.util

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * UMP Consent SDK: check whether consent is required when the app opens,
 * show Google's consent form if so, and only then initialise ads.
 * If the consent request fails (e.g. offline — this app is offline-first),
 * ads are still initialised; no consent state is invented.
 */
object ConsentManager {
    fun requestConsent(activity: Activity, onDone: () -> Unit) {
        val consentInformation: ConsentInformation =
            UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    onDone()
                }
            },
            { _ -> onDone() }
        )
    }
}
