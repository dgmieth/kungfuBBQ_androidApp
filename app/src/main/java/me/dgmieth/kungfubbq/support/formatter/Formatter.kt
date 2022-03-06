package me.dgmieth.kungfubbq.support.formatter

import android.text.Html
import com.onesignal.HTML
import me.dgmieth.kungfubbq.datatabase.roomEntities.CookingDateDishesDB

const val TAG = "FORMATTER"

class FormatObject {
    companion object{
        var shared = FormatObject()
        private var mealBoxTotalAmount = 0.0
        private var dishesArray : MutableList<Int> = kotlin.collections.mutableListOf()
        private var address = ""
        private var timeVal = ""

        fun formatDishesListForMenuScrollViews(ary:List<CookingDateDishesDB>):String{
            var menu = ""
            var menuIndex = 1
            //clearing list and total amount
            dishesArray.clear()
            mealBoxTotalAmount = 0.0
            //if only one dish in array -> system follows first business rule
            if(ary.count()==1){
                for(m in ary) {
                    menu = "${menu}<p>${menuIndex}- ${m.dishName}</p>"
                    menuIndex += 1
                    mealBoxTotalAmount += m.dishPrice.toDouble()
                    dishesArray.add(m.dishId)
                }
                return menu
            }
            //if more than one dish in array -> system follows new business rule
            menu = "<p><strong>BOX MEAL</strong></p>"
            var fifoIndex = 1
            val fifoIntro = "<p><strong>FIRST COME, FIRST SERVED</strong></p>"
            var fifo = "$fifoIntro"
            for(m in ary){
                if(m.dishFifo==0){
                    menu = "${menu}<p>${menuIndex}- ${m.dishName}${if(m.dishDescription != "") " (${m.dishDescription})" else ""}</p>"
                    menuIndex += 1
                    mealBoxTotalAmount += m.dishPrice.toDouble()
                    dishesArray.add(m.dishId)
                }else{
                    fifo = "${fifo}<p>${fifoIndex}- ${m.dishName}${if(m.dishDescription != "") " (${m.dishDescription})" else ""}</p>"
                    fifoIndex += 1
                }
            }
            menu = "${menu}${if(fifo != fifoIntro) fifo else ""}"
            return menu
        }
        fun formatEventAddress(monthValue:Int,dayMonth:Int,time:String,street:String?,complement:String?,city:String?,state:String?,zipCode:String?): String {
            val month = when(monthValue) {
                0 -> "Jan"
                1 -> "Feb"
                2 -> "Mar"
                3 -> "Apr"
                4 -> "May"
                5 -> "Jun"
                6 -> "Jul"
                7 -> "Aug"
                8 -> "Sep"
                9 -> "Oct"
                10 -> "Nov"
                else -> "Dec"
            }
            address = ""
            timeVal = "$month $dayMonth at $time"
            street?.let { it -> address = "$address${if(it==""||it=="Not informed") "" else "$it,"}" }
            complement?.let { it -> address = "$address${if(it==""||it=="Not informed") "" else " $it,"}" }
            city?.let { it -> address = "$address${if(it==""||it=="Not informed") "" else " $it"}" }
            state?.let { it -> address = "$address${if(it==""||it=="Not informed") "" else " - $it"}" }
            zipCode?.let { it -> address = "$address${if(it==""||it=="Not informed") "" else " - $it"}" }
            return "<strong>$month $dayMonth</strong> at <strong>$time</strong> at <strong>$address</strong>"
        }
        fun returnAddress():String{
            return address
        }
        fun returnEventTime():String{
            return timeVal
        }
        fun getDishesForOrders():MutableList<Int>{
            return this.dishesArray
        }
        fun returnMealBoxTotalAmount():Double{
            return this.mealBoxTotalAmount
        }
        fun returnTotalAmountDue(mealsQtty:Int):Double{
            return this.mealBoxTotalAmount*mealsQtty
        }
    }
}