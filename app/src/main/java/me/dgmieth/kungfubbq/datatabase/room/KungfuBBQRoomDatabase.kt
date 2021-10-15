package me.dgmieth.kungfubbq.datatabase.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserDB

const val DB_VERSION = 1
const val DB_NAME = "kungfuBBQapp.db"

@Database(entities = [UserDB::class],version = DB_VERSION)
abstract class KungfuBBQRoomDatabase:RoomDatabase() {
    abstract fun kungfuBBQRoomDao() : KungfuBBQRoomDAO
    companion object{
        @Volatile
        private var dbInstance : KungfuBBQRoomDatabase? = null
        fun getInstance(mContext: Context):KungfuBBQRoomDatabase = dbInstance ?: synchronized(this){
            print("getInstanceCalled")
            dbInstance ?: buildDatabaseInstance(mContext).also {
                print("getInstanceCalled -> returned")
                dbInstance = it
            }
        }
        private fun buildDatabaseInstance(mContext: Context) =
            Room.databaseBuilder(mContext, KungfuBBQRoomDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration()
                .build()

    }
}