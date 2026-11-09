package com.shiftpay.tracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.shiftpay.tracker.util.AdMobHelper

/**
 * Anchored adaptive banner. Always place this BELOW primary actions
 * (e.g. below the Clock In / Clock Out buttons) so ads never cover or
 * push the main controls off screen.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdMobHelper.adaptiveBannerSize(context))
                adUnitId = AdMobHelper.BANNER_TEST_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
