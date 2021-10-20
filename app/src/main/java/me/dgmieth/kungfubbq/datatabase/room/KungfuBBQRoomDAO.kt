package me.dgmieth.kungfubbq.datatabase.room

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Single
import me.dgmieth.kungfubbq.datatabase.roomEntities.SocialMediaInfo
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserAndSocialMedia
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserDB

@Dao
interface KungfuBBQRoomDAO {
    //USER AND SOCIAL MEDIA INFO ENTITIES
    @Query("DELETE FROM ${UserDB.TABLE_NAME}")
    fun deleteAllUserInfo():Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(data:UserDB):Completable

    @Query("DELETE FROM ${SocialMediaInfo.TABLE_NAME}")
    fun deleteAllSocialMediaInfo():Completable
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSocialMediaInfo(data:MutableList<SocialMediaInfo>):Completable

    @Transaction
    @Query("Select * FROM ${UserDB.TABLE_NAME}")
    fun getUser():Single<UserAndSocialMedia>


    @Delete
    fun deleteUser(user:UserDB):Completable

    @Update
    fun updateUser(user:UserDB)
}