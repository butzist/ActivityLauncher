package de.szalkowski.activitylauncher.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "packages")
data class AppPackageEntity(
    @PrimaryKey val packageName: String,
    val name: String,
    val version: String,
    val iconResourceName: String?,
    val isFullyLoaded: Boolean,
    val lastUpdated: Long,
)

@Entity(
    tableName = "activities",
    foreignKeys = [
        ForeignKey(
            entity = AppPackageEntity::class,
            parentColumns = ["packageName"],
            childColumns = ["packageName"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("packageName")],
)
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val packageName: String,
    val name: String,
    val shortCls: String,
    val fullCls: String,
    val isDefault: Boolean,
    val isPrivate: Boolean,
    val iconResourceName: String?,
)

@Entity(tableName = "favorites", primaryKeys = ["packageName", "className"])
data class FavoriteEntity(
    val packageName: String,
    val className: String,
    val name: String,
    val intentUri: String,
    val iconBundle: ByteArray,
    val launcherPlugin: String?,
    val timestamp: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FavoriteEntity

        if (packageName != other.packageName) return false
        if (className != other.className) return false
        if (name != other.name) return false
        if (intentUri != other.intentUri) return false
        if (!iconBundle.contentEquals(other.iconBundle)) return false
        if (launcherPlugin != other.launcherPlugin) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + className.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + intentUri.hashCode()
        result = 31 * result + iconBundle.contentHashCode()
        result = 31 * result + (launcherPlugin?.hashCode() ?: 0)
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

@Entity(tableName = "recents", primaryKeys = ["packageName", "className"])
data class RecentEntity(
    val packageName: String,
    val className: String,
    val name: String,
    val intentUri: String,
    val iconBundle: ByteArray,
    val launcherPlugin: String?,
    val timestamp: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RecentEntity

        if (packageName != other.packageName) return false
        if (className != other.className) return false
        if (name != other.name) return false
        if (intentUri != other.intentUri) return false
        if (!iconBundle.contentEquals(other.iconBundle)) return false
        if (launcherPlugin != other.launcherPlugin) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + className.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + intentUri.hashCode()
        result = 31 * result + iconBundle.contentHashCode()
        result = 31 * result + (launcherPlugin?.hashCode() ?: 0)
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey val id: String,
    val packageName: String,
    val className: String,
    val name: String,
    val intentUri: String,
    val iconBundle: ByteArray,
    val launcherPlugin: String?,
    val shortcutPlugin: String?,
    val timestamp: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShortcutEntity

        if (id != other.id) return false
        if (packageName != other.packageName) return false
        if (className != other.className) return false
        if (name != other.name) return false
        if (intentUri != other.intentUri) return false
        if (!iconBundle.contentEquals(other.iconBundle)) return false
        if (launcherPlugin != other.launcherPlugin) return false
        if (shortcutPlugin != other.shortcutPlugin) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + className.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + intentUri.hashCode()
        result = 31 * result + iconBundle.contentHashCode()
        result = 31 * result + (launcherPlugin?.hashCode() ?: 0)
        result = 31 * result + (shortcutPlugin?.hashCode() ?: 0)
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
