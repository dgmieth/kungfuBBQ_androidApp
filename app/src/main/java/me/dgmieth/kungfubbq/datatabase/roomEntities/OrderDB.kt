package me.dgmieth.kungfubbq.datatabase.roomEntities

import androidx.room.*

@Entity(tableName = OrderDB.TABLE_NAME,primaryKeys = ["orderId"],
   // foreignKeys = [ForeignKey(entity = CookingDateDB::class, parentColumns = arrayOf("cookingDateId"),childColumns = arrayOf("cookingDateIdFk"), onDelete = ForeignKey.CASCADE)]
)
data class OrderDB (
    @ColumnInfo(name = ORDER_ID)
    var orderId: Int,
    @ColumnInfo(name = ORDER_DATE)
    var orderDate: String,
    @ColumnInfo(name = COOKING_DATE_ID_FK)
    var cookingDateIdFk: Int,
    @ColumnInfo(name = ORDER_STATUS_ID)
    var orderStatusId: Int,
    @ColumnInfo(name = ORDER_STATUS_NAME)
    var orderStatusName: String,
    @ColumnInfo(name = USER_ID)
    var userId: Int,
    @ColumnInfo(name = USER_NAME)
    var userName: String,
    @ColumnInfo(name = USER_EMAIL)
    var userEmail: String,
    @ColumnInfo(name = USER_PHONE_NUMBER)
    var userPhoneNumber: String,
        ){
    companion object {
        const val TABLE_NAME="OrderDB"
        const val ORDER_ID = "orderId"
        const val ORDER_DATE = "orderDate"
        const val COOKING_DATE_ID_FK = "cookingDateIdFk"
        const val ORDER_STATUS_ID = "orderStatusId"
        const val ORDER_STATUS_NAME = "orderStatusName"
        const val USER_ID = "userId"
        const val USER_NAME = "userName"
        const val USER_EMAIL = "userEmail"
        const val USER_PHONE_NUMBER = "userPhoneNumber"
    }
}

@Entity(tableName = OrderDishesDB.TABLE_NAME,primaryKeys = ["dishId","orderIdFk"], foreignKeys = arrayOf(
    ForeignKey(entity = OrderDB::class, parentColumns = arrayOf("orderId"),childColumns = arrayOf("orderIdFk"), onDelete = ForeignKey.CASCADE)
))
data class OrderDishesDB(
    @ColumnInfo(name = DISH_ID)
    var dishId : Int,
    @ColumnInfo(name = DISH_NAME)
    var dishName : String,
    @ColumnInfo(name = DISH_PRICE)
    var dishPrice : String,
    @ColumnInfo(name = DISH_QTTY)
    var dishQuantity : Int,
    @ColumnInfo(name = OBSERVATION)
    var observation : String,
    @ColumnInfo(name = ORDER_ID_FK)
    var orderIdFk : Int
){
    companion object {
        const val TABLE_NAME="OrderDishes"
        const val DISH_ID="dishId"
        const val DISH_NAME = "dishName"
        const val DISH_PRICE = "dishPrice"
        const val DISH_QTTY = "dishQuantity"
        const val OBSERVATION = "observation"
        const val ORDER_ID_FK = "orderIdFk"
    }
}
data class OrderWithDishes(
    @Embedded val order : OrderDB,
    @Relation(
        parentColumn = "orderId",
        entityColumn = "orderIdFk"
    )
    val dishes : List<OrderDishesDB>
)
//data class OrderAndCookingDate(
//    @Embedded val order : OrderDB,
//    @Relation(
//        parentColumn = "cookingDateIdFk",
//        entityColumn = "cookingDateId"
//    )
//    val cookingDate : CookingDateDB
//)
//data class OrderAndCookingDateWithDishes(
//    @Embedded val order : OrderWithDishes,
//    @Relation(
//        parentColumn = "cookingDateIdFk",
//        entityColumn = "cookingDateId"
//    )
//    val cookingDate : CookingDateDB
//)
