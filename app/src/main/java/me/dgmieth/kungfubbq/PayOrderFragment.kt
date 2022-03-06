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
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import io.reactivex.disposables.CompositeDisposable
import me.dgmieth.kungfubbq.databinding.FragmentPayorderBinding
import me.dgmieth.kungfubbq.datatabase.room.Actions
import me.dgmieth.kungfubbq.datatabase.room.KungfuBBQRoomDatabase
import me.dgmieth.kungfubbq.datatabase.room.RoomViewModel
import me.dgmieth.kungfubbq.datatabase.roomEntities.*
import me.dgmieth.kungfubbq.httpCtrl.HttpCtrl
import me.dgmieth.kungfubbq.support.formatter.FormatObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.*

class PayOrderFragment : Fragment(R.layout.fragment_payorder), OnMapReadyCallback {
    private val TAG = "PayOrderFragment"

    private val MAPS_API_KEY = "google_maps_api_key"
    private var viewModel: RoomViewModel? = null
    private var cookingDate : CookingDateAndCookingDateDishesWithOrder? = null
    private var userPayOrder : UserAndSocialMedia? = null
    private var btnClick = true
    private var mealsQtty = 0
    private var orderNr = 0

    private var bag = CompositeDisposable()

    private val args : PayOrderFragmentArgs by navArgs()

