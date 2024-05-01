package dev.sanmer.pi.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.sanmer.pi.model.IPackageInfo

@Entity(tableName = "packages")
data class PackageInfoEntity(
    @PrimaryKey val packageName: String,
    val authorized: Boolean
) {
    constructor(packageInfo: IPackageInfo) : this(
        packageName = packageInfo.packageName,
        authorized = packageInfo.isAuthorized
    )
}