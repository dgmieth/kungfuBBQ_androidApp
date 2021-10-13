package me.dgmieth.kungfubbq.datatabase

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kotlin.io.path.createTempDirectory

private const val TAG = "Database"

private const val DATABASE_NAME = "kungfuBBQapp"
private const val DATABASE_VERSION = 1

internal class KungfuBBQDatabase private constructor(context: Context):SQLiteOpenHelper(context, DATABASE_NAME,null,DATABASE_VERSION) {

    init {
        Log.d(TAG, "database: initialising")
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.d(TAG, "database: starts")
        val sSQL = """CREATE TABLE ${UserContract.TABLE_NAME}(
            ${UserContract.Columns.ID} INTEGER PRIMARY KEY NOT NULL, 
            ${UserContract.Columns.EMAIL} TEXT NOT NULL,
            ${UserContract.Columns.MEMBER_SINCE} TEXT,
            ${UserContract.Columns.NAME} TEXT,
            ${UserContract.Columns.PHONE_NUMBER} TEXT,
            ${UserContract.Columns.TOKEN} TEXT,
            ${UserContract.Columns.LOGGED} INTEGER DEFAULT 0); CREATE TABLE ${SocialMediaContract.TABLE_NAME} (
            ${SocialMediaContract.Columns.USER_ID} INTEGER NOT NULL,
            ${SocialMediaContract.Columns.SOCIAL_MEDIA} TEXT,
            ${SocialMediaContract.Columns.SOCIAL_MEDIA_USER_NAME} TEXT,
            CONSTRAINT fk_user 
                FOREIGN KEY (${SocialMediaContract.Columns.USER_ID}
                REFERENCES ${UserContract.TABLE_NAME}(${UserContract.Columns.ID}))""".trimMargin().replaceIndent(" ")
        Log.d(TAG, "database: ends")
        db.execSQL(sSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }

    companion object : SingletonCreator<KungfuBBQDatabase,Context>(::KungfuBBQDatabase)
}