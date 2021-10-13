package me.dgmieth.kungfubbq.datatabase

import android.content.ContentUris
import android.net.Uri
import android.provider.BaseColumns

object SocialMediaContract {
    internal const val TABLE_NAME = "SocialMedia"

    val CONTENT_URI : Uri = Uri.withAppendedPath(CONTENT_AUTHORITY_URI, TABLE_NAME)

    const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$CONTENT_AUTHORITY.$TABLE_NAME"
    const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.$CONTENT_AUTHORITY.$TABLE_NAME"

    object Columns {
        const val USER_ID = "UserId"
        const val SOCIAL_MEDIA = "SocialMedia"
        const val SOCIAL_MEDIA_USER_NAME = "SocialMediaUserName"
    }
    //methods
    fun getId(uri: Uri):Long{
        return ContentUris.parseId(uri)
    }
    fun builUriFromId(id:Long):Uri{
        return ContentUris.withAppendedId(CONTENT_URI,id)
    }
}