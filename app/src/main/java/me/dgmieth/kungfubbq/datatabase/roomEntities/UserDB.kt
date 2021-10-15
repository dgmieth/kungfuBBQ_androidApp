package me.dgmieth.kungfubbq.datatabase.roomEntities

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = UserDB.TABLE_NAME)
data class UserDB (
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = ID)
    var userId: Int? = null,
    @ColumnInfo(name = EMAIL)
    var email: String? = null,
    @ColumnInfo(name = MEMBER_SINCE)
    var memberSince: String? = null,
    @ColumnInfo(name = NAME)
    var name: String? = null,
    @ColumnInfo(name = PHONE_NUMBER)
    var phoneNumber: String? = null,
    @ColumnInfo(name = TOKEN)
    var token: String? = null,
    @ColumnInfo(name = LOGGED)
    var logged: Int? = null
        ){
    companion object{
        const val TABLE_NAME="User"
        const val ID = "id"
        const val EMAIL = "email"
        const val MEMBER_SINCE = "memberSince"
        const val NAME = "name"
        const val PHONE_NUMBER = "phoneNumber"
        const val TOKEN = "token"
        const val LOGGED = "logged"
    }
}