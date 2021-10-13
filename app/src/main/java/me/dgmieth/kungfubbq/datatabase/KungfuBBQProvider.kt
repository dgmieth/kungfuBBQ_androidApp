package me.dgmieth.kungfubbq.datatabase

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.util.Log
import java.lang.IllegalArgumentException

private const val TAG = "AppProvider"

const val CONTENT_AUTHORITY = "me.dgmieth.kungfubbq.provider"

private const val USER = 100

private const val COOKING_DATE = 200

val CONTENT_AUTHORITY_URI : Uri = Uri.parse("content://${CONTENT_AUTHORITY}")

class KungfuBBQProvider:ContentProvider() {
    private val uriMatcher by lazy { buildURIMatcher() }

    private fun buildURIMatcher():UriMatcher {
        Log.d(TAG,"UriMatcher: initialises")
        val matcher = UriMatcher(UriMatcher.NO_MATCH)

        matcher.addURI(CONTENT_AUTHORITY,UserContract.TABLE_NAME, USER)

        Log.d(TAG,"UriMatcher: ends")
        return matcher
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        Log.d(TAG, "query called with uri $uri")
        val match =  uriMatcher.match(uri)
        Log.d(TAG, "query -> match  $match")
        val queryBuilder = SQLiteQueryBuilder()

        when(match){
            USER -> {
                queryBuilder.tables = UserContract.TABLE_NAME
            }
            else -> throw IllegalArgumentException("Unknown URI $uri")
        }
        val db = KungfuBBQDatabase.getInstace(context!!).readableDatabase
        val cursor = queryBuilder.query(db,projection,selection,selectionArgs,null,null,sortOrder)
        Log.d(TAG, "query -> cursor count ${cursor.count}")
        Log.d(TAG, "query ends")
        return cursor
    }

    override fun getType(uri: Uri): String? {
        TODO("Not yet implemented")
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        TODO("Not yet implemented")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        TODO("Not yet implemented")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        TODO("Not yet implemented")
    }
}