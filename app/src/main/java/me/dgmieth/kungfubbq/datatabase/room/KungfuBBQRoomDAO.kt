package me.dgmieth.kungfubbq.datatabase.room

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Single
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserAndSocialMedia
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserDB

@Dao
interface KungfuBBQRoomDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(data:UserDB):Completable

    @Transaction
    @Query("Select * FROM ${UserDB.TABLE_NAME}")
    fun getUser():Single<UserAndSocialMedia>

    @Delete
    fun deleteUser(user:UserDB):Completable

    @Update
    fun updateUser(user:UserDB)
}