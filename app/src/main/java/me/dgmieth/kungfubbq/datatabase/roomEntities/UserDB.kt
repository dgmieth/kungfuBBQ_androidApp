package me.dgmieth.kungfubbq.datatabase.roomEntities

import android.provider.BaseColumns
import androidx.room.*

@Entity(tableName = UserDB.TABLE_NAME)
data class UserDB (
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = USER_ID)
    var userId: Int,
    @ColumnInfo(name = EMAIL)
    var email: String,
    @ColumnInfo(name = MEMBER_SINCE)
    var memberSince: String?,
    @ColumnInfo(name = NAME)
    var name: String?,
    @ColumnInfo(name = PHONE_NUMBER)
    var phoneNumber: String?,
    @ColumnInfo(name = TOKEN)
    var token: String?,
    @ColumnInfo(name = LOGGED)
    var logged: Int?
        ){
    companion object{
        const val TABLE_NAME="User"
        const val USER_ID = "userId"
        const val EMAIL = "email"
        const val MEMBER_SINCE = "memberSince"
        const val NAME = "name"
        const val PHONE_NUMBER = "phoneNumber"
        const val TOKEN = "token"
        const val LOGGED = "logged"
    }
}

@Entity(tableName = SocialMediaInfo.TABLE_NAME,primaryKeys = ["socialMedia","userIdFk"])
data class SocialMediaInfo(
    @ColumnInfo(name = SOCIAL_MEDIA)
    var socialMedia : String,
    @ColumnInfo(name = SOCIAL_MEDIA_NAME)
    var socialMediaName : String?,
    @ColumnInfo(name = USER_ID_FK)
    var userIdFk: Int
){
    companion object {
        const val TABLE_NAME="SocialMediaInfo"
        const val SOCIAL_MEDIA="socialMedia"
        const val SOCIAL_MEDIA_NAME = "socialMediaName"
        const val USER_ID_FK = "userIdFk"
    }
}

data class UserAndSocialMedia(
    @Embedded val user : UserDB,
    @Relation(
        parentColumn = "userId",
        entityColumn = "userIdFk"
    )
    val socialMedia : List<SocialMediaInfo>
)