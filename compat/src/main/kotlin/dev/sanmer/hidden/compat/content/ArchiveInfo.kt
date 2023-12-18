package dev.sanmer.hidden.compat.content

import android.os.Parcel
import android.os.Parcelable
import java.io.File

data class ArchiveInfo(
    private val path: String,
    val packageName: String,
    val originating: String
) : Parcelable {
    val packageFile: File get() = File(path)

    constructor(parcel: Parcel) : this(
        path = parcel.readString() ?: "",
        packageName = parcel.readString() ?: "",
        originating = parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(path)
        parcel.writeString(packageName)
        parcel.writeString(originating)
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