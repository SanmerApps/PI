package dev.sanmer.hidden.compat.content

import android.content.pm.PackageInfo
import android.os.Parcel
import android.os.Parcelable
import dev.sanmer.hidden.compat.utils.readParcelable
import java.io.File

data class ArchiveInfo(
    private val archiveFilePath: String,
    val originatingPackageName: String,
    val archivePackageInfo: PackageInfo,
) : Parcelable {
    val archiveFile: File get() = File(archiveFilePath)

    constructor(parcel: Parcel) : this(
        archiveFilePath = checkNotNull(parcel.readString()),
        originatingPackageName = checkNotNull(parcel.readString()),
        archivePackageInfo = checkNotNull(parcel.readParcelable(PackageInfo::class.java))
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(archiveFilePath)
        parcel.writeString(originatingPackageName)
        parcel.writeParcelable(archivePackageInfo, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ArchiveInfo> {
        override fun createFromParcel(parcel: Parcel): ArchiveInfo {
            return ArchiveInfo(parcel)
        }

        override fun newArray(size: Int): Array<ArchiveInfo?> {
            return arrayOfNulls(size)
        }
    }
}