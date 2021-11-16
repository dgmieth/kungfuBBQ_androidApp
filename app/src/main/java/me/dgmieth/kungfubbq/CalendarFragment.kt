package me.dgmieth.kungfubbq

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.Observer
import com.applandeo.materialcalendarview.EventDay
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_calendar.*
import me.dgmieth.kungfubbq.datatabase.room.Actions
import me.dgmieth.kungfubbq.datatabase.room.KungfuBBQRoomDatabase
import me.dgmieth.kungfubbq.datatabase.room.RoomViewModel
import me.dgmieth.kungfubbq.datatabase.roomEntities.*
import me.dgmieth.kungfubbq.httpCtrl.HttpCtrl
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment(R.layout.fragment_calendar) {

    private val TAG = "CalendarFragment"

    private val events : MutableList<EventDay> = mutableListOf()
    private var cookingDates : List<CookingDateAndCookingDateDishesWithOrder>? = null
    private var datesArray : MutableList<Pair<String,Int>>? = null
    private var selectedCookingDate = 0

    private var viewModel: RoomViewModel? = null
    private var bag = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProviders.of(this).get(RoomViewModel::class.java)
        var db = KungfuBBQRoomDatabase.getInstance(requireActivity())
        viewModel?.getDbInstance(db)
        //subscribing to returnMsg
        viewModel?.returnMsg?.subscribe({
            Log.d(TAG,"returnMsg has value of $it")
            when(it){
                Actions.CookingDatesComplete -> {
                    viewModel?.getCookingDates()
                }
                else -> {
                    Handler(Looper.getMainLooper()).post{
                        Toast.makeText(requireActivity(),"It was not possible to retrieve information from kungfuBBQ server. Please try again later.",Toast.LENGTH_LONG).show()
                    }
                }
            }
        },{},{})?.let {
            bag.add(it)
        }
        //Subscribing to cookingD
        viewModel?.cookingDates?.subscribe({
            Log.d("CookingDateObservable","value is $it")
            cookingDates = it
            Log.d("CookingDates","array values is $cookingDates")
            cookingDates?.let{ cd ->
                var dArray : MutableList<Pair<String,Int>> = mutableListOf()
                for(i in cd) {
                    val cal = Calendar.getInstance()
                    var splitDate =
                        (i.cookingDateAndDishes.cookingDate.cookingDate.split(" ")[0]).split("-")
                    cal.set(splitDate[0].toInt(), splitDate[1].toInt()-1, splitDate[2].toInt())
                    events.add(EventDay(cal, R.drawable.icon_calendar))
                    dArray.add(Pair(cal.time.toString(),i.cookingDateAndDishes.cookingDate.cookingDateId))
                }
                datesArray = dArray
                Handler(Looper.getMainLooper()).post{
                    calendarSpinnerLayout.isVisible = false
                    ifSelectedDateHasCookingDateMath(calendarCalendar.firstSelectedDate.time.toString())
                    Log.d("CalendarCalendar","set events called")
                    calendarCalendar.setEvents(events)
                    calendarCalendar.setOnDayClickListener { eventDay ->
                        ifSelectedDateHasCookingDateMath(eventDay.calendar.time.toString())
                        println(eventDay.calendar.time.toString())
                    }
                }
            }
        },{
            Log.d("CookingDateObservable","error is $it")

        },{})?.let {
            bag.add(it)
        }
        viewModel?.user?.observe(viewLifecycleOwner, Observer {
            if(!it.user.email.isNullOrEmpty()){
                returnUserFromDBSuccess(it)
            }else{
                returnUserFromDBNull()
            }
        })
        viewModel?.getUser()
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        calendarMenu.movementMethod = ScrollingMovementMethod()
        calendarSpinnerLayout.visibility = View.VISIBLE
        val today = Date()
        val dateFormatter = SimpleDateFormat()
        dateFormatter.applyPattern("y")
        val year = (dateFormatter.format(today).toString()).toInt()
        dateFormatter.applyPattern("M")
        var month = (dateFormatter.format(today).toString()).toInt()-1
        dateFormatter.applyPattern("d")
        val date = (dateFormatter.format(today).toString()).toInt()
        val min = Calendar.getInstance()
        val max = Calendar.getInstance()
        val todayCal = Calendar.getInstance()
        min.set(year,month,date)
        min.add(Calendar.DAY_OF_MONTH,-1)
        todayCal.set(year,month,date,0,0,0)
        calendarCalendar.setDate(todayCal)
        //setting min date in calendar
        calendarCalendar.setMinimumDate(min)
        month += 1
        if(month > 11){
            max.set(Calendar.YEAR, year + 1)
            max.set(Calendar.MONTH, 0)
        }else{
            max.set(Calendar.YEAR, year)
            max.set(Calendar.MONTH, month)
        }
        //setting max date
        max.set(Calendar.DATE, max.getActualMaximum(Calendar.DATE))
        calendarCalendar.setMaximumDate(max)
        //setting onClickListener
        calendarPreOrder.setOnClickListener {
            val action = CalendarFragmentDirections.callPreOrder(selectedCookingDate)
            findNavController().navigate(action)
        }
        calendarUpdateOrder.setOnClickListener {
            val action = CalendarFragmentDirections.callUpdateOrder(selectedCookingDate)
            findNavController().navigate(action)
        }
        calendarPayOrder.setOnClickListener {
            val action = CalendarFragmentDirections.callPayOrder(selectedCookingDate)
            findNavController().navigate(action)
        }
        calendarPaidOrder.setOnClickListener {
            val action = CalendarFragmentDirections.callPaidOrder(selectedCookingDate)
            findNavController().navigate(action)
        }
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
    /*
    OTHER METHODS
     */
    private fun ifSelectedDateHasCookingDateMath(eventDay:String){
        datesArray?.let { it ->
            for(i in it){
                if(i.first==eventDay){
                    calendarNoCookingDate.visibility = View.INVISIBLE
                    val cdAr = cookingDates!!.filter {cd ->  cd.cookingDateAndDishes.cookingDate.cookingDateId == i.second }
                    val cd = cdAr[0]
                    val dates =  i.first.split(" ")
                    val complement = if (cd.cookingDateAndDishes.cookingDate.complement == "Not informed") "" else ", ${cd.cookingDateAndDishes.cookingDate.complement}"
                    var dateAddress = "${dates[1]} ${dates[2]} at ${cd.cookingDateAndDishes.cookingDate.street}${complement}"
                    dateAddress.also { calendarDate.text = it }
                    calendarStatus.text = cd.cookingDateAndDishes.cookingDate.cookingStatus
                    var menu = ""
                    var menuIndex = 1
                    for(m in cd.cookingDateAndDishes.cookingDateDishes){
                        menu = "${menu}${menuIndex}- ${m.dishName} \n"
                        menuIndex += 1
                    }
                    calendarMenu.text = menu
                    updateUIBtns(i.second)
                    break
                }else{
                    calendarNoCookingDate.visibility = View.VISIBLE
                }
            }
        }
    }
    private fun updateUIBtns(cookingDateId:Int){
        selectedCookingDate = cookingDateId
        cookingDates?.let {
            val cdArray = it.filter {e -> e.cookingDateAndDishes.cookingDate.cookingDateId == cookingDateId}
            val cd = cdArray[0]
            Log.d("UpdateUiBtns","cookingDate ${cd.cookingDateAndDishes.cookingDate}")
            if(cd.cookingDateAndDishes.cookingDate.cookingStatusId == 4){
                Log.d("UpdateUiBtns","cookingDate opened to orders")
                if(cd.order.isNotEmpty()){
                    Log.d("UpdateUiBtns","cookingDate opened to orders -> orders not empty")
                    showOrderBtns(
                        placeOrder= false,
                        updateOrder = true,
                        payOrder = false,
                        paidOrder = false
                    )
                }else{
                    Log.d("UpdateUiBtns","cookingDate opened to orders -> orders empty")
                    showOrderBtns(
                        placeOrder = true,
                        updateOrder = false,
                        payOrder = false,
                        paidOrder = false
                    )
                }
            }else {
                Log.d(TAG,"cookingDate close to orders")
                if(cd.order.isNotEmpty()){
                    Log.d(TAG,"order is ${cd.order}")
                    var order = cd.order[0].order
                    if (order.orderStatusId == 2 ){ /*Waiting cooking calendar date closure and sorting*/
                        /*show update btn*/
                        showOrderBtns(
                            placeOrder = false,
                            updateOrder = true,
                            payOrder = false,
                            paidOrder = false
                        )
                    }
                    if (order.orderStatusId == 3 ){ /*Waiting user confirmation/payment*/
                        /*show pay btn*/
                        showOrderBtns(
                            placeOrder = false,
                            updateOrder = false,
                            payOrder = true,
                            paidOrder = false
                        )
                    }
                    if (order.orderStatusId == 4 ){ /*Waiting selected users to drop out*/
                        /*create user alert*/
                        Log.d(TAG,"waiting dropouts")
                        showAlert("Your order did not make it to this list, but you are on the waiting list for drop out orders. You'll receive a notification if your order gets onto this list",
                                "Order status")
                    }
                    if (arrayListOf<Int>(5,8,9,10,11).contains(order.orderStatusId) ){ /*5-Confirmed/paid by user 8-Waiting order pickup alert 9- Waiting pickup 10- Delivered 11-Closed  */
                        /*show checkout pair order btn*/
                        showOrderBtns(
                            placeOrder = false,
                            updateOrder = false,
                            payOrder = false,
                            paidOrder = true
                        )
                    }
                    if (order.orderStatusId == 6 ){ /*Declined by user*/
                        /*create user alert*/
                        Log.d(TAG,"order declined")
                        showAlert("You cancelled this order if you wish to order food from us, please choose another available cooking date",
                            "Order status")
                    }
                    if (order.orderStatusId == 7 ){ /*Not made to this cookingCalendar date list*/
                        /*create user alert*/
                        showAlert("We are sorry! Unfortunately your order did not make to the final list on this cooking date. Please, order from us again on another available cooking date",
                            "Order status")
                    }
                    if (order.orderStatusId == 12 ){ /*The cooking calendar register was excluded by the database administrator, application user or routine*/
                        /*create user alert*/
                        Log.d(TAG,"did not make to this cd")
                        showAlert("You missed the time you had to confirm the order. Please order again from another available cooking date.",
                            "Order status")
                    }
                }else{
                    Log.d(TAG,"order is emtpy")
                    showOrderBtns(
                        placeOrder= false,
                        updateOrder = false,
                        payOrder = false,
                        paidOrder = false
                    )
                }

            }

        }
    }
    private fun showOrderBtns(placeOrder:Boolean, updateOrder:Boolean, payOrder:Boolean, paidOrder:Boolean){
        calendarPreOrder.visibility = if(placeOrder) View.VISIBLE else View.INVISIBLE
        calendarUpdateOrder.visibility = if(updateOrder) View.VISIBLE else View.INVISIBLE
        calendarPayOrder.visibility = if(payOrder) View.VISIBLE else View.INVISIBLE
        calendarPaidOrder.visibility = if(paidOrder) View.VISIBLE else View.INVISIBLE
    }
    private fun returnUserFromDBNull() {
        Handler(Looper.getMainLooper()).post{
            Toast.makeText(requireActivity(),"It was not possible to recover data from app`s database. Please restart the app.",Toast.LENGTH_LONG).show()
        }
    }

    private fun returnUserFromDBSuccess(it: UserAndSocialMedia) {
        var httpUrl = HttpUrl.Builder()
            .scheme("https")
            .host("${getString(R.string.kungfuServerUrlNoSchema)}")
            .addQueryParameter("email",it.user.email)
            .addQueryParameter("id",it.user.userId.toString())
            .addPathSegments("api/cookingCalendar/activeCookingDatesWithinSixtyDays")
            .build()
        HttpCtrl.shared.newCall(HttpCtrl.get("","",httpUrl,it.user.token)).enqueue(object :
            Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.d(TAG, "return is $e")
                Handler(Looper.getMainLooper()).post{
                    Toast.makeText(requireActivity(),"The attempt to retrieve data from KungfuBBQ server failed with generalized error message: ${e.localizedMessage}",
                        Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    for ((name, value) in response.headers) {
                        println("$name: $value")
                    }
                    val json = JSONObject(response.body!!.string())
                    if(!json.getBoolean("hasErrors")){
                        println(json)
                        var cDates = json.getJSONArray("msg").getJSONArray(0)
                        var orders = json.getJSONArray("msg").getJSONArray(1)
                        var listCDates : MutableList<CookingDateDB> = arrayListOf()
                        var listOrders : MutableList<OrderDB> = arrayListOf()
                        var listCDatesDishes : MutableList<CookingDateDishesDB> = arrayListOf()
                        var listOrderDishes : MutableList<OrderDishesDB> = arrayListOf()
                        for(i in 0 until cDates.length()){
                            val iJ = cDates.getJSONObject(i)
                            listCDates.add(CookingDateDB(
                                iJ.getInt("cookingDateId"),
                                iJ.getString("cookingDate"),
                                iJ.getInt("mealsForThis"),
                                iJ.getInt("addressId"),
                                iJ.getString("street"),
                                iJ.getString("complement"),
                                iJ.getString("city"),
                                iJ.getString("state"),
                                iJ.getString("zipcode"),
                                iJ.getString("country"),
                                iJ.getDouble("lat"),
                                iJ.getDouble("lng"),
                                iJ.getInt("cookingStatusId"),
                                iJ.getString("cookingStatus"),
                                iJ.getInt("menuID")))
                            for(x in 0 until iJ.getJSONArray("dishes").length()){
                                val ds = iJ.getJSONArray("dishes").getJSONObject(x)
                                listCDatesDishes.add(CookingDateDishesDB(ds.getInt("dishId"),
                                    ds.getString("dishName"),
                                    ds.getString("dishPrice"),
                                    ds.getString("dishIngredients"),
                                    ds.getString("dishDescription"),
                                    iJ.getInt("cookingDateId")))
                            }
                        }
                        for(i in 0 until orders.length()){
                            val iO = orders.getJSONObject(i)
                            listOrders.add(OrderDB(iO.getInt("orderId"),
                                iO.getString("orderDate"),
                                iO.getInt("cookingDateId"),
                                iO.getInt("orderStatusId"),
                                iO.getString("orderStatusName"),
                                iO.getInt("userId"),
                                iO.getString("userName"),
                                iO.getString("userEmail"),
                                iO.getString("userPhoneNumber")))
                            for(x in 0 until iO.getJSONArray("dishes").length()){
                                val ds = iO.getJSONArray("dishes").getJSONObject(x)
                                listOrderDishes.add(OrderDishesDB(ds.getInt("dishId"),
                                    ds.getString("dishName"),
                                    ds.getString("dishPrice"),
                                    ds.getInt("dishQtty"),
                                    ds.getString("observation"),
                                    iO.getInt("orderId")))
                            }
                        }
                        viewModel?.insertAllCookingDates(listCDates,listCDatesDishes,listOrders,listOrderDishes)
                    }else{
                        if(json.getInt("errorCode")==-1){
                            Handler(Looper.getMainLooper()).post{
                                Toast.makeText(requireActivity(),"${json.getString("msg")}",
                                    Toast.LENGTH_LONG).show()
                                val action = NavGraphDirections.callHome(false)
                                findNavController().navigate(action)
                            }
                        }else{
                            Handler(Looper.getMainLooper()).post{
                                Toast.makeText(requireActivity(),"The attempt to retrieve data from KungfuBBQ server failed with server message: ${json.getString("msg")}",
                                    Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        })
    }
    private fun showAlert(message:String,title:String){
        Handler(Looper.getMainLooper()).post{
            var dialogBuilder = AlertDialog.Builder(activity)
            dialogBuilder.setMessage(message)
                .setCancelable(true)
                .setNegativeButton("Cancel",DialogInterface.OnClickListener{
                        _, _ ->
                })
                .setPositiveButton("Ok", DialogInterface.OnClickListener{
                        _, _ ->
                })
            val alert = dialogBuilder.create()
            alert.setTitle(title)
            alert.show()
        }
    }
}