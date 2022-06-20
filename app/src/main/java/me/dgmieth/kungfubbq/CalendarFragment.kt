package me.dgmieth.kungfubbq

import androidx.appcompat.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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
import me.dgmieth.kungfubbq.support.formatter.FormatObject
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

var selectedDate = LocalDate.now()

class CalendarFragment : Fragment(R.layout.fragment_calendar){

    private val TAG = "CalendarFragment"
    private val events : MutableList<LocalDate> = mutableListOf()
    private var cookingDates : List<CookingDateAndCookingDateDishesWithOrder>? = null
    private var datesArray : MutableList<Pair<LocalDate,Int>>? = null
    private var selectedCookingDate = 0
    private var user: UserAndSocialMedia? = null

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private var btnClick = true

    private var viewModel: RoomViewModel? = null
    private var bag = CompositeDisposable()

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
                    showAlert("It was not possible to retrieve information from kungfuBBQ server. Please try again later.","Error!")
                }
            }
        },{
            showSpinner(false)
            showAlert("It was not possible to retrieve information from kungfuBBQ server. Please try again later.","Error!")
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
                        (i.cookingDateAndDishes.cookingDate.cookingDateAmPm.split(" ")[0]).split("-")
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
                    binding.calendarView.monthScrollListener = {month ->
                        var oldDate: LocalDate
                        //selectedDate = null
                        binding.calendarView.notifyCalendarChanged()
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
                user = it
                getActiveCookingDatesNextTwelveMonths(it)
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
                    if(day.owner >= DayOwner.THIS_MONTH){
                            if (day.date >= todayDate) {
                                if (selectedDate != day.date) {
                                    val oldDate = selectedDate
                                    selectedDate = day.date
                                    refreshCalendarView(day.date, oldDate)
                                    ifSelectedDateHasCookingDateMatch(selectedDate)
                                }
                            }
                        }
                }
            }
            fun bind(day: CalendarDay) {
                this.day = day
                bind.todayDay.text = dateFormatterCV.format(day.date)
                bind.kungfuBBQImg.visibility = View.INVISIBLE
                datesArray?.let{
//                    if(it.size>0){
//                        for(i in it){
//                            Log.d(TAG,"unfo ${i.first} ${day.date}")
//                            if(i.first==day.date){
//                                Log.d(TAG,"foundMatch ${i.first} ${day.date}")
//                                bind.kungfuBBQImg.visibility = View.VISIBLE
//                            }
//                        }
//                    }
                    if((it.filter { i -> i.first == day.date }).isNotEmpty()){
                        bind.kungfuBBQImg.visibility = View.VISIBLE
                    }
                }

                bind.selectionCircle.visibility = View.INVISIBLE
                bind.todayCircle.visibility = View.INVISIBLE
                bind.todayDay.setTextColor(Color.BLACK)

                if (day.owner == DayOwner.THIS_MONTH ) {
                    when {
                        day.date == todayDate -> {
                            bind.todayCircle.visibility = View.VISIBLE
                            bind.todayDay.setTextColor(Color.BLACK)
                        }
                        day.date < todayDate -> {
                            bind.todayDay.setTextColor(Color.TRANSPARENT)
                        }
                        else -> {
                            bind.todayDay.setTextColor(Color.WHITE)
                        }
                    }
                } else if (day.owner == DayOwner.PREVIOUS_MONTH || day.owner == DayOwner.NEXT_MONTH)  {
                    bind.todayDay.setTextColor(Color.TRANSPARENT)
                    bind.kungfuBBQImg.visibility = View.INVISIBLE
                    bind.selectionCircle.visibility = View.INVISIBLE
                }
                if(selectedDate == day.date){
                    if(day.owner == DayOwner.THIS_MONTH){
                        bind.selectionCircle.visibility = View.VISIBLE
                        bind.todayDay.setTextColor(Color.BLACK)
                    }
                }else{
                    bind.selectionCircle.visibility = View.INVISIBLE
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
        binding.calendarView.setup(currentMonth, currentMonth.plusMonths(12), firstDayOfWeek)
        binding.calendarView.scrollToDate(LocalDate.now())
        binding.calendarView.scrollToDate(selectedDate)
        //setting onClickListener
        binding.calendarPreOrder.setOnClickListener {
            btnClick = true
            val action = CalendarFragmentDirections.callPreOrder(selectedCookingDate)
            findNavController().navigate(action)
        }
        binding.calendarUpdateOrder.setOnClickListener {
            btnClick = true
            val action = CalendarFragmentDirections.callUpdateOrder(selectedCookingDate)
            findNavController().navigate(action)
        }
        binding.calendarPayOrder.setOnClickListener {
            btnClick = true
            val action = CalendarFragmentDirections.callPayOrder(selectedCookingDate)
            findNavController().navigate(action)
        }
        binding.calendarPaidOrder.setOnClickListener {
            btnClick = true
            val action = CalendarFragmentDirections.callPaidOrder(selectedCookingDate)
            findNavController().navigate(action)
        }
        binding.calendarConfirm.setOnClickListener {
            confirmPresence()
        }
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }

    override fun onResume() {
        super.onResume()
        if(!btnClick){
            val action = HomeFragmentDirections.callHome(true)
            findNavController().navigate(action)
        }
        btnClick = false
    }
    /*
     * HTTP request
     */
    private fun getActiveCookingDatesNextTwelveMonths(it: UserAndSocialMedia) {
        var httpUrl = HttpUrl.Builder()
            .scheme("https")
            .host("${getString(R.string.kungfuServerUrlNoSchema)}")
            .addQueryParameter("email",it.user.email)
            .addQueryParameter("id",it.user.userId.toString())
            .addQueryParameter("version_code","${BuildConfig.VERSION_CODE}")
            .addQueryParameter("mobileOS","android")
            .addPathSegments("api/cookingCalendar/activeCookingDateWithinNextTwelveMonths")
            .build()
        HttpCtrl.shared.newCall(HttpCtrl.get("","",httpUrl,it.user.token)).enqueue(object :
            Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                showAlert("The attempt to retrieve data from KungfuBBQ server failed with generalized error message: ${e.localizedMessage}","Error!")
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
                            Log.d(TAG, "maybe go ${iJ.isNull("maybeGo")}")
                            listCDates.add(CookingDateDB(
                                iJ.getInt("cookingDateId"),
                                iJ.getString("cookingDate"),
                                iJ.getString("cookingDateAmPm"),
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
                                iJ.getInt("menuID"),
                                iJ.getString("endTime"),
                                iJ.getString("cookingDateEndAmPm"),
                                iJ.getString("venue"),
                                if (iJ.isNull("maybeGo")) 0 else iJ.getInt("maybeGo") ,
                                if (iJ.isNull("eventOnly")) 0 else iJ.getInt("eventOnly")
                            ))
                            for(x in 0 until iJ.getJSONArray("dishes").length()){
                                val ds = iJ.getJSONArray("dishes").getJSONObject(x)
                                listCDatesDishes.add(CookingDateDishesDB(ds.getInt("dishId"),
                                    ds.getString("dishName"),
                                    ds.getString("dishPrice"),
                                    ds.getInt("dishFifo"),
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
                                iO.getDouble("tipAmount"),
                                iO.getString("userPhoneNumber")))
                            for(x in 0 until iO.getJSONArray("dishes").length()){
                                val ds = iO.getJSONArray("dishes").getJSONObject(x)
                                listOrderDishes.add(OrderDishesDB(ds.getInt("dishId"),
                                    ds.getString("dishName"),
                                    ds.getString("dishPrice"),
                                    ds.getInt("dishFifo"),
                                    ds.getInt("dishQtty"),
                                    ds.getString("observation"),
                                    iO.getInt("orderId")))
                            }
                        }
                        viewModel?.insertAllCookingDates(listCDates,listCDatesDishes,listOrders,listOrderDishes)
                    }else{
                        if(json.getInt("errorCode")==-1){
                            showSpinner(false)
                            showAlert("${getString(R.string.not_logged_in_message)}","${getString(R.string.not_logged_in)}")
                        }else{
                            showSpinner(false)
                            showAlert("The attempt to retrieve data from KungfuBBQ server failed with server message: ${json.getString("msg")}","Error!")
                            noCookingDateMessage(true)
                        }
                    }
                }
            }
        })
    }
    private fun confirmPresence(){
        user?.let {
            val body = FormBody.Builder()
                .add("email",it.user.email)
                .add("id",it.user.userId.toString())
                .add("cookingDate_id",selectedCookingDate.toString())
                .build()
            HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/api/cookingCalendar/confirmPresence",body,it.user.token.toString())).enqueue(object :
                Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    showSpinner(false)
                    showAlert("The attempt to confirm your presence failed with server message: ${e.localizedMessage}.","Confirmation failed!")
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        showSpinner(false)
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        val json = JSONObject(response.body!!.string())
                        if(!json.getBoolean("hasErrors")){
                            val msg = json.getString("msg")
                            showAlert(msg,"Presence confirmed!")
                        }else{
                            if(json.getInt("errorCode")==-1){
                                showAlert("${getString(R.string.not_logged_in_message)}","${getString(R.string.not_logged_in)}")
                            }else{
                                showAlert("The attempt to confirm your presence failed with server message: ${json.getString("msg")}","Confirmation failed!")
                            }
                        }
                    }
                }
            })
        }
    }
    // ==============================================
    // UI update
    private fun ifSelectedDateHasCookingDateMatch(eventDay:LocalDate){
        selectedDate = eventDay
        datesArray?.let { it ->
            for(i in it){
                if(i.first==eventDay){
//                    binding.calendarNoCookingDate.visibility = View.INVISIBLE
                    noCookingDateMessage(false)
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
//                    var dateAddress = "$month ${i.first.dayOfMonth} at ${cd.cookingDateAndDishes.cookingDate.cookingDate.split("-")[1]} \n ${FormatObject.formatEventAddress(cd.cookingDateAndDishes.cookingDate.street,cd.cookingDateAndDishes.cookingDate.complement,cd.cookingDateAndDishes.cookingDate.city,cd.cookingDateAndDishes.cookingDate.state,cd.cookingDateAndDishes.cookingDate.zipcode)}"
                    binding.calendarDate.text = Html.fromHtml(FormatObject.formatEventAddress(i.first.month.value-1,i.first.dayOfMonth,cd.cookingDateAndDishes.cookingDate.cookingDateAmPm.split(" ")[1],cd.cookingDateAndDishes.cookingDate.street,cd.cookingDateAndDishes.cookingDate.city,cd.cookingDateAndDishes.cookingDate.state,cd.cookingDateAndDishes.cookingDate.cookingDateEndAmPm.split(" ")[1],cd.cookingDateAndDishes.cookingDate.venue),Html.FROM_HTML_MODE_COMPACT)
                    Log.d(TAG, "lines of text ${binding.calendarDate.text.lines()[binding.calendarDate.text.lines().size-1]}")
                    if(binding.calendarDate.text.lines()[binding.calendarDate.text.lines().size-1].isNullOrEmpty()){
                        if(binding.calendarDate.text.lines().size-1<=4){
                            binding.calendarDate.maxLines = binding.calendarDate.text.lines().size-1
                        }
                    }
                    binding.calendarStatus.text = cd.cookingDateAndDishes.cookingDate.cookingStatus
//                    var menu = "<p><strong>BOX MEAL:</strong></p>"
//                    var menuIndex = 1
//                    var fifoIndex = 1
//                    val fifoIntro = "<p><strong>${getString(R.string.first_come_first_served)}:</strong></p>"
//                    var fifo = "$fifoIntro"
//                    for(m in cd.cookingDateAndDishes.cookingDateDishes){
//                        if(m.dishFifo==0){
//                            menu = "${menu}<p>${menuIndex}- ${m.dishName}${if(m.dishDescription != "") "( ${m.dishDescription})" else ""}</p>"
//                            menuIndex += 1
//                        }else{
//                            fifo = "${fifo}<p>${fifoIndex}- ${m.dishName}${if(m.dishDescription != "") "( ${m.dishDescription})" else ""}</p>"
//                            fifoIndex += 1
//                        }
//                    }
//                    menu = "${menu}${if(fifo != fifoIntro) fifo else ""}"
//                    Log.d(TAG,"html is $menu")
//                    binding.calendarMenu.text = Html.fromHtml(menu,Html.FROM_HTML_MODE_COMPACT)
                    binding.calendarMenu.text = Html.fromHtml(FormatObject.formatDishesListForMenuScrollViews(cd.cookingDateAndDishes.cookingDateDishes),Html.FROM_HTML_MODE_COMPACT)
                    //Log.d(TAG,"${FormatObject.formatEventAddress(cd.cookingDateAndDishes.cookingDate.street,cd.cookingDateAndDishes.cookingDate.complement,cd.cookingDateAndDishes.cookingDate.city,cd.cookingDateAndDishes.cookingDate.state,cd.cookingDateAndDishes.cookingDate.zipcode)}")
                    if(binding.calendarMenu.canScrollVertically(1) || binding.calendarMenu.canScrollVertically(-1)){
                        binding.calendarMenu.scrollTo(0,0)
                        binding.calendarScrollImg.visibility = View.VISIBLE
                    }else {
                        binding.calendarScrollImg.visibility = View.INVISIBLE
                    }
                    Log.d(TAG, "height is ${binding.calendarMenu.height}")
                    Log.d(TAG, "height is ${binding.calendarMenu.canScrollVertically(1)} ${binding.calendarMenu.canScrollVertically(-1)}")
                    updateUIBtns(i.second)
                    break
                }else{
//                    binding.calendarNoCookingDate.visibility = View.VISIBLE
                    noCookingDateMessage(true)
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
    private fun noCookingDateMessage(show:Boolean){
        Handler(Looper.getMainLooper()).post {
            if(show){
                binding.calendarNoCookingDate.visibility = View.VISIBLE
                binding.calendarCookingDate.visibility = View.INVISIBLE
            }else{
                binding.calendarNoCookingDate.visibility = View.INVISIBLE
                binding.calendarCookingDate.visibility = View.VISIBLE
            }
        }
    }
    private fun updateUIBtns(cookingDateId:Int){
        selectedCookingDate = cookingDateId
        cookingDates?.let {
            val cdArray = it.filter {e -> e.cookingDateAndDishes.cookingDate.cookingDateId == cookingDateId}
            val cd = cdArray[0]
            if(cd.cookingDateAndDishes.cookingDate.cookingStatusId == 4) {
                if (cd.order.isNotEmpty()) {
                    showOrderBtns(
                        placeOrder = false,
                        updateOrder = true,
                        payOrder = false,
                        paidOrder = false
                    )
                } else {
                    showOrderBtns(
                        placeOrder = true,
                        updateOrder = false,
                        payOrder = false,
                        paidOrder = false
                    )
                }
            }else if(cd.cookingDateAndDishes.cookingDate.cookingStatusId < 4) {
                showOrderBtns(
                    placeOrder = false,
                    updateOrder = false,
                    payOrder = false,
                    paidOrder = false
                )
            }else if(cd.cookingDateAndDishes.cookingDate.cookingStatusId == 20){
                if(cd.cookingDateAndDishes.cookingDate.maybeGo == 1){
                    showOrderBtns(
                        placeOrder = false,
                        updateOrder = false,
                        payOrder = false,
                        paidOrder = false,
                        confirmBtn = false,
                        confirmMsg = true
                    )
                }else {
                    showOrderBtns(
                        placeOrder = false,
                        updateOrder = false,
                        payOrder = false,
                        paidOrder = false,
                        confirmBtn = true,
                        confirmMsg = false
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
                        showAlert("Your order did not make it to this list, but you are on the waiting list for drop out orders. You'll receive a notification if your order gets onto this list. You may also DM KungfuBBQ to find out if there will be first come first served meals.",
                                "Order status")
                    }
                    if (arrayListOf<Int>(5,8,9,10,11,14).contains(order.orderStatusId) ){ /*5-Confirmed/paid by user 8-Waiting order pickup alert 9- Waiting pickup 10- Delivered 11-Closed  14-payAtPickup*/
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
                        showAlert("You cancelled this order if you wish to order food from us, please choose another available event. You may also DM KungfuBBQ to find out if there will be first come first served meals.",
                            "Order status")
                    }
                    if (order.orderStatusId == 7 ){ /*Not made to this cookingCalendar date list*/
                        /*create user alert*/
                        showAlert("We are sorry! Unfortunately your order did not make to the final list on this event. Please, order from us again on another available event. You may also DM KungfuBBQ to find out if there will be first come first served meals.",
                            "Order status")
                    }
                    if (order.orderStatusId == 12 ){ /*The cooking calendar register was excluded by the database administrator, application user or routine*/
                        /*create user alert*/
                        showAlert("You missed the time you had to confirm the order. Please order again from another available event. You may also DM KungfuBBQ to find out if there will be first come first served meals.",
                            "Order status")
                    }
                }else{
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
    private fun showOrderBtns(placeOrder:Boolean, updateOrder:Boolean, payOrder:Boolean, paidOrder:Boolean, confirmBtn:Boolean = false, confirmMsg: Boolean = false){
        binding.calendarPreOrder.visibility = if(placeOrder) View.VISIBLE else View.INVISIBLE
        binding.calendarUpdateOrder.visibility = if(updateOrder) View.VISIBLE else View.INVISIBLE
        binding.calendarPayOrder.visibility = if(payOrder) View.VISIBLE else View.INVISIBLE
        binding.calendarPaidOrder.visibility = if(paidOrder) View.VISIBLE else View.INVISIBLE
        binding.calendarConfirm.visibility = if(confirmBtn) View.VISIBLE else View.INVISIBLE
        binding.calendarConfirmMsg.visibility = if(confirmMsg) View.VISIBLE else View.INVISIBLE
    }
    private fun returnUserFromDBNull() {
        showAlert("It was not possible to recover data from app`s database. Please restart the app.","Database error!")
    }
    private fun refreshCalendarView(newDate:LocalDate?,oldDate:LocalDate?){
        newDate?.let { binding.calendarView.notifyDateChanged(it) }
        oldDate?.let { binding.calendarView.notifyDateChanged(it) }
    }


    private fun showAlert(message:String,title:String){
        Handler(Looper.getMainLooper()).post{
            var dialogBuilder = AlertDialog.Builder(requireContext())
            dialogBuilder.setMessage(message)
                .setCancelable(title != "${getString(R.string.not_logged_in)}")
                .setPositiveButton("Ok", DialogInterface.OnClickListener{
                        _, _ ->
                    if(title=="${getString(R.string.not_logged_in)}"){
                        USER_LOGGED = false
                        val action = NavGraphDirections.callHome(false)
                        findNavController().navigate(action)
                    }
                    if(title=="Presence confirmed!"){
                        showOrderBtns(
                            placeOrder = false,
                            updateOrder = false,
                            payOrder = false,
                            paidOrder = false,
                            confirmBtn = false,
                            confirmMsg = true
                        )
                    }
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