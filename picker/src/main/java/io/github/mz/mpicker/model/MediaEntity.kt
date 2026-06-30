package io.github.mz.mpicker.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

/** 按 name 反序列化 MediaType；兼容旧 Parcel 或未知值时回退到 IMAGE。 */
private fun readMediaTypeSafe(name: String): MediaType =
    try {
        MediaType.valueOf(name)
    } catch (e: Exception) {
        MediaType.IMAGE
    }

data class MediaEntity(
    val id: Long,
    val uri: Uri,
    /**
     * 真实文件路径
     */
    val filePath: String?,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val dateAddedSec: Long,
    val width: Int,
    val height: Int,
    val mediaType: MediaType,
    /** 音频专辑 id（仅 AUDIO 有值），用于拼 albumart uri 加载封面 */
    val albumId: Long = 0L,
    val mirrorHorizontal: Boolean = false,
) : Parcelable {

    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isAudio: Boolean get() = mimeType.startsWith("audio/")

    /** 音频专辑封面 uri：无封面时返回 null */
    val albumArtUri: Uri?
        get() = if (isAudio && albumId > 0)
            Uri.parse("content://media/external/audio/albumart/$albumId")
        else null

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        Uri.parse(parcel.readString().orEmpty()),
        parcel.readString(),
        parcel.readString().orEmpty(),
        parcel.readString().orEmpty(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readInt(),
        parcel.readInt(),
        readMediaTypeSafe(parcel.readString().orEmpty()),
        parcel.readLong(),
        if (parcel.dataAvail() > 0) parcel.readByte() != 0.toByte() else false,
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(uri.toString())
        dest.writeString(filePath)
        dest.writeString(displayName)
        dest.writeString(mimeType)
        dest.writeLong(sizeBytes)
        dest.writeLong(durationMs)
        dest.writeLong(dateAddedSec)
        dest.writeInt(width)
        dest.writeInt(height)
        dest.writeString(mediaType.name)
        dest.writeLong(albumId)
        dest.writeByte(if (mirrorHorizontal) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<MediaEntity> {
        override fun createFromParcel(parcel: Parcel) = MediaEntity(parcel)
        override fun newArray(size: Int): Array<MediaEntity?> = arrayOfNulls(size)
    }
}
