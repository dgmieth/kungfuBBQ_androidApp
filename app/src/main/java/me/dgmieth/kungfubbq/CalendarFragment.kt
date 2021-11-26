package me.dgmieth.kungfubbq

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
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
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import io.reactivex.disposables.CompositeDisposable
import me.dgmieth.kungfubbq.databinding.CalendarDayLayoutBinding
import me.dgmieth.kungfubbq.databinding.CalendarMonthHeaderLayoutBinding
import me.dgmieth.kungfubbq.databinding.FragmentCalendarBinding
import me.dgmieth.kungfubbq.datatabase.room.Actions
import me.dgmieth.kungfubbq.datatabase.room.KungfuBBQRoomDatabase
import me.dgmieth.kungfubbq.datatabase.room.RoomViewModel
import me.dgmieth.kungfubbq.datatabase.roomEntities.*
import me.dgmieth.kungfubbq.httpCtrl.HttpCtrl
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

class CalendarFragment : Fragment(R.layout.fragment_calendar){

    private val TAG = "CalendarFragment"
    private val events : MutableList<LocalDate> = mutableListOf()
    private var cookingDates : List<CookingDateAndCookingDateDishesWithOrder>? = null
    private var datesArray : MutableList<Pair<LocalDate,Int>>? = null
    private var selectedCookingDate = 0

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private var viewModel: RoomViewModel? = null
    private var bag = CompositeDisposable()

    private var selectedDate = LocalDate.now()
    private var todayDate = LocalDate.now()
    private val dateFormatterCV = DateTimeFormatter.ofPattern("dd")

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
        viewModel?.getDbInstance(KungfuBBQRoomDatabase.getInstance(requireActivity()))
        //subscribing to returnMsg
        viewModel?.returnMsg?.subscribe({
            showSpinner(false)
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
        },{
            showSpinner(false)
            Handler(Looper.getMainLooper()).post{
                Toast.makeText(requireActivity(),"It was not possible to retrieve information from kungfuBBQ server. Please try again later.",Toast.LENGTH_LONG).show()
            }
        },{})?.let {
            bag.add(it)
        }
        //Subscribing to cookingD
        viewModel?.cookingDates?.subscribe({
            cookingDates = it
            cookingDates?.let{ cd ->
                var dArray : MutableList<Pair<LocalDate,Int>> = mutableListOf()
                for(i in cd) {
                    var splitDate =
                        (i.cookingDateAndDishes.cookingDate.cookingDate.split(" ")[0]).split("-")
                    val localDate = LocalDate.of(splitDate[0].toInt(),splitDate[1].toInt(),splitDate[2].toInt())
                    events.add(localDate)
                    Handler(Looper.getMainLooper()).post {
                        binding.calendarView?.notifyDateChanged(localDate)
                    }
                    dArray.add(Pair(localDate,i.cookingDateAndDishes.cookingDate.cookingDateId))
                }
                datesArray = dArray
                Handler(Looper.getMainLooper()).post{
                    val oldDate = selectedDate
                    refreshCalendarView(todayDate,oldDate)
                    ifSelectedDateHasCookingDateMatch(selectedDate)
                    showSpinner(false)
                }
            }
        },{
            Log.d("CookingDateObservable","error is $it")
        },{})?.let {
            bag.add(it)
        }
        viewModel?.user?.observe(viewLifecycleOwner, Observer {
            if(!it.user.email.isNullOrEmpty()){
                getActiveCookingDatesWithinSixtyDays(it)
            }else{
                returnUserFromDBNull()
            }
        })
        viewModel?.getUser()
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showSpinner(true)
        binding.calendarMenu.movementMethod = ScrollingMovementMethod()

