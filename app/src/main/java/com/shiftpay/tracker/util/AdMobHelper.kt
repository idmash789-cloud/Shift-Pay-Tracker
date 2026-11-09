package com.shiftpay.tracker.util

import android.content.Context
import android.util.DisplayMetrics
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * AdMob helper — TEST ad unit IDs only, never ship production IDs from here.
 * Replace the constants with real production IDs (and remove test devices)
 * before a Play Store release, and fill in app-ads.txt (see docs/app-ads.txt).
 *
 * Placement rules (locked): banner is an ADAPTIVE banner at the BOTTOM of
 * Home / Pay Summary, below the clock in/out buttons — never covering them.
 * Interstitial: at most once per session AND at least 3 minutes apart, only
 * after a completed action (e.g. after viewing a pay summary), never on
 * app open and never while the user is typing or clocking in/out.
 * Rewarded: only for PDF pay-stub export, always opt-in.
 */
object AdMobHelper {

    const val BANNER_TEST_ID = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_TEST_ID = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED_TEST_ID = "ca-app-pub-3940256099942544/5224354917"
    const val NATIVE_TEST_ID = "ca-app-pub-3940256099942544/2247696110"
    const val APP_OPEN_TEST_ID = "ca-app-pub-3940256099942544/9257395921"

    fun initialize(context: Context) {
        MobileAds.initialize(context)
    }

    /** Anchored adaptive banner sized for the current screen width. */
    fun adaptiveBannerSize(context: Context): AdSize {
        val displayMetrics: DisplayMetrics = context.resources.displayMetrics
        val widthPixels = displayMetrics.widthPixels.toFloat()
        val density = displayMetrics.density
        val adWidth = (widthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }

    fun createBannerAdView(context: Context): AdView {
        return AdView(context).apply {
            setAdSize(adaptiveBannerSize(context))
            adUnitId = BANNER_TEST_ID
            loadAd(AdRequest.Builder().build())
        }
    }

    fun loadInterstitial(context: Context, onLoaded: (InterstitialAd?) -> Unit) {
        InterstitialAd.load(
            context,
            INTERSTITIAL_TEST_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    onLoaded(interstitialAd)
                }

                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    onLoaded(null)
                }
            }
        )
    }
}
