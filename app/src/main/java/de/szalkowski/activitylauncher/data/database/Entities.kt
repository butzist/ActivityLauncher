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
)
