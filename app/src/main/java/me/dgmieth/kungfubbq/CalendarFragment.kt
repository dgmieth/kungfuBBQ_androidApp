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
import java.util.*

class CalendarFragment : Fragment(R.layout.fragment_calendar) {

    private val TAG = "CalendarFragment"

    private val events : MutableList<EventDay> = mutableListOf()
    private var bag = CompositeDisposable()

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
        viewModel?.returnMsg?.subscribe({
            Log.d(TAG,"returnMsg has value of $it")
                when(it){
                    Actions.CookingDatesComplete -> {
                        viewModel?.getCookingDate(23)
                    }
                    else -> {

                    }
                }
        },{},{})?.let {
            bag.add(it)
        }
        viewModel?.user?.observe(viewLifecycleOwner, Observer {
            if(!it.user.email.isNullOrEmpty()){
                returnUserFromDBSuccess(it)
            }else{
                returnUserFromDBNull()
            }
        })
        viewModel?.order?.observe(viewLifecycleOwner, Observer {
            Log.d(TAG,"order returned was ${it}")
        })
        viewModel?.getUser()
        return super.onCreateView(inflater, container, savedInstanceState)
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

                                Log.d(TAG, "Dishes are ${listOrders}")
                        viewModel?.insertAllCookingDates(listCDates,listCDatesDishes,listOrders,listOrderDishes)
//                        Handler(Looper.getMainLooper()).post {
//                            viewModel?.getCookingDate(23)
//                        }
                        Log.d(TAG, "return cDates are $cDates")
                        Log.d(TAG, "Orders are $orders")
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        for(i in 1..4){
            val cal = Calendar.getInstance()
            cal.set(2021,9,(i*7))
            events.add(EventDay(cal,R.drawable.icon_calendar))
        }
        for(i in 1..4){
            val cal = Calendar.getInstance()
            cal.set(2021,10,(i*7))
            events.add(EventDay(cal,R.drawable.icon_calendar))
        }
        calendarCalendar.setEvents(events)
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
        calendarCalendar.setOnDayClickListener { eventDay ->
            println(eventDay.calendar.time.toString())
        }
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
}