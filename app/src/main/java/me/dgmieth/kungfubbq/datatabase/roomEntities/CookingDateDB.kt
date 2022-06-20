package me.dgmieth.kungfubbq.datatabase.roomEntities

import androidx.room.*

@Entity(tableName = CookingDateDB.TABLE_NAME)
data class CookingDateDB (
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = COOKING_DATE_ID)
    var cookingDateId: Int,
    @ColumnInfo(name = COOKING_DATE)
    var cookingDate: String,
    @ColumnInfo(name = COOKING_DATE_AM_PM)
    var cookingDateAmPm: String,
    @ColumnInfo(name = MEALS_FOR_THIS)
    var mealsForThis: Int,
    @ColumnInfo(name = ADDRESS_ID)
    var addressId: Int,
    @ColumnInfo(name = STREET)
    var street: String,
    @ColumnInfo(name = COMPLEMENT)
    var complement: String,
    @ColumnInfo(name = CITY)
    var city: String,
    @ColumnInfo(name = STATE)
    var state: String,
    @ColumnInfo(name = ZIPCODE)
    var zipcode: String,
    @ColumnInfo(name = COUNTRY)
    var country: String,
    @ColumnInfo(name = LAT)
    var lat: Double,
    @ColumnInfo(name = LNG)
    var lng: Double,
    @ColumnInfo(name = COOKING_STATUS_ID)
    var cookingStatusId: Int,
    @ColumnInfo(name = COOKING_STATUS)
    var cookingStatus: String,
    @ColumnInfo(name = MENU_ID)
    var menuID: Int,
    @ColumnInfo(name = END_TIME)
    var endTime: String,
    @ColumnInfo(name = COOKING_DATE_END_AM_PM)
    var cookingDateEndAmPm: String,
    @ColumnInfo(name = VENUE)
    var venue: String,
    @ColumnInfo(name = MAYBE_GO)
    var maybeGo: Int,
    @ColumnInfo(name = EVENT_ONLY)
    var eventOnly: Int,
        ){
    companion object{
        const val TABLE_NAME="CookingDates"
        const val COOKING_DATE_ID = "cookingDateId"
        const val COOKING_DATE = "cookingDate"
        const val END_TIME = "endTime"
        const val COOKING_DATE_END_AM_PM = "cookingDateEndAmPm"
        const val COOKING_DATE_AM_PM = "cookingDateAmPm"
        const val MEALS_FOR_THIS = "mealsForThis"
        const val ADDRESS_ID = "addressId"
        const val VENUE = "venue"
        const val STREET = "street"
        const val COMPLEMENT = "complement"
        const val CITY = "city"
        const val STATE = "state"
        const val ZIPCODE = "zipcode"
        const val COUNTRY = "country"
        const val LAT = "lat"
        const val LNG = "lng"
        const val COOKING_STATUS_ID = "cookingStatusId"
        const val COOKING_STATUS = "cookingStatus"
        const val MENU_ID = "menuID"
        const val MAYBE_GO = "maybeGo"
        const val EVENT_ONLY = "eventOnly"
    }
}
@Entity(tableName = CookingDateDishesDB.TABLE_NAME,primaryKeys = ["dishId","cookingDateIdFk"], foreignKeys = arrayOf(
    ForeignKey(entity = CookingDateDB::class, parentColumns = arrayOf("cookingDateId"),childColumns = arrayOf("cookingDateIdFk"), onDelete = ForeignKey.CASCADE)
))
data class CookingDateDishesDB(
    @ColumnInfo(name = DISH_ID)
    var dishId : Int,
    @ColumnInfo(name = DISH_NAME)
    var dishName : String,
    @ColumnInfo(name = DISH_PRICE)
    var dishPrice : String,
    @ColumnInfo(name = DISH_FIFO)
    var dishFifo : Int,
    @ColumnInfo(name = DISH_INGREDIENTS)
    var dishIngredients : String,
    @ColumnInfo(name = DISH_DESCRIPTION)
    var dishDescription : String,
    @ColumnInfo(name = COOKING_DATE_ID_FK)
    var cookingDateIdFk : Int
){
    companion object {
        const val TABLE_NAME="CookingDateDishes"
        const val DISH_ID="dishId"
        const val DISH_NAME = "dishName"
        const val DISH_PRICE = "dishPrice"
        const val DISH_FIFO = "dishFifo"
        const val DISH_INGREDIENTS = "dishIngredients"
        const val DISH_DESCRIPTION = "dishDescription"
        const val COOKING_DATE_ID_FK = "cookingDateIdFk"
    }
}
data class CookingDateAndCookingDateDishes(
    @Embedded val cookingDate : CookingDateDB,
    @Relation (
        parentColumn = "cookingDateId",
        entityColumn = "cookingDateIdFk"
            )
    val cookingDateDishes : List<CookingDateDishesDB>
)
data class CookingDateAndCookingDateDishesWithOrder(
    @Embedded val cookingDateAndDishes : CookingDateAndCookingDateDishes,
    @Relation(
        parentColumn = "cookingDateId",
        entityColumn = "cookingDateIdFk",
        entity = OrderDB::class
    )
    val order : List<OrderWithDishes>
)
data class CookingDateAndCookingDateDishesWithOrderWithDishes(
    @Embedded val cookingDateAndDishesWithOrder : CookingDateAndCookingDateDishesWithOrder,
    @Relation(
        parentColumn = "orderId",
        entityColumn = "orderIdFk"
    )
    val orderAndOrderDishes : List<OrderWithDishes>
)