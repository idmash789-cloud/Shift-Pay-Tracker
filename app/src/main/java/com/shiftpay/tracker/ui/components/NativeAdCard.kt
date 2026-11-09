package com.shiftpay.tracker.ui.components

import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.shiftpay.tracker.util.AdMobHelper

/**
 * Native ad between content items, clearly separated and labelled "Ad".
 * It never pretends to be a shift, a job or any other content.
 * If the ad fails to load, nothing is shown — no placeholder, no gap.
 */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Card(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("Ad", modifier = Modifier.padding(start = 12.dp, top = 8.dp))
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                NativeAdView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    val headline = TextView(ctx).apply { textSize = 16f }
                    val body = TextView(ctx).apply { textSize = 13f }
                    val cta = TextView(ctx).apply { textSize = 14f }
                    addView(headline); addView(body); addView(cta)
                    this.headlineView = headline
                    this.bodyView = body
                    this.callToActionView = cta
                }
            },
            update = { nativeAdView ->
                AdLoader.Builder(context, AdMobHelper.NATIVE_TEST_ID)
                    .forNativeAd { ad: NativeAd ->
                        (nativeAdView.headlineView as? TextView)?.text = ad.headline
                        (nativeAdView.bodyView as? TextView)?.text = ad.body
                        (nativeAdView.callToActionView as? TextView)?.text = ad.callToAction
                        nativeAdView.setNativeAd(ad)
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) { /* show nothing */ }
                    })
                    .build()
                    .loadAd(AdRequest.Builder().build())
            }
        )
    }
}
