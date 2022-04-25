package me.dgmieth.kungfubbq.datatabase.roomEntities
import androidx.room.*

@Entity(tableName = RememberMeInfo.TABLE_NAME)
data class RememberMeInfo (
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = REMEMBER)
    var remember: Int,
    @ColumnInfo(name = PASSWORD)
    var password: String
){
    companion object{
        const val TABLE_NAME="RememberMe"
        const val REMEMBER = "remember"
        const val PASSWORD = "password"
    }
}