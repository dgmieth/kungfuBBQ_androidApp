package me.dgmieth.kungfubbq

import android.app.Notification
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class CalendarFragment : Fragment(R.layout.fragment_calendar) {

    private val TAG = "CalendarFragment"

    private val events : MutableList<EventDay> = mutableListOf()
    private var bag = CompositeDisposable()
    private var cookingDates : List<CookingDateAndCookingDateDishesWithOrder>? = null
    private var datesArray : MutableList<Pair<String,Int>>? = null

    private var viewModel: RoomViewModel? = null

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
                    Log.d("CookingDates","split array is $splitDate")
                    cal.set(splitDate[0].toInt(), splitDate[1].toInt()-1, splitDate[2].toInt())
                    Log.d("CookingDates","calendar is $cal")
                    events.add(EventDay(cal, R.drawable.icon_calendar))

                    dArray.add(Pair(cal.time.toString(),i.cookingDateAndDishes.cookingDate.cookingDateId))
                }
                datesArray = dArray
                
                Handler(Looper.getMainLooper()).post{
                    Log.d("CalendarCalendar","set events called")
                    calendarCalendar.setEvents(events)
                    calendarCalendar.setOnDayClickListener { eventDay ->
                        //Log.d("DatesArray","Teste ${datesArray!!}")
                        datesArray?.let { it ->
                            for(i in it){
                                println(i.first)
                                if(i.first==eventDay.calendar.time.toString()){
                                    val cdAr = cookingDates!!.filter {cd ->  cd.cookingDateAndDishes.cookingDate.cookingDateId == i.second }
                                    val cd = cdAr[0]
                                    val dates =  i.first.split(" ")
                                    Log.d("DatesArray","$dates")
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
                                    calendarMenu.setText(menu)
                                }else{

                                }
                            }
                        }
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
        Log.d("CalendarFragment", "Year $year, Month $month, Date $date")
        min.set(year,month,date)
        calendarCalendar.setDate(min)
        min.set(Calendar.DATE,1)
        Log.d("CalendarFragment", "$min")
        //setting min date in calendar
        calendarCalendar.setMinimumDate(min)
        month += 2
        if(month > 11){
            max.set(Calendar.YEAR, year + 1)
            max.set(Calendar.MONTH, 0)
        }else{
            max.set(Calendar.YEAR, year)
            max.set(Calendar.MONTH, month)
        }
        //setting max date
        max.set(Calendar.DATE, min.getActualMaximum(Calendar.DATE))
        Log.d("CalendarFragment", "Year $year, Month $month, Date $date")
        Log.d("CalendarFragment", "$max")
        calendarCalendar.setMaximumDate(max)
        //setting onClickListener

        calendarPreOrder.setOnClickListener {
            val action = CalendarFragmentDirections.callPreOrder()
            findNavController().navigate(action)
        }
        calendarUpdateOrder.setOnClickListener {
            val action = CalendarFragmentDirections.callUpdateOrder()
            findNavController().navigate(action)
        }
        calendarPayOrder.setOnClickListener {
            val action = CalendarFragmentDirections.callPayOrder()
            findNavController().navigate(action)
        }
        calendarPaidOrder.setOnClickListener {
            val action = CalendarFragmentDirections.callPaidOrder()
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
                        var cDates = json.getJSONArray("data").getJSONArray(0)
                        var orders = json.getJSONArray("data").getJSONArray(1)
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
                                Toast.makeText(requireActivity(),"The attempt to retrieve data from KungfuBBQ server failed with generalized server message: ${json.getString("msg")}",
                                    Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        })
    }
}