package com.example.examplemediastore.mediastore

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.*
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import com.example.examplemediastore.MainActivity.Companion.REQUEST_PERMISSION_DELETE
import java.io.FileOutputStream
import java.io.InputStream

object MediaStoreManager {

    const val COLLECTION_IMAGE = 0
    const val COLLECTION_AUDIO = 1
    const val COLLECTION_VIDEO = 2
    const val COLLECTION_DOWNLOAD = 3
    const val COLLECTION_FILE = 4

    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveMediaFile(context: Context, fileName: String, collection: Int, inputStream: InputStream): Boolean {

        try {
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1)

            val contentResolver = context.contentResolver
            val collectionUri = when (collection) {
                COLLECTION_IMAGE -> contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                COLLECTION_AUDIO -> contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                COLLECTION_VIDEO -> contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                COLLECTION_DOWNLOAD -> contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                else -> contentResolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), contentValues)
            } ?: return false

            val fileDescriptor = contentResolver.openFileDescriptor(collectionUri, "w", null) ?: return false
            val fileOutputStream = FileOutputStream(fileDescriptor.fileDescriptor)
            val buffer = ByteArray(1024)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                fileOutputStream.write(buffer, 0, read)
            }
            inputStream.close()
            fileOutputStream.close()
            fileDescriptor.close()

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            contentResolver.update(collectionUri, contentValues, null, null)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun queryMediaFiles(context: Context, collection: Int, keyword: String?): ArrayList<MediaFile> {

        val mediaFiles = ArrayList<MediaFile>()

        // uri : 쿼리할 데이터의 URI
        val uri = when (collection) {
            COLLECTION_IMAGE -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            COLLECTION_AUDIO -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            COLLECTION_VIDEO -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            COLLECTION_DOWNLOAD -> MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else -> MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } ?: return mediaFiles

        // projection : 쿼리 결과로 받고싶은 데이터 종류
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.SIZE
        )

        // selection, selectionArgs : 쿼리 조건문 (DISPLAY_NAME 이 keyword 를 포함할 경우)
        var selection: String? = null
        var selectionArgs: Array<String>? = null
        if (keyword != null && keyword.isNotEmpty()) {
            selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE?"
            selectionArgs = arrayOf("%$keyword%")
        }

        // setOrder : 쿼리 결과 정렬
        val sortOrderDesc = "${MediaStore.Images.Media.DATE_MODIFIED} DESC" // 내림차순 정렬
        val sortOrderAsc = "${MediaStore.Images.Media.DATE_MODIFIED} ASC" // 오름차순 정렬

        val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrderDesc)
        if (cursor == null || !cursor.moveToFirst()) return mediaFiles

        cursor.use { c ->

            val idIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeTypeIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val pathIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            val dataIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

            do {
                val id = c.getLong(idIndex)
                val name = c.getString(nameIndex)
                val mimeType = c.getString(mimeTypeIndex)
                val relativePath = c.getString(pathIndex)
                val fullPath = c.getString(dataIndex)
                val date = c.getLong(dateIndex) * 1000
                val size = c.getInt(sizeIndex)

                val contentUri = ContentUris.withAppendedId(uri, id)
                val mediaFile = MediaFile(contentUri, name, mimeType, relativePath, fullPath, date, size)

                mediaFiles.add(mediaFile)
                Log.d("MediaStore", "queryFiles add mediaFile : $mediaFile")
            } while (c.moveToNext())
        }
        cursor.close()

        return mediaFiles
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun queryMediaFileFromUri(context: Context, contentUri: Uri): MediaFile? {

        var mediaFile: MediaFile? = null

        // uri : 쿼리할 데이터의 URI
        val uri = contentUri

        // projection : 쿼리 결과로 받고싶은 데이터 종류
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.SIZE
        )

        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        if (cursor == null || !cursor.moveToFirst()) return null

        cursor.use { c ->

            val idIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeTypeIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val pathIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            val dataIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

            do {
                val id = c.getLong(idIndex)
                val name = c.getString(nameIndex)
                val mimeType = c.getString(mimeTypeIndex)
                val relativePath = c.getString(pathIndex)
                val fullPath = c.getString(dataIndex)
                val date = c.getLong(dateIndex) * 1000
                val size = c.getInt(sizeIndex)

                mediaFile = MediaFile(contentUri, name, mimeType, relativePath, fullPath, date, size)
                Log.d("MediaStore", "queryFileByUri mediaFile : $mediaFile")
            } while (c.moveToNext())
        }
        cursor.close()

        return mediaFile
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun queryFilesFromName(context: Context, fileName: String): MediaFile? {

        var mediaFile: MediaFile? = null

        // uri : 쿼리할 데이터의 URI
        val uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL) ?: return null

        // projection : 쿼리 결과로 받고싶은 데이터 종류
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.SIZE
        )

        // selection, selectionArgs : 쿼리 조건문 (DISPLAY_NAME 이 fileName 일 경우)
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME}=?"
        val selectionArgs = arrayOf(fileName)

        val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
        if (cursor == null || !cursor.moveToFirst()) return null

        cursor.use { c ->

            val idIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeTypeIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val pathIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            val dataIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

            do {
                val id = c.getLong(idIndex)
                val name = c.getString(nameIndex)
                val mimeType = c.getString(mimeTypeIndex)
                val relativePath = c.getString(pathIndex)
                val fullPath = c.getString(dataIndex)
                val date = c.getLong(dateIndex) * 1000
                val size = c.getInt(sizeIndex)

                val contentUri = ContentUris.withAppendedId(uri, id)

                if (fileName == name) {
                    mediaFile = MediaFile(contentUri, name, mimeType, relativePath, fullPath, date, size)
                    Log.d("MediaStore", "queryFileByFileName mediaFile : $mediaFile")
                    break
                }
            } while (c.moveToNext())
        }
        cursor.close()

        return mediaFile
    }

    fun openInputStream(context: Context, mediaFile: MediaFile): InputStream? {
        return context.contentResolver.openInputStream(mediaFile.contentUri)
    }

    fun openFile(context: Context, mediaFile: MediaFile) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(mediaFile.contentUri, mediaFile.mimeType)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun deleteFile(context: Context, mediaFile: MediaFile): Int {
        return try {
            val contentResolver = context.contentResolver
            contentResolver.delete(mediaFile.contentUri, null, null)
        } catch (e: RecoverableSecurityException) {
            // 권한이 없기 때문에 예외가 발생됩니다.
            // RemoteAction은 Exception과 함께 전달됩니다.
            // RemoteAction에서 IntentSender 객체를 가져올 수 있습니다.
            // startIntentSenderForResult()를 호출하여 팝업을 띄웁니다.
            val intentSender = e.userAction.actionIntent.intentSender
            startIntentSenderForResult(context as Activity,
                intentSender,
                REQUEST_PERMISSION_DELETE,
                null,
                0,
                0,
                0,
                null
            )
            return 0
        }
    }
}