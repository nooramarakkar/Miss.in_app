package com.missin.app.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            // 1. Open a stream to read the image from the Android gallery
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            // 2. Create a temporary file in your app's cache directory
            val tempFile = File.createTempFile("cloudinary_upload_", ".jpg", context.cacheDir)

            // 3. Copy the data from the gallery into your temporary file
            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)

            // 4. Clean up
            inputStream.close()
            outputStream.close()

            // 5. Return the physical file ready for Cloudinary
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}