package de.szalkowski.activitylauncher.entrypoint

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@RunWith(AndroidJUnit4::class)
class PackageChangeReceiverTest {
    private lateinit var receiver: PackageChangeReceiver
    private val packageRepository: PackageRepository = mock()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        receiver = PackageChangeReceiver()
        receiver.packageRepository = packageRepository
    }

    @Test
    fun testPackageAddedTriggersLoadDetails() = runBlocking {
        val packageName = "com.test.app"
        val intent = Intent(Intent.ACTION_PACKAGE_ADDED).apply {
            data = Uri.parse("package:$packageName")
        }

        receiver.onReceive(context, intent)

        // Give some time for the coroutine in receiver to run
        var verified = false
        for (i in 1..10) {
            try {
                verify(packageRepository).loadDetails(packageName)
                verified = true
                break
            } catch (e: Throwable) {
                Thread.sleep(50)
            }
        }
        if (!verified) verify(packageRepository).loadDetails(packageName)
    }

    @Test
    fun testPackageRemovedTriggersRemovePackage() = runBlocking {
        val packageName = "com.test.app"
        val intent = Intent(Intent.ACTION_PACKAGE_REMOVED).apply {
            data = Uri.parse("package:$packageName")
            putExtra(Intent.EXTRA_REPLACING, false)
        }

        receiver.onReceive(context, intent)

        // Give some time for the coroutine in receiver to run
        var verified = false
        for (i in 1..10) {
            try {
                verify(packageRepository).removePackage(packageName)
                verified = true
                break
            } catch (e: Throwable) {
                Thread.sleep(50)
            }
        }
        if (!verified) verify(packageRepository).removePackage(packageName)
    }

    @Test
    fun testPackageRemovedWithReplacingDoesNotTriggerRemove() = runBlocking {
        val packageName = "com.test.app"
        val intent = Intent(Intent.ACTION_PACKAGE_REMOVED).apply {
            data = Uri.parse("package:$packageName")
            putExtra(Intent.EXTRA_REPLACING, true)
        }

        receiver.onReceive(context, intent)

        verify(packageRepository, never()).removePackage(packageName)
    }
}
