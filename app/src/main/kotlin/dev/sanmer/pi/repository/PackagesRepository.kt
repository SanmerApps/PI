package dev.sanmer.pi.repository

import android.content.pm.PackageInfo
import kotlinx.coroutines.flow.Flow

interface PackagesRepository {
    fun getInstalledPackagesAsFlow(flags: Int): Flow<List<PackageInfo>>
    fun getPackageInfoAsFlow(packageName: String, flags: Int): Flow<PackageInfo>
    fun loadLabel(packageInfo: PackageInfo): CharSequence
}