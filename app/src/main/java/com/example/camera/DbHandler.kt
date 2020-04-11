package com.example.camera

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.util.Log

class DbHandler(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        //Constants for db name and version
        private const val DATABASE_NAME = "camera"
        private const val DATABASE_VERSION = 1

        //Constants for table and columns
        const val TABLE_IMAGES = "images"
        const val IMAGE_ID = "_id"
        const val IMAGE_FILE_NAME = "fileName"
        const val IMAGE_FILE_URI = "uri"
        val ALL_COLUMNS = arrayOf(IMAGE_ID, IMAGE_FILE_NAME, IMAGE_FILE_URI)

        //Create Table
        private const val CREATE_TABLE = "CREATE TABLE " + TABLE_IMAGES + " (" +
                IMAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                IMAGE_FILE_NAME + " TEXT, " +
                IMAGE_FILE_URI + " TEXT " +
                ")"
    }

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, i: Int, i1: Int) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS $TABLE_IMAGES")
        onCreate(sqLiteDatabase)
    }

    fun addImageFileImageTable(uri: Uri, contentValues: ContentValues) {
        val db = writableDatabase
        db.insert(TABLE_IMAGES, null, contentValues)
        db.close()
        Log.d("STATUSCHECK", "in DbHandler, addImageFileImageTable(). content values: ${contentValues}")
    }

    fun removeImageFileImageTable(uri: Uri) {
        val db = writableDatabase
        db.delete(TABLE_IMAGES, "uri = ?", arrayOf(uri.toString()))
        db.close()
    }
}