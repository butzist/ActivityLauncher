package de.szalkowski.activitylauncher.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PackageDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var packageDao: PackageDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        packageDao = database.packageDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndGetPackage() = runBlocking {
        val pkg = AppPackageEntity("com.test.app", "Test App", "1.0", null, true, 0)
        packageDao.insertPackage(pkg)
        val loaded = packageDao.getPackage("com.test.app")
        assertNotNull(loaded)
        assertEquals(pkg.name, loaded?.name)
    }

    @Test
    fun insertAndGetWithActivities() = runBlocking {
        val pkg = AppPackageEntity("com.test.app", "Test App", "1.0", null, true, 0)
        val activity = ActivityEntity(id = 0, packageName = "com.test.app", name = "Main", shortCls = "Main", fullCls = "com.test.app.Main", isDefault = true)

        packageDao.insertPackage(pkg)
        packageDao.insertActivities(listOf(activity))

        val loaded = packageDao.getPackageWithActivities("com.test.app")
        assertNotNull(loaded)
        assertEquals(1, loaded?.activities?.size)
        assertEquals("Main", loaded?.activities?.get(0)?.name)
    }
}
