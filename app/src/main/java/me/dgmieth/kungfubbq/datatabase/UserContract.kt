package me.dgmieth.kungfubbq.datatabase

import android.content.ContentUris
import android.net.Uri
import android.provider.BaseColumns

object UserContract {
    internal const val TABLE_NAME = "User"

    val CONTENT_URI : Uri = Uri.withAppendedPath(CONTENT_AUTHORITY_URI, TABLE_NAME)

    const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$CONTENT_AUTHORITY.$TABLE_NAME"
    const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.$CONTENT_AUTHORITY.$TABLE_NAME"

    object Columns {
        const val ID = BaseColumns._ID
        const val EMAIL = "Email"
        const val MEMBER_SINCE = "MemberSince"
        const val NAME = "Name"
        const val PHONE_NUMBER = "PhoneNumber"
        const val TOKEN = "Token"
        const val LOGGED = "Logged"
    }
    //methods
    fun getId(uri: Uri):Long{
        return ContentUris.parseId(uri)
    }
    fun builUriFromId(id:Long):Uri{
        return ContentUris.withAppendedId(CONTENT_URI,id)
    }
}