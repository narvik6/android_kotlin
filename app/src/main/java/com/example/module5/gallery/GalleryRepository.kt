package com.example.module5.gallery

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GalleryRepository(private val picturesDir: File) {

            
    fun createPhotoFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(picturesDir, "IMG_$timestamp.jpg")
    }

            
    fun loadPhotos(): List<PhotoItem> =
        picturesDir
            .listFiles { f -> f.isFile && f.extension.lowercase() == "jpg" }
            ?.sortedByDescending { it.lastModified() }
            ?.map { PhotoItem(file = it, name = it.name) }
            ?: emptyList()

            
    fun exportToGallery(context: Context, photoFile: File): Boolean {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, photoFile.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/MyGallery"
                )
                                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return false

                        resolver.openOutputStream(uri)?.use { out ->
                photoFile.inputStream().use { it.copyTo(out) }
            }

                        contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
