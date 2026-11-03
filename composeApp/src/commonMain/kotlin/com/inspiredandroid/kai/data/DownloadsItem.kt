package com.inspiredandroid.kai.data

/**
 * Downloads আইটেমের ডোমেইন মডেল।
 * DownloadManager, MediaStore, SAF থেকে একীভূত ডেটা।
 * Platform-agnostic core model - Android-specific extensions in androidMain.
 */
data class DownloadsItem(
    /** অনন্য আইডেন্টিফায়ার (DownloadManager ID বা MediaStore _ID) */
    val id: Long,

    /** কনটেন্ট URI — content:// URI */
    val contentUri: String,

    /** প্রদর্শন নাম (Display Name) */
    val displayName: String,

    /** MIME টাইপ (image/jpeg, application/pdf, application/zip, etc.) */
    val mimeType: String,

    /** ফাইলের আকার বাইটে */
    val sizeBytes: Long,

    /** MediaStore/DownloadManager-এ যোগ হওয়ার সময় (epoch ms) */
    val dateAdded: Long,

    /** শেষ পরিবর্তনের সময় (epoch ms) */
    val dateModified: Long,

    /** ডেটা সোর্স */
    val source: DownloadsSource,

    /** সাপেক্ষ পাথ (API 29+) */
    val relativePath: String?,

    /** DownloadManager বিবরণ (DESCRIPTION কলাম) */
    val description: String?,

    /** Media Type (DownloadManager: MEDIA_TYPE) */
    val mediaType: Int?,

    /** লোকাল ফাইল URI (DownloadManager: LOCAL_URI) */
    val destinationUri: String?,

    /** পেন্ডিং অবস্থায় আছে কিনা (API 29+) */
    val isPending: Boolean,

    /** DownloadManager স্ট্যাটাস (STATUS কলাম) */
    val status: Int?,

    /** DownloadManager রিজন (REASON কলাম) */
    val reason: Int?,
) {
    /** স्रोত enum - ডেটা কোথা থেকে এসেছে */
    enum class DownloadsSource {
        DOWNLOAD_MANAGER, // DownloadManager API থেকে
        MEDIA_STORE, // MediaStore (ইমেজ/ভিডিও/অডিও)
        SAF, // Storage Access Framework (user-selected)
        UNKNOWN, // অজানা সোর্স
    }

    /** সোর্সের মানব-পঠনযোগ্য নাম */
    val sourceDisplayName: String
        get() = when (source) {
            DownloadsSource.DOWNLOAD_MANAGER -> "DownloadManager"
            DownloadsSource.MEDIA_STORE -> "MediaStore"
            DownloadsSource.SAF -> "Storage Access Framework"
            DownloadsSource.UNKNOWN -> "Unknown"
        }

    /** ফাইলের আকার মানব-পঠনযোগ্য ফরম্যাটে */
    fun humanReadableSize(): String = when {
        sizeBytes < 1024 -> "$sizeBytes B"
        sizeBytes < 1024 * 1024 -> "${(sizeBytes / 1024)} KB"
        sizeBytes < 1024 * 1024 * 1024 -> "${sizeBytes / (1024.0 * 1024)} MB"
        else -> "${sizeBytes / (1024.0 * 1024 * 1024)} GB"
    }
}
