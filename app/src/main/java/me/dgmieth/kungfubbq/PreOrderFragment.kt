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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_preorder.*
import me.dgmieth.kungfubbq.datatabase.room.KungfuBBQRoomDatabase
import me.dgmieth.kungfubbq.datatabase.room.RoomViewModel
import me.dgmieth.kungfubbq.datatabase.roomEntities.CookingDateAndCookingDateDishesWithOrder
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserAndSocialMedia
import me.dgmieth.kungfubbq.httpCtrl.HttpCtrl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.*

class PreOrderFragment : Fragment(R.layout.fragment_preorder),OnMapReadyCallback {
    private val TAG = "PreOrderFragment"

    private val MAPS_API_KEY = "google_maps_api_key"
    private var viewModel: RoomViewModel? = null
    private var cookingDate : CookingDateAndCookingDateDishesWithOrder? = null
    private var userPreOrder : UserAndSocialMedia? = null
    private var selectedQtty = 1

    private var bag = CompositeDisposable()

    private val args : PreOrderFragmentArgs by navArgs()

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
            when(it){
                else -> {
                    Handler(Looper.getMainLooper()).post{
                        Toast.makeText(requireActivity(),"It was not possible to retrieve information from kungfuBBQ server. Please try again later.",
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
        },{},{})?.let {
            bag.add(it)
        }
        //Subscribing to cookingD
        viewModel?.cookingDates?.subscribe({
            var cdArray = it.filter { c -> c.cookingDateAndDishes.cookingDate.cookingDateId == args.cookingDateId }
            cookingDate = cdArray[0]
            cookingDate?.let { cd ->
                //updating date
                val splitDate = (cd.cookingDateAndDishes.cookingDate.cookingDate.split(" ")[0]).split("-")
                val cal = Calendar.getInstance()
                cal.set(splitDate[0].toInt(), splitDate[1].toInt()-1, splitDate[2].toInt())
                val dateStrParts = cal.time.toString().split(" ")
                preOrderDate.text = "${dateStrParts[1]} ${dateStrParts[2]}"
                //updating status
                preOrderStatus.text = cd.cookingDateAndDishes.cookingDate.cookingStatus
                //updating menu
                var menuT = ""
                var menuIndex = 1
                var mealsSum = 0.0
                for(m in cd.cookingDateAndDishes.cookingDateDishes){
                    menuT = "${menuT}${menuIndex}- ${m.dishName} - U$${m.dishPrice}\n"
                    menuIndex += 1
                    mealsSum += m.dishPrice.toDouble()
                }
                preOrderMenu.text = menuT
                //updating maps
                preOrderLocationText.text = "${cd.cookingDateAndDishes.cookingDate.street}, ${cd.cookingDateAndDishes.cookingDate.city}"
                preOrderLocationMap.getMapAsync(this)
                var priceString = "U$ ${String.format("%.2f",mealsSum)}"
                //updating meal price
                preOrderMealPrice.text = priceString
                //updating total meal price
                preOrderTotalPrice.text = priceString
            }
        },{
            Log.d("CookingDateObservable","error is $it")

        },{})?.let {
            bag.add(it)
        }
        viewModel?.user?.observe(viewLifecycleOwner, Observer {
            if(!it.user.email.isNullOrEmpty()){
                userPreOrder = it
                viewModel?.getCookingDates()
            }else{
                Handler(Looper.getMainLooper()).post{
                    Toast.makeText(requireActivity(),"It was not possible to retrieve information from this app's database. Please restart the app.",
                        Toast.LENGTH_LONG).show()
                    var action = CalendarFragmentDirections.callCalendarFragmentGlobal()
                    findNavController().navigate(action)
                }
            }
        })
        //db getUser information request
        if(args.cookingDateId != 0) {
            viewModel?.getUser()
        }else{
            var dialogBuilder = AlertDialog.Builder(activity)
            dialogBuilder.setMessage("Communication with this apps's database failed. Please restart the app.")
                .setCancelable(false)
                .setPositiveButton("Ok", DialogInterface.OnClickListener{
                        dialog, id ->
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
        super.onViewCreated(view, savedInstanceState)
        initGoogleMap(savedInstanceState)
        preOrderMenu.movementMethod = ScrollingMovementMethod()
        preOrderNumberOfMeals.minValue = 1
        preOrderNumberOfMeals.maxValue = 100
        preOrderNumberOfMeals.wrapSelectorWheel = true
        preOrderNumberOfMeals.setOnValueChangedListener{_,_,newVal ->
            selectedQtty = newVal
            val mealsPrice = (preOrderMealPrice.text.toString().split(" ")[1].toDouble())
            val total = mealsPrice * newVal
            preOrderTotalPrice.text = "U$ ${String.format("%.2f",total)}"
        }
        //click listeners
        preOrderCancelBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
        preOrderPreOrderBtn.setOnClickListener {
            showSpinner(true)
            placePreOrder()
        }
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
    override fun onStart() {
        super.onStart()
        preOrderLocationMap.onStart()
    }
    override fun onResume() {
        super.onResume()
        preOrderLocationMap.onResume()
    }
    override fun onPause() {
        super.onPause()
        preOrderLocationMap.onPause()
    }
    override fun onStop() {
        super.onStop()
        preOrderLocationMap.onStop()
    }
    override fun onDestroy() {
        super.onDestroy()
        bag.clear()
        bag.dispose()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        preOrderLocationMap.onSaveInstanceState(outState)
    }
    override fun onLowMemory() {
        super.onLowMemory()
        preOrderLocationMap.onLowMemory()
    }
    //===========================================================
    //maps
    private fun initGoogleMap(savedInstanceState: Bundle?) {
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPS_API_KEY)
        }
        preOrderLocationMap.onCreate(mapViewBundle)
    }
    override fun onMapReady(map: GoogleMap) {
        cookingDate?.let {
            var position = LatLng(it.cookingDateAndDishes.cookingDate.lat, it.cookingDateAndDishes.cookingDate.lng)
            val dayton = LatLng(39.758949, -84.191605)
            map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title("KungfuBBQ")
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(position,16.0f))
        }
    }
    /* =========================================================================================
 *   preorder http request
 * */
    private fun placePreOrder(){
        var dishesAry : MutableList<Int> = kotlin.collections.mutableListOf()
        var dishesQtty : MutableList<Int> = kotlin.collections.mutableListOf()
        for(d in cookingDate!!.cookingDateAndDishes.cookingDateDishes){
            dishesAry.add(d.dishId)
            dishesQtty.add(selectedQtty)
        }
        val body = okhttp3.FormBody.Builder()
            .add("email",userPreOrder!!.user.email)
            .add("id",userPreOrder!!.user.userId.toString())
            .add("cookingDate_id", cookingDate!!.cookingDateAndDishes.cookingDate.cookingDateId.toString())
            .add("dish_id", dishesAry.toString())
            .add("dish_qtty", dishesQtty.toString())
            .add("extras_id", kotlin.collections.mutableListOf<kotlin.Int>().toString())
            .add("extras_qtty", kotlin.collections.mutableListOf<kotlin.Int>().toString())
            .build()
        Log.d(TAG,"Body is ${body.toString()}")
        HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/api/order/newOrder",body,userPreOrder!!.user.token)).enqueue(object :
            Callback {
            override fun onFailure(call: Call, e: IOException) {
                showSpinner(false)
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post{
                    Toast.makeText(requireActivity(),"The attempt to save your order to KungfuBBQ server failed with generalized message: ${e.localizedMessage}",Toast.LENGTH_LONG).show()
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
                            alert.setTitle("Pre-order created on KungfuBBQ's server")
                            alert.show()
                        }
                    }else{
                        when {
                            json.getInt("errorCode")==-1 -> {
                                notLoggedIntAlert()
                            }
                            json.getInt("errorCode")==-2 -> {
                                showWarningMessage(json.getString("msg"))
                            }
                            json.getInt("errorCode")==-3 -> {
                                showWarningMessage(json.getString("msg"))
                            }
                            else -> {
                                Handler(Looper.getMainLooper()).post{
                                    Toast.makeText(requireActivity(),"The attempt to save your order to KungfuBBQ server failed with server message: ${json.getString("msg")}",
                                        Toast.LENGTH_LONG).show()
                                    var action = CalendarFragmentDirections.callCalendarFragmentGlobal()
                                    findNavController().navigate(action)
                                }
                            }
                        }
                    }
                }
            }
        })
    }
    /*
    * UI ELEMENTS
    */
    private fun showSpinner(value: Boolean){
        Handler(Looper.getMainLooper()).post {
            preOrderSpinerLayout.visibility =  when(value){
                true -> View.VISIBLE
                else -> View.INVISIBLE
            }
        }
    }
    private fun showWarningMessage(text:String){
        //showWarningMessage(json.getString("msg"))
        Handler(Looper.getMainLooper()).post{
            Toast.makeText(requireActivity(),"$text",
                Toast.LENGTH_LONG).show()
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