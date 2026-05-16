package de.szalkowski.activitylauncher.services

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.intergi.playwiremobile.notifier.PMNotifier
import com.intergi.playwiresdk.PlaywireSDK
import com.intergi.playwiresdk.ads.view.PWViewAd
import com.intergi.playwiresdk.ads.view.banner.PWBannerView
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaywireServiceImpl @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
) : AdService {

    private var isInitialized = false

    override fun initialize(activity: Activity, container: ViewGroup) {
        if (isInitialized) return

        val publisherId = context.getString(R.string.publisher_id)
        val appId = context.getString(R.string.app_id)

        if (publisherId.isEmpty() || appId.isEmpty()) {
            return
        }

        PMNotifier.startConsoleLogger()
        PlaywireSDK.start(publisherId, appId, activity) { success, _ ->
            if (success) {
                isInitialized = true
                setupBanner(activity, container)
            }
        }
    }

    fun setupBanner(activity: Activity, container: ViewGroup) {
        val listener = object : PWViewAd.Listener {
            override fun onViewAdLoaded(ad: PWViewAd) {
                container.visibility = View.VISIBLE
            }

            override fun onViewAdFailedToLoad(ad: PWViewAd) {
                container.visibility = View.GONE
            }

            override fun onViewAdOpened(ad: PWViewAd) {}
            override fun onViewAdClosed(ad: PWViewAd) {}
            override fun onViewAdClicked(ad: PWViewAd) {}
            override fun onViewAdImpression(ad: PWViewAd) {}
        }

        val banner = PWBannerView(activity, "banner-320x50", listener)
        container.removeAllViews()
        container.addView(banner)
        banner.load()
    }
}
