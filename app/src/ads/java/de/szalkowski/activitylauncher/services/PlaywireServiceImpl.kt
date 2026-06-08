package de.szalkowski.activitylauncher.services

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.intergi.playwiresdk.PWNotifier
import com.intergi.playwiresdk.PlaywireSDK
import com.intergi.playwiresdk.ads.view.PWViewAd
import com.intergi.playwiresdk.ads.view.banner.PWBannerView
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.BuildConfig
import de.szalkowski.activitylauncher.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaywireServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AdService {
    private var isInitialized = false
    private var banner: PWBannerView? = null

    override fun initialize(activity: Activity) {
        if (isInitialized) return

        val publisherId = context.getString(R.string.publisher_id)
        val appId = context.getString(R.string.app_id)

        if (publisherId.isEmpty() || appId.isEmpty()) {
            return
        }

        if (BuildConfig.DEBUG) {
            PWNotifier.startConsoleLogger()
        }

        PlaywireSDK.start(publisherId, appId, activity) { success, _ ->
            if (success) {
                isInitialized = true
            }
        }
    }

    override fun loadBanner(context: Context, container: ViewGroup) {
        banner?.destroy()

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

        banner = PWBannerView(context, "banner-320x50", listener)
        container.removeAllViews()
        container.addView(banner)
        banner?.load()
    }

    override fun destroyBanner() {
        banner?.destroy()
        banner = null
    }
}