        //setting min date in calendar
        class MonthViewContainer(view: View) : ViewContainer(view) {
            val bind = CalendarMonthHeaderLayoutBinding.bind(view)
            fun bind(day: CalendarMonth) {
                bind.headerTextView.text = day.yearMonth.month.name
            }
        }
        binding.calendarView.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, month: CalendarMonth) = container.bind(month)
        }
        class DayViewContainer(view: View) : ViewContainer(view) {
            val bind = CalendarDayLayoutBinding.bind(view)
            lateinit var day: CalendarDay

            init {
                view.setOnClickListener {
                    if(day.owner == DayOwner.THIS_MONTH){
                        if(day.date >= todayDate){
                            if (selectedDate != day.date) {
                                val oldDate = selectedDate
                                selectedDate = day.date
                                Log.d(TAG,"newSelectedDate is $selectedDate")
                                refreshCalendarView(day.date,oldDate)
                                ifSelectedDateHasCookingDateMatch(selectedDate)
                            }
                        }
                    }
                }
            }
            fun bind(day: CalendarDay) {
                Log.d(TAG, "bind called")
                this.day = day
                bind.todayDay.text = dateFormatterCV.format(day.date)
                datesArray?.let{
                    if(it.size>0){
                        for(i in it){
                            if(i.first==day.date){
                                bind.kungfuBBQImg.visibility = View.VISIBLE
                            }
                        }
                    }
                }
                if(selectedDate==day.date){
                    bind.selectionCircle.visibility = View.VISIBLE
                    bind.todayDay.setTextColor(Color.BLACK)
                    return
                }
                bind.selectionCircle.visibility = View.INVISIBLE
                if(day.date == todayDate){
                    bind.todayCircle.visibility = View.VISIBLE
                    bind.todayDay.setTextColor(Color.BLACK)
                } else {
                    if (day.owner == DayOwner.THIS_MONTH) {
                        if (day.date < todayDate) {
                            bind.todayDay.setTextColor(Color.GRAY)
                        }else {
                            bind.todayDay.setTextColor(Color.WHITE)
                        }
                    } else {
                        bind.todayDay.setTextColor(Color.BLACK)
                        bind.kungfuBBQImg.visibility = View.INVISIBLE
                    }
                    bind.todayCircle.isVisible = false
                }
            }
        }
        binding.calendarView.dayBinder = object : DayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.bind(day)
            }
        }
        val currentMonth = YearMonth.now()
        // Value for firstDayOfWeek does not matter since inDates and outDates are not generated.
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        binding.calendarView.setup(currentMonth, currentMonth.plusMonths(1), firstDayOfWeek)
        binding.calendarView.scrollToDate(LocalDate.now())
        binding.calendarView.scrollToDate(selectedDate)

        //setting onClickListener
        binding.calendarPreOrder.setOnClickListener {
            val action = CalendarFragmentDirections.callPreOrder(selectedCookingDate)
            findNavController().navigate(action)
        }
        binding.calendarUpdateOrder.setOnClickListener {
            val action = CalendarFragmentDirections.callUpdateOrder(selectedCookingDate)
            findNavController().navigate(action)
        }
        binding.calendarPayOrder.setOnClickListener {
            val action = CalendarFragmentDirections.callPayOrder(selectedCookingDate)
            findNavController().navigate(action)
        }
        binding.calendarPaidOrder.setOnClickListener {
            val action = CalendarFragmentDirections.callPaidOrder(selectedCookingDate)
            findNavController().navigate(action)
        }
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
    /*
     * HTTP request
     */
    private fun getActiveCookingDatesWithinSixtyDays(it: UserAndSocialMedia) {
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
                    val json = JSONObject(response.body!!.string())
                    if(!json.getBoolean("hasErrors")){
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
                                Toast.makeText(requireActivity(),"You are not authenticated in Kungfu BBQ server anylonger. Please log in again.",
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
    // ==============================================
    // UI update
    private fun ifSelectedDateHasCookingDateMatch(eventDay:LocalDate){
        datesArray?.let { it ->
            for(i in it){
                if(i.first==eventDay){
                    binding.calendarNoCookingDate.visibility = View.INVISIBLE
                    val cdAr = cookingDates!!.filter {cd ->  cd.cookingDateAndDishes.cookingDate.cookingDateId == i.second }
                    val cd = cdAr[0]
                    val month = when(i.first.month.value-1) {
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
                    val complement = if (cd.cookingDateAndDishes.cookingDate.complement == "Not informed") "" else ", ${cd.cookingDateAndDishes.cookingDate.complement}"
                    var dateAddress = "$month ${i.first.dayOfMonth} at ${cd.cookingDateAndDishes.cookingDate.street}${complement}"
                    dateAddress.also { binding.calendarDate.text = it }
                    binding.calendarStatus.text = cd.cookingDateAndDishes.cookingDate.cookingStatus
                    var menu = ""
                    var menuIndex = 1
                    for(m in cd.cookingDateAndDishes.cookingDateDishes){
                        menu = "${menu}${menuIndex}- ${m.dishName} \n"
                        menuIndex += 1
                    }
                    binding.calendarMenu.text = menu
                    updateUIBtns(i.second)
                    break
                }else{
                    binding.calendarNoCookingDate.visibility = View.VISIBLE
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
    private fun updateUIBtns(cookingDateId:Int){
        selectedCookingDate = cookingDateId
        cookingDates?.let {
            val cdArray = it.filter {e -> e.cookingDateAndDishes.cookingDate.cookingDateId == cookingDateId}
            val cd = cdArray[0]
            if(cd.cookingDateAndDishes.cookingDate.cookingStatusId == 4){
                if(cd.order.isNotEmpty()){
                    showOrderBtns(
                        placeOrder= false,
                        updateOrder = true,
                        payOrder = false,
                        paidOrder = false
                    )
                }else{
                    showOrderBtns(
                        placeOrder = true,
                        updateOrder = false,
                        payOrder = false,
                        paidOrder = false
                    )
                }
            }else {
                if(cd.order.isNotEmpty()){
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
        binding.calendarPreOrder.visibility = if(placeOrder) View.VISIBLE else View.INVISIBLE
        binding.calendarUpdateOrder.visibility = if(updateOrder) View.VISIBLE else View.INVISIBLE
        binding.calendarPayOrder.visibility = if(payOrder) View.VISIBLE else View.INVISIBLE
        binding.calendarPaidOrder.visibility = if(paidOrder) View.VISIBLE else View.INVISIBLE
    }
    private fun returnUserFromDBNull() {
        Handler(Looper.getMainLooper()).post{
            Toast.makeText(requireActivity(),"It was not possible to recover data from app`s database. Please restart the app.",Toast.LENGTH_LONG).show()
        }
    }
    private fun refreshCalendarView(newDate:LocalDate,oldDate:LocalDate){
        binding.calendarView.notifyDateChanged(newDate)
        oldDate?.let { binding.calendarView.notifyDateChanged(it) }
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
    private fun showSpinner(value: Boolean){
        Handler(Looper.getMainLooper()).post {
            binding.calendarSpinnerLayout.visibility =  when(value){
                true -> View.VISIBLE
                else -> View.INVISIBLE
            }
        }
    }
}