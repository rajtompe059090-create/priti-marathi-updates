package com.example.ui.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdMobConstants {
    const val BANNER_TEST_AD_UNIT_ID = "ca-app-pub-6146868530948467/2163486647"
    const val INTERSTITIAL_TEST_AD_UNIT_ID = "ca-app-pub-6146868530948467/9123646663"
}

class InterstitialAdManager(private val context: Context) {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    fun loadAd() {
        if (interstitialAd != null || isLoading) return
        isLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            AdMobConstants.INTERSTITIAL_TEST_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    Log.d("AdMobHelper", "Interstitial test ad loaded successfully.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    Log.w("AdMobHelper", "Interstitial test ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun showAd(activity: Activity, onAdDismissedOrFailed: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadAd() // Preload next
                    onAdDismissedOrFailed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    loadAd() // Preload next
                    onAdDismissedOrFailed()
                }
            }
            ad.show(activity)
        } else {
            // Not ready or failed, never block the user
            loadAd()
            onAdDismissedOrFailed()
        }
    }
}

@Composable
fun BottomBannerAd(
    modifier: Modifier = Modifier
) {
    if (androidx.compose.ui.platform.LocalInspectionMode.current) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(SurfaceDark)
                .border(1.dp, BorderSubtle),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Ad Banner (Preview)",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        return
    }

    var isAdLoaded by remember { mutableStateOf(false) }
    var adFailed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                try {
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = AdMobConstants.BANNER_TEST_AD_UNIT_ID
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                super.onAdLoaded()
                                isAdLoaded = true
                                adFailed = false
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                super.onAdFailedToLoad(error)
                                isAdLoaded = false
                                adFailed = true
                                Log.w("AdMobHelper", "Banner test ad failed: ${error.message}")
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                } catch (t: Throwable) {
                    adFailed = true
                    android.view.View(context)
                }
            }
        )

        // Clean placeholder overlay if ad is still loading or failed
        if (!isAdLoaded) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceDark)
            ) {
                Text(
                    text = if (adFailed) "Ad उपलब्ध नाही — पुढे सुरू ठेवा." else "Ad loading...",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
