package de.szalkowski.activitylauncher.services

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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

    override fun loadBanner(activity: Activity, container: ViewGroup) {
        if (isInitialized) {
            setupBanner(activity, container)
            return
        }

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
                setupBanner(activity, container)
            }
        }
    }

    override fun removeBanner(container: ViewGroup) {
        container.visibility = View.GONE

        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is PWBannerView) {
                child.destroy()
            }
        }
        container.removeAllViews()
    }

    private fun setupBanner(context: Context, container: ViewGroup) {
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

        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER,
        )

        val banner = PWBannerView(context, "banner-320x50", listener)
        container.removeAllViews()
        container.addView(banner, layoutParams)
        banner.load()
    }
}
