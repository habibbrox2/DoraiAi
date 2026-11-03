package com.inspiredandroid.kai.data

/**
 * Gallery মিডিয়া আইটেমের ডোমেইন মডেল।
 * MediaStore কলামগুলোর সাথে ম্যাপ করা হয়েছে।
 */
data class GalleryMediaItem(
    /** MediaStore _ID — স্থিতিশীল, অনন্য আইডেন্টিফায়ার */
    val id: Long,

    /** কনটেন্ট URI — content://media/external/images/media/{id} বা video equivalent */
    val contentUri: String,

    /** ফাইলের প্রদর্শন নাম (Display Name) */
    val displayName: String,

    /** MIME টাইপ — উদাহরণ: image/jpeg, video/mp4 */
    val mimeType: String,

    /** ইমেজ/ভিডিওর প্রস্থ পিক্সেলে (WIDTH কলাম) */
    val width: Int?,

    /** ইমেজ/ভিডিওর উচ্চতা পিক্সেলে (HEIGHT কলাম) */
    val height: Int?,

    /** ফাইলের আকার বাইটে (SIZE কলাম) */
    val sizeBytes: Long,

    /** MediaStore-এ যোগ হওয়ার সময় (DATE_ADDED) — epoch milliseconds */
    val dateAdded: Long,

    /** ফাইল শেষ পরিবর্তনের সময় (DATE_MODIFIED) — epoch milliseconds */
    val dateModified: Long,

    /** EXIF-এ নথিভুক্তিকৃত ছবি তুলে দেওয়ার সময় (DATE_TAKEN) — epoch milliseconds, নেইলে null */
    val dateTaken: Long?,

    /** সাপেক্ষ পাথ (RELATIVE_PATH) — API 29+ এ উপলব্ধ, নেইলে null */
    val relativePath: String?,

    /** আপলোড/প্রসেসিং ভাবে পেন্ডিং কিনা (IS_PENDING) — API 29+ */
    val isPending: Boolean,

    /** পছন্দ হিসেবে চিহ্নিত কিনা (IS_FAVORITE) — API 29+ */
    val isFavorite: Boolean,
) {

    /** মিডিয়া ইমেজ কিনা তা চেক করে */
    val isImage: Boolean
        get() = mimeType.startsWith("image/")

    /** মিডিয়া ভিডিও কিনা তা চেক করে */
    val isVideo: Boolean
        get() = mimeType.startsWith("video/")

    /** ফাইলের আকার মানব-पঠনযোগ্য ফরম্যাটে রিটার্ন করে (KB, MB, GB) */
    fun humanReadableSize(): String = when {
        sizeBytes < 1024 -> "$sizeBytes B"
        sizeBytes < 1024 * 1024 -> "${(sizeBytes / 1024)} KB"
        sizeBytes < 1024 * 1024 * 1024 -> "${sizeBytes / (1024.0 * 1024)} MB"
        else -> "${sizeBytes / (1024.0 * 1024 * 1024)} GB"
    }

    /** রেজোলিউশন স্ট্রিং রিটার্ন করে (উদাহরণ: "1920x1080") */
    val resolutionString: String?
        get() = if (width != null && height != null) "${width}x$height" else null
}
