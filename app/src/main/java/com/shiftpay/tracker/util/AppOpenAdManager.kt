package com.shiftpay.tracker.util

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

/**
 * App Open ad — OPTIONAL format. Shown at most once per app session, and
 * only after the startup flow is ready (never blocking first paint).
 * If it fails to load, the app simply continues without it.
 */
class AppOpenAdManager {
    private var appOpenAd: AppOpenAd? = null
    private var shownThisSession = false

    fun load(context: Context) {
        AppOpenAd.load(
            context,
            AdMobHelper.APP_OPEN_TEST_ID,
            AdRequest.Builder().build(),
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) { appOpenAd = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { appOpenAd = null }
            }
        )
    }

    fun showIfAvailable(activity: Activity) {
        if (shownThisSession) return
        val ad = appOpenAd ?: return
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { appOpenAd = null }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { appOpenAd = null }
            override fun onAdShowedFullScreenContent() { shownThisSession = true }
        }
        ad.show(activity)
    }
}
