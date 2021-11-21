package me.dgmieth.kungfubbq

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.fragment_payorder.*
import me.dgmieth.kungfubbq.datatabase.room.Actions
import me.dgmieth.kungfubbq.datatabase.room.KungfuBBQRoomDatabase
import me.dgmieth.kungfubbq.datatabase.room.RoomViewModel
import me.dgmieth.kungfubbq.datatabase.roomEntities.*
import me.dgmieth.kungfubbq.httpCtrl.HttpCtrl
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

    private var bag = CompositeDisposable()

    private val args : PayOrderFragmentArgs by navArgs()

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
                        Handler(Looper.getMainLooper()).post{
                            loginSpinerLayout.visibility = View.INVISIBLE
                            Toast.makeText(requireActivity(),"It was not possible to authenticate this user in KungfuBBQ's server. Please try again in some minutes",Toast.LENGTH_LONG).show()
                        }
                    }
                    else -> { }
                }
            },
            {
                Handler(Looper.getMainLooper()).post{
                    loginSpinerLayout.visibility = View.INVISIBLE
                    Toast.makeText(requireActivity(),"Log in attempt failed. Please try again in some minutes",Toast.LENGTH_LONG).show()
                }
            },{})?.let{ bag.add(it)}
        //Subscribing to user
        viewModel?.user?.observe(viewLifecycleOwner, Observer {
            Log.d(TAG, "user returned $it")
            viewModel?.getCookingDates()
            if(!it.user.email.isNullOrEmpty()){
                userPayOrder = it
            }else{
                Handler(Looper.getMainLooper()).post{
                    Toast.makeText(requireActivity(),"It was not possible to retrieve information from this app's database. Please restart the app.",
                        Toast.LENGTH_LONG).show()
                    var action = CalendarFragmentDirections.callCalendarFragmentGlobal()
                    findNavController().navigate(action)
                }
            }
        })
        //Subscribing to cookingD
        viewModel?.cookingDates?.subscribe({
            var cdArray = it.filter { c -> c.cookingDateAndDishes.cookingDate.cookingDateId == args.cookingDateId }
            cookingDate = cdArray[0]
            renewToken()
            cookingDate?.let { cd ->
                //updating date
                val splitDate = (cd.cookingDateAndDishes.cookingDate.cookingDate.split(" ")[0]).split("-")
                val cal = Calendar.getInstance()
                cal.set(splitDate[0].toInt(), splitDate[1].toInt()-1, splitDate[2].toInt())
                val dateStrParts = cal.time.toString().split(" ")
                payOrderDate.text = "${dateStrParts[1]} ${dateStrParts[2]}"
                //updating status
                payOrderStatus.text = cd.cookingDateAndDishes.cookingDate.cookingStatus
                //updating menu
                var menuT = ""
                var menuIndex = 1
                var mealsSum = 0.0
                for(m in cd.cookingDateAndDishes.cookingDateDishes){
                    menuT = "${menuT}${menuIndex}- ${m.dishName} - U$${m.dishPrice}\n"
                    menuIndex += 1
                    mealsSum += m.dishPrice.toDouble()
                }
                payOrderMenu.text = menuT
                //updating maps
                payOrderLocationText.text = "${cd.cookingDateAndDishes.cookingDate.street}, ${cd.cookingDateAndDishes.cookingDate.city}"
                payOrderLocationMap.getMapAsync(this)
                var priceString = "U$ ${String.format("%.2f",mealsSum)}"
                //updating meal price
                payOrderMealPrice.text = priceString
                //updating total meal price
                payOrderNumberOfMeals.text = cd.order[0].dishes[0].dishQuantity.toString()
                payOrderTotalPrice.text = "U$ ${String.format("%.2f",mealsSum*cd.order[0].dishes[0].dishQuantity)}"
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
            var dialogBuilder = AlertDialog.Builder(activity)
            dialogBuilder.setMessage("Communication with this apps's database failed. Please restart the app.")
                .setCancelable(false)
                .setPositiveButton("Ok", DialogInterface.OnClickListener{
                        _, _ ->
                    Handler(Looper.getMainLooper()).post {
                        var action = CalendarFragmentDirections.callCalendarFragmentGlobal()
                        findNavController().navigate(action)
                    }
                })
            val alert = dialogBuilder.create()
            alert.setTitle("Database communication failure")
            alert.show()
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initGoogleMap(savedInstanceState)
        showSpinner(true)
        payOrderMenu.movementMethod = ScrollingMovementMethod()
        //click listeners
        payOrderCancelBtn.setOnClickListener {
            deleteOrderAlert()
        }
        payOrderPayBtn.setOnClickListener {
            val action = PayOrderFragmentDirections.callPay(
                userPayOrder!!.user.email,
                userPayOrder!!.user.userId,
                args.cookingDateId,
                cookingDate!!.order[0].order.orderId,
                userPayOrder!!.user.token!!
            )
            findNavController().navigate(action)
        }
        super.onViewCreated(view, savedInstanceState)
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
    override fun onStart() {
        super.onStart()
        payOrderLocationMap.onStart()
    }
    override fun onResume() {
        super.onResume()
        payOrderLocationMap.onResume()
    }
    override fun onPause() {
        super.onPause()
        payOrderLocationMap.onPause()
    }
    override fun onStop() {
        super.onStop()
        payOrderLocationMap.onStop()
    }
    override fun onDestroy() {
        super.onDestroy()
        bag.clear()
        bag.dispose()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        payOrderLocationMap.onSaveInstanceState(outState)
    }
    override fun onLowMemory() {
        super.onLowMemory()
        payOrderLocationMap.onLowMemory()
    }
    //============================================================
    // maps
    private fun initGoogleMap(savedInstanceState: Bundle?) {
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPS_API_KEY)
        }
        payOrderLocationMap.onCreate(mapViewBundle)
        payOrderLocationMap.getMapAsync(this)
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
        var dialogBuilder = AlertDialog.Builder(activity)
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
                Handler(Looper.getMainLooper()).post{
                    Toast.makeText(requireActivity(),"The attempt to delete your order from KungfuBBQ's server failed with generalized message: ${e.localizedMessage}",
                        Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                showSpinner(false)
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val json = JSONObject(response.body!!.string())
                    if(!json.getBoolean("hasErrors")){
                        Handler(Looper.getMainLooper()).post{
                            var dialogBuilder = AlertDialog.Builder(activity)
                            dialogBuilder.setMessage("${json.getString("msg")}")
                                .setCancelable(false)
                                .setPositiveButton("Ok", DialogInterface.OnClickListener{
                                        _, _ ->
                                    Handler(Looper.getMainLooper()).post {
                                        var action = CalendarFragmentDirections.callCalendarFragmentGlobal()
                                        findNavController().navigate(action)
                                    }
                                })
                            val alert = dialogBuilder.create()
                            alert.setTitle("Pre-order successfully deleted from KungfuBBQ's server")
                            alert.show()
                        }
                    }else{
                        when {
                            json.getInt("errorCode")==-1 -> {
                                notLoggedIntAlert()
                            }
                            json.getInt("errorCode") <= -2 -> {
                                showWarningMessage(json.getString("msg"))
                            }
                            else -> {
                                Handler(Looper.getMainLooper()).post{
                                    Toast.makeText(requireActivity(),"The attempt to delete your order from KungfuBBQ's server failed with server message: ${json.getString("msg")}",
                                        Toast.LENGTH_LONG).show()
                                }
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
                    Handler(Looper.getMainLooper()).post{
                        Toast.makeText(requireActivity(),"The attempt to connect to KungfuBBQ's server failed with generalized error message: ${e.localizedMessage}",
                            Toast.LENGTH_LONG).show()
                    }
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        val json = JSONObject(response.body!!.string())
                        if(!json.getBoolean("hasErrors")){
                            viewModel?.updateUserToken(u.user.userId, json.getString("msg"))
                        }else{
                            if(json.getInt("errorCode")==-1){
                                notLoggedIntAlert()
                            }else{
                                Handler(Looper.getMainLooper()).post{
                                    Toast.makeText(requireActivity(),"The attempt to connect to KungfuBBQ server failed with server message: ${json.getString("msg")}",
                                        Toast.LENGTH_LONG).show()
                                }
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
            payOrderSpinnerLayout.visibility =  when(value){
                true -> View.VISIBLE
                else -> View.INVISIBLE
            }
        }
    }
    private fun showWarningMessage(text:String){
        //showWarningMessage(json.getString("msg"))
        Handler(Looper.getMainLooper()).post{
            Toast.makeText(requireActivity(),"$text", Toast.LENGTH_LONG).show()
            val action = NavGraphDirections.callCalendarFragmentGlobal()
            findNavController().navigate(action)
        }
    }
    private fun notLoggedIntAlert(){
        Handler(Looper.getMainLooper()).post{
            Toast.makeText(requireActivity(),"You are not authenticated in Kungfu BBQ server anylonge. Please log in again.",
                Toast.LENGTH_LONG).show()
            val action = NavGraphDirections.callHome(false)
            findNavController().navigate(action)
        }
    }
}