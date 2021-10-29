package me.dgmieth.kungfubbq.datatabase.room

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Single
import me.dgmieth.kungfubbq.datatabase.roomEntities.*

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

    //COOKING DATE, DISHES and ORDERS
    @Transaction
    @Query("DELETE FROM ${CookingDateDB.TABLE_NAME}")
    fun deleteAllCookingDates():Completable
    @Transaction
    @Query("DELETE FROM ${CookingDateDishesDB.TABLE_NAME}")
    fun deleteAllCookingDateDishes():Completable
    @Transaction
    @Query("DELETE FROM ${OrderDB.TABLE_NAME}")
    fun deleteAllOrders():Completable
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllCookingDates(cookingDates: MutableList<CookingDateDB>):Completable
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllCookingDatesDishes(cookingDatesDishes: MutableList<CookingDateDishesDB>):Completable
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllOrders(orders: MutableList<OrderDB>):Completable
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllOrdersDishes(orderDishes: MutableList<OrderDishesDB>):Completable

    @Transaction
    @Query("Select * FROM ${CookingDateDB.TABLE_NAME} WHERE ${CookingDateDB.COOKING_DATE_ID} = :cookingDateId")
    fun getCookingDate(cookingDateId:Int):Single<CookingDateAndCookingDateDishesWithOrder>


    @Delete
    fun deleteUser(user:UserDB):Completable

    @Update
    fun updateUser(user:UserDB)
}