    private var _binding: FragmentPayorderBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG,"onCreateView called")
        viewModel = ViewModelProviders.of(this).get(RoomViewModel::class.java)
        viewModel?.getDbInstance(KungfuBBQRoomDatabase.getInstance(requireActivity()))
        //subscribing to returnMsg events
        viewModel?.returnMsg?.subscribe(
            {
                showSpinner(false)
                when(it){
                    Actions.UserError ->{
                        showAlert("It was not possible to authenticate this user in KungfuBBQ's server. Please try again in some minutes","${getString(R.string.database_error)}")
                    }
                    else -> { }
                }
            },
            {
                showAlert("It was not possible to authenticate this user in KungfuBBQ's server. Please try again in some minutes","${getString(R.string.database_error)}")
            },{})?.let{ bag.add(it)}
        //Subscribing to user
        viewModel?.user?.observe(viewLifecycleOwner, Observer {
            Log.d(TAG, "user returned $it")
            viewModel?.getCookingDates()
            if(!it.user.email.isNullOrEmpty()){
                userPayOrder = it
            }else{
                showAlert("It was not possible to authenticate this user in KungfuBBQ's server. Please try again in some minutes","${getString(R.string.database_error)}")
            }
        })
        //Subscribing to cookingD
        viewModel?.cookingDates?.subscribe({
            var cdArray = it.filter { c -> c.cookingDateAndDishes.cookingDate.cookingDateId == args.cookingDateId }
            cookingDate = cdArray[0]
            renewToken()
            cookingDate?.let { cd ->
                //updating date
//                val splitDate = (cd.cookingDateAndDishes.cookingDate.cookingDate.split(" ")[0]).split("-")
//                val cal = Calendar.getInstance()
//                cal.set(splitDate[0].toInt(), splitDate[1].toInt()-1, splitDate[2].toInt())
//                val dateStrParts = cal.time.toString().split(" ")
                binding.payOrderDate.text = FormatObject.returnEventTime()
                //updating status
                binding.payOrderStatus.text = cd.cookingDateAndDishes.cookingDate.cookingStatus
                //updating menu
//                var menuT = ""
//                var menuIndex = 1
//                var mealsSum = 0.0
//                for(m in cd.cookingDateAndDishes.cookingDateDishes){
//                    menuT = "${menuT}${menuIndex}- ${m.dishName} - U$${m.dishPrice}\n"
//                    menuIndex += 1
//                    mealsSum += m.dishPrice.toDouble()
//                }
//                binding.payOrderMenu.text = menuT
                binding.payOrderMenu.text = Html.fromHtml(FormatObject.formatDishesListForMenuScrollViews(cd.cookingDateAndDishes.cookingDateDishes),
                    Html.FROM_HTML_MODE_COMPACT)
                //updating maps
                binding.payOrderLocationText.text = FormatObject.returnAddress()
                binding.payOrderLocationMap.getMapAsync(this)
                var priceString = "U$ ${String.format("%.2f",FormatObject.returnMealBoxTotalAmount())}"
                //updating meal price
                binding.payOrderMealPrice.text = priceString
                //updating total meal price
                binding.payOrderNumberOfMeals.text = cd.order[0].dishes[0].dishQuantity.toString()
                orderNr = cd.order[0].order.orderId
                binding.payOrderTotalPrice.text = "U$ ${String.format("%.2f",FormatObject.returnTotalAmountDue(cd.order[0].dishes[0].dishQuantity))}"
                mealsQtty = cd.order[0].dishes[0].dishQuantity
            }
        },{
            Log.d("CookingDateObservable","error is $it")
        },{})?.let {
            bag.add(it)
        }
        //db getUser information request
        if(args.cookingDateId != 0) {
            viewModel?.getUser()
        }else{
            showAlert("It was not possible to authenticate this user in KungfuBBQ's server. Please try again in some minutes","${getString(R.string.database_error)}")
//            var dialogBuilder = AlertDialog.Builder(requireContext())
//            dialogBuilder.setMessage("Communication with this apps's database failed. Please restart the app.")
//                .setCancelable(false)
//                .setPositiveButton("Ok", DialogInterface.OnClickListener{
//                        _, _ ->
//                    Handler(Looper.getMainLooper()).post {
//                        var action = CalendarFragmentDirections.callCalendarFragmentGlobal()
//                        findNavController().navigate(action)
//                    }
//                })
//            val alert = dialogBuilder.create()
//            alert.setTitle("Database communication failure")
//            alert.show()
        }
        _binding = FragmentPayorderBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initGoogleMap(savedInstanceState)
        showSpinner(true)
        binding.payOrderMenu.movementMethod = ScrollingMovementMethod()
        //click listeners
        binding.payOrderCancelBtn.setOnClickListener {
            deleteOrderAlert()
        }
        binding.payOrderPayBtn.setOnClickListener {
            showAlert(getString(R.string.payment_options_text), getString(R.string.payment_options))
        }
        super.onViewCreated(view, savedInstanceState)
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
    override fun onStart() {
        super.onStart()
        binding.payOrderLocationMap.onStart()
    }
    override fun onResume() {
        super.onResume()
        binding.payOrderLocationMap.onResume()
        if(!btnClick){
            val action = HomeFragmentDirections.callHome(true)
            findNavController().navigate(action)
        }
        btnClick = false
    }
    override fun onPause() {
        super.onPause()
        binding.payOrderLocationMap.onPause()
    }
    override fun onStop() {
        super.onStop()
        binding.payOrderLocationMap.onStop()
    }
    override fun onDestroy() {
        super.onDestroy()
        bag.clear()
        bag.dispose()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.payOrderLocationMap.onSaveInstanceState(outState)
    }
    override fun onLowMemory() {
        super.onLowMemory()
        binding.payOrderLocationMap.onLowMemory()
    }
    //============================================================
    // maps
    private fun initGoogleMap(savedInstanceState: Bundle?) {
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPS_API_KEY)
        }
        binding.payOrderLocationMap.onCreate(mapViewBundle)
        binding.payOrderLocationMap.getMapAsync(this)
    }
    override fun onMapReady(map: GoogleMap) {
        cookingDate?.let {
            var position = LatLng(it.cookingDateAndDishes.cookingDate.lat, it.cookingDateAndDishes.cookingDate.lng)
            map.addMarker(
                com.google.android.gms.maps.model.MarkerOptions()
                    .position(position)
                    .title("KungfuBBQ")
            )
            map.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(position,16.0f))
        }
    }
    /* =========================================================================================
    *   http requests
    * */
    private fun deleteOrderAlert() {
        var dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setMessage("Are you sure you want to delete your order? This action cannot be undone.")
            .setCancelable(true)
            .setNegativeButton("Cancel", DialogInterface.OnClickListener{
                    _,_->
            })
            .setPositiveButton("Yes", DialogInterface.OnClickListener{
                    _, _ ->
                showSpinner(true)
                Handler(Looper.getMainLooper()).post {
                    deleteOrder()
                }
            })
        val alert = dialogBuilder.create()
        alert.setTitle("Delete your order?")
        alert.show()
    }
    private fun deleteOrder(){
        val body = okhttp3.FormBody.Builder()
            .add("email",userPayOrder!!.user.email)
            .add("id",userPayOrder!!.user.userId.toString())
            .add("cookingDate_id",args.cookingDateId.toString())
            .add("order_id", cookingDate!!.order[0].order.orderId.toString())
            .build()
        Log.d(TAG,"Body is ${body.toString()}")
        HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/api/order/cancelMadeToListOrder",body,userPayOrder!!.user.token)).enqueue(object :
            Callback {
            override fun onFailure(call: Call, e: IOException) {
                showSpinner(false)
                e.printStackTrace()
                showAlert("The attempt to delete your order from KungfuBBQ's server failed with generalized message: ${e.localizedMessage}","Error!")
            }
            override fun onResponse(call: Call, response: Response) {
                showSpinner(false)
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val json = JSONObject(response.body!!.string())
                    if(!json.getBoolean("hasErrors")){
                        showAlert("Pre-order successfully deleted from KungfuBBQ's server","${getString(R.string.success)}")
                    }else{
                        when {
                            json.getInt("errorCode")==-1 -> {
                                showAlert("${getString(R.string.not_logged_in_message)}","${getString(R.string.not_logged_in)}")
                            }
                            else -> {
                                showAlert("The attempt to delete your order from KungfuBBQ's server failed with server message: ${json.getString("msg")}","Error!")
                            }
                        }
                    }
                }
            }
        })
    }
    private fun renewToken(){
        userPayOrder?.let {
            val u = it
            var httpUrl = HttpUrl.Builder()
                .scheme("https")
                .host("${getString(R.string.kungfuServerUrlNoSchema)}")
                .addQueryParameter("email",it.user.email)
                .addPathSegments("api/user/renewToken")
                .build()
            HttpCtrl.shared.newCall(HttpCtrl.get("","",httpUrl,it.user.token)).enqueue(object :
                Callback {
                override fun onFailure(call: Call, e: IOException) {
                    showSpinner(false)
                    e.printStackTrace()
                    showAlert("The attempt to connect to KungfuBBQ's server failed with generalized error message: ${e.localizedMessage}","Error!")
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        val json = JSONObject(response.body!!.string())
                        if(!json.getBoolean("hasErrors")){
                            viewModel?.updateUserToken(u.user.userId, json.getString("msg"))
                        }else{
                            if(json.getInt("errorCode")==-1){
                                showAlert("${getString(R.string.not_logged_in_message)}","${getString(R.string.not_logged_in)}")
                            }else{
                                showAlert("The attempt to connect  to KungfuBBQ's server failed with server message: ${json.getString("msg")}","Error!")
                            }
                        }
                    }
                }
            })
        }
    }
    /*
    * UI ELEMENTS
    */
    private fun showSpinner(value: Boolean){
        Handler(Looper.getMainLooper()).post {
            binding.payOrderSpinnerLayout.visibility =  when(value){
                true -> View.VISIBLE
                else -> View.INVISIBLE
            }
        }
    }
    private fun payAtPickUp(){
        val body = okhttp3.FormBody.Builder()
            .add("email",userPayOrder!!.user.email)
            .add("id",userPayOrder!!.user.userId.toString())
            .add("cookingDate_id",args.cookingDateId.toString())
            .add("order_id", cookingDate!!.order[0].order.orderId.toString())
            .build()
        Log.d(TAG,"Body is ${body.toString()}")
        HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/api/order/payAtPickup",body,userPayOrder!!.user.token)).enqueue(object :
            Callback {
            override fun onFailure(call: Call, e: IOException) {
                showSpinner(false)
                e.printStackTrace()
                showAlert("The attempt to confirm order nr. $orderNr in KungfuBBQ's server failed with generalized message: ${e.localizedMessage}","Error!")
            }
            override fun onResponse(call: Call, response: Response) {
                showSpinner(false)
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val json = JSONObject(response.body!!.string())
                    if(!json.getBoolean("hasErrors")){
                        showAlert("Order nr. $orderNr was confirmed successfully. It must be paid at pick up on the day of the event","${getString(R.string.success)}")
                    }else{
                        when {
                            json.getInt("errorCode")==-1 -> {
                                showAlert("${getString(R.string.not_logged_in_message)}","${getString(R.string.not_logged_in)}")
                            }
                            else -> {
                                showAlert("The attempt to confirm order nr. $orderNr in KungfuBBQ's server failed with server message: ${json.getString("msg")}","Error!")
                            }
                        }
                    }
                }
            }
        })
    }
    private fun showAlert(message:String,title:String) {
        Handler(Looper.getMainLooper()).post {
            var dialogBuilder = AlertDialog.Builder(requireContext())
            if(title==getString(R.string.payment_options)){
                dialogBuilder.setMessage(message)
                    .setCancelable(true)
                    .setNeutralButton("Pay at pick up",DialogInterface.OnClickListener{_,_->
                        showSpinner(true)
                        payAtPickUp()
                    })
                    .setPositiveButton("Pay now", DialogInterface.OnClickListener{_,_->
                        btnClick = true
                        val action = PayOrderFragmentDirections.callPay(
                            userPayOrder!!.user.email,
                            userPayOrder!!.user.userId,
                            args.cookingDateId,
                            cookingDate!!.order[0].order.orderId,
                            userPayOrder!!.user.token!!,
                            qttyOfMeals = mealsQtty
                        )
                        findNavController().navigate(action)
                    })
            }else{
                dialogBuilder.setMessage(message)
                    .setCancelable(title != "${getString(R.string.not_logged_in)}")
                    .setPositiveButton("Ok", DialogInterface.OnClickListener { _, _ ->
                        if (title == "${getString(R.string.not_logged_in)}") {
                            USER_LOGGED = false
                            val action = NavGraphDirections.callHome(false)
                            findNavController().navigate(action)
                        }
                        if (title == "${getString(R.string.database_error)}") {
                            var action = CalendarFragmentDirections.callCalendarFragmentGlobal()
                            findNavController().navigate(action)
                        }
                        if (title == "${getString(R.string.success)}") {
                            var action = CalendarFragmentDirections.callCalendarFragmentGlobal()
                            findNavController().navigate(action)
                        }
                    })
            }
            val alert = dialogBuilder.create()
            alert.setTitle(title)
            alert.show()
            if(title==getString(R.string.payment_options)){
//                alert.getButton(0).setTextColor(Color.DKGRAY)
                alert.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.BLACK);
            }
        }
    }
}