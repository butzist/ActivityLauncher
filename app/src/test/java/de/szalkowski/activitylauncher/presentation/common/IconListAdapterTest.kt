package de.szalkowski.activitylauncher.presentation.common

import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import de.szalkowski.activitylauncher.domain.model.IconInfo
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class IconListAdapterTest {
    private val iconLoader: IconLoader = mock()
    private lateinit var adapter: IconListAdapter

    @Before
    fun setup() {
        adapter = IconListAdapter(iconLoader)
        val icons = listOf(
            IconInfo("com.example:drawable/icon_apple"),
            IconInfo("com.example:drawable/icon_banana"),
            IconInfo("com.example:drawable/icon_cherry"),
        )
        whenever(iconLoader.loadIcons(anyOrNull())).thenReturn(icons)
        adapter.resolve(null)
    }

    @Test
    fun `initial count is correct`() {
        assertEquals(3, adapter.itemCount)
    }
}
