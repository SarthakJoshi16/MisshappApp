package supabase

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import io.github.jan.supabase.storage.storage
import java.io.ByteArrayOutputStream
import java.io.File

// ---------------- POST MEDIA UPLOAD ----------------

suspend fun uploadPostMedia(
    file: File,
    userId: String
): String {

    val bucket = SupabaseClient.client.storage.from("media")

    val fileExt = file.extension.ifEmpty { "jpg" }
    val fileName = "${userId}_${System.currentTimeMillis()}.$fileExt"
    val path = "posts/$fileName"

    Log.d("UPLOAD_POST", "Uploading to media/$path")

    val bytes = safeMediaBytes(file)

    bucket.upload(
        path = path,
        data = bytes,
        upsert = true
    )

    return bucket.publicUrl(path)
}

// ---------------- PROFILE IMAGE UPLOAD ----------------

suspend fun uploadProfileImage(
    file: File,
    userId: String
): String {

    val bucket = SupabaseClient.client.storage.from("media")

    val fileExt = file.extension.ifEmpty { "jpg" }
    val path = "profiles/$userId.$fileExt"

    Log.d("UPLOAD_PROFILE", "Uploading to media/$path")

    bucket.upload(
        path = path,
        data = safeImageBytes(file), // profile images are ALWAYS images
        upsert = true
    )

    return bucket.publicUrl(path)
}

// ---------------- MEDIA BYTE HANDLING (THE REAL FIX) ----------------

private fun safeMediaBytes(file: File): ByteArray {
    return if (isVideoFile(file)) {
        Log.d("UPLOAD_MEDIA", "Detected video file, uploading raw bytes")
        file.readBytes() // ✅ DO NOT decode video
    } else {
        Log.d("UPLOAD_MEDIA", "Detected image file, compressing")
        safeImageBytes(file)
    }
}

// ---------------- IMAGE COMPRESSION (SAFE) ----------------

private fun safeImageBytes(file: File): ByteArray {

    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    // Read image bounds only
    BitmapFactory.decodeFile(file.path, options)

    options.inSampleSize = calculateInSampleSize(options, 1024, 1024)
    options.inJustDecodeBounds = false

    val bitmap = BitmapFactory.decodeFile(file.path, options)
        ?: throw IllegalStateException("Bitmap decode failed")

    val output = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, output)
    bitmap.recycle()

    return output.toByteArray()
}

// ---------------- HELPERS ----------------

private fun isVideoFile(file: File): Boolean {
    return file.extension.lowercase() in listOf(
        "mp4",
        "mkv",
        "webm",
        "mov",
        "3gp"
    )
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val (height, width) = options.outHeight to options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        var halfHeight = height / 2
        var halfWidth = width / 2

        while (
            halfHeight / inSampleSize >= reqHeight &&
            halfWidth / inSampleSize >= reqWidth
        ) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
