package de.szalkowski.activitylauncher.entrypoint

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.szalkowski.activitylauncher.data.packages.PackageDataSource
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@RunWith(AndroidJUnit4::class)
class PackageChangeReceiverTest {
    private lateinit var receiver: PackageChangeReceiver
    private val dataSource: PackageDataSource = mock()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        receiver = PackageChangeReceiver()
        receiver.dataSource = dataSource
    }

    @Test
    fun testPackageAddedTriggersLoadDetails() = runBlocking {
        val packageName = "com.test.app"
        val intent = Intent(Intent.ACTION_PACKAGE_ADDED).apply {
            data = Uri.parse("package:$packageName")
        }

        receiver.onReceive(context, intent)

        Thread.sleep(100)
        verify(dataSource).loadDetails(packageName)
    }

    @Test
    fun testPackageRemovedTriggersRemovePackage() = runBlocking {
        val packageName = "com.test.app"
        val intent = Intent(Intent.ACTION_PACKAGE_REMOVED).apply {
            data = Uri.parse("package:$packageName")
        }

        receiver.onReceive(context, intent)

        Thread.sleep(100)
        verify(dataSource).removePackage(packageName)
    }
}
