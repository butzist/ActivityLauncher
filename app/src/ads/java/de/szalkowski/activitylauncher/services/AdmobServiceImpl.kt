package de.szalkowski.activitylauncher.services

import android.app.Activity
import android.view.View
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdmobServiceImpl @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
) : AdmobService {

    override fun initialize(activity: Activity) {
        MobileAds.initialize(context) {
            val adView = activity.findViewById<AdView>(de.szalkowski.activitylauncher.R.id.ad_view)
            val adContainer = activity.findViewById<View>(de.szalkowski.activitylauncher.R.id.ad_container)

            adView?.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    adContainer?.visibility = View.VISIBLE
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    adContainer?.visibility = View.GONE
                }
            }

            adView?.loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
        }

        try {
            val consentInformation = UserMessagingPlatform.getConsentInformation(context)
            if (consentInformation.isConsentFormAvailable) {
                loadConsentForm(activity)
            }
        } catch (e: Exception) {
            // Handle exception - GDPR not available
        }
    }

    private fun loadConsentForm(activity: Activity) {
        try {
            UserMessagingPlatform.loadConsentForm(
                context,
                { form ->
                    try {
                        form.show(activity) { }
                    } catch (e: Exception) {
                        // Handle error
                    }
                },
                { exception ->
                    // Handle error
                },
            )
        } catch (e: Exception) {
            // Handle exception
        }
    }

    override fun showPrivacyOptionsDialog(activity: Activity) {
        try {
            UserMessagingPlatform.loadConsentForm(
                context,
                { form ->
                    form.show(activity) { }
                },
                { exception ->
                    Snackbar.make(
                        activity.findViewById(android.R.id.content),
                        "Unable to load consent form",
                        Snackbar.LENGTH_SHORT,
                    ).show()
                },
            )
        } catch (e: Exception) {
            Snackbar.make(
                activity.findViewById(android.R.id.content),
                "Unable to load consent form",
                Snackbar.LENGTH_SHORT,
            ).show()
        }
    }

    override val isPrivacyOptionsRequired: Boolean
        get() = try {
            UserMessagingPlatform.getConsentInformation(context).privacyOptionsRequirementStatus ==
                com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        } catch (e: Exception) {
            false
        }

    override val isEnabled: Boolean
        get() = true
}
