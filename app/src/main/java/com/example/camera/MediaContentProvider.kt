package com.example.camera

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class MediaContentProvider : ContentProvider() {


    companion object {
        private const val AUTHORITY = "com.example.camera.mediacontentprovider"
        private const val BASE_PATH = "camera"
        val CONTENT_URI = Uri.parse("content://$AUTHORITY/$BASE_PATH")
        private const val IMAGES = 1
        private const val FILE_NAME = 2
        private const val URI = 3

        private val MIME_TYPES = HashMap<String, String>()
        private val mRoots = HashMap<String, File>()
        private val uriMatcher: UriMatcher? = UriMatcher(UriMatcher.NO_MATCH)
        private var database: SQLiteDatabase? = null

        init {
            MIME_TYPES[".jpg"] = "image/jpeg"
            MIME_TYPES[".jpeg"] = "image/jpeg"

            uriMatcher!!.addURI(AUTHORITY, BASE_PATH, IMAGES)
            uriMatcher.addURI(AUTHORITY, "$BASE_PATH/1", FILE_NAME)
            uriMatcher.addURI(AUTHORITY, "$BASE_PATH/2", URI)
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        var delCount = 0
        delCount = when (uriMatcher!!.match(uri)) {
            IMAGES -> database!!.delete(DbHandler.TABLE_IMAGES, selection, selectionArgs)
            else     -> throw java.lang.IllegalArgumentException("This is an Unknown URI $uri")
        }
        context!!.contentResolver.notifyChange(uri, null)
        return delCount
    }

    override fun getType(uri: Uri): String? {
        val path = uri.toString()
        for (extension in MIME_TYPES.keys) {
            if (path.endsWith(extension)) {
                return MIME_TYPES[extension]
            }
        }
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val id = database!!.insert(DbHandler.TABLE_IMAGES, null, values)

        if (id > 0) {
            val _uri = ContentUris.withAppendedId(CONTENT_URI, id)
            context!!.contentResolver.notifyChange(_uri, null)
            return _uri
        }
        throw SQLException("Insertion Failed for URI :$uri")
    }

    override fun onCreate(): Boolean {
        val helper = DbHandler(context)
        database = helper.writableDatabase
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        val cursor: Cursor
        cursor = when (uriMatcher!!.match(uri)) {
            FILE_NAME -> database!!.query(DbHandler.TABLE_IMAGES, DbHandler.ALL_COLUMNS,
                null, null, null, null, DbHandler.IMAGE_FILE_NAME + " ASC")
            URI       -> database!!.query(DbHandler.TABLE_IMAGES, DbHandler.ALL_COLUMNS,
                null, null, null, null, DbHandler.IMAGE_URI + " ASC")
            else     -> throw IllegalArgumentException("This is an Unknown URI $uri")
        }
        cursor.setNotificationUri(context!!.contentResolver, uri)

        return cursor
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?,
                        selectionArgs: Array<String>?): Int {
        TODO("Implement this to handle requests to update one or more rows.")
    }

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val f = File(context!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "openFileTemp.jpg")
        f.delete()
        try {
            f.createNewFile()
            if (f.exists()) {
                return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_WRITE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        throw FileNotFoundException(uri.getPath())
    }

    fun getFileForUri(uri: Uri): File? {
        var path = uri.encodedPath
        val splitIndex = path!!.indexOf('/', 1)
        val tag = Uri.decode(path.substring(1, splitIndex))
        path = Uri.decode(path.substring(splitIndex + 1))
        val root: File = mRoots.get(tag)
            ?: throw java.lang.IllegalArgumentException("Unable to find configured root for $uri")
        var file = File(root, path)
        file = try {
            file.canonicalFile
        } catch (e: IOException) {
            throw java.lang.IllegalArgumentException("Failed to resolve canonical path for $file")
        }
        if (!file.path.startsWith(root.path)) {
            throw SecurityException("Resolved path jumped beyond configured root")
        }
        return file
    }
}
