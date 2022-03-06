package me.dgmieth.kungfubbq

import androidx.appcompat.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import io.reactivex.disposables.CompositeDisposable
import me.dgmieth.kungfubbq.databinding.FragmentUpdateorderBinding
import me.dgmieth.kungfubbq.datatabase.room.KungfuBBQRoomDatabase
import me.dgmieth.kungfubbq.datatabase.room.RoomViewModel
import me.dgmieth.kungfubbq.datatabase.roomEntities.CookingDateAndCookingDateDishesWithOrder
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserAndSocialMedia
import me.dgmieth.kungfubbq.httpCtrl.HttpCtrl
import me.dgmieth.kungfubbq.support.formatter.FormatObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.*

class UpdateOrderFragment : Fragment(R.layout.fragment_updateorder), OnMapReadyCallback {

    private val TAG = "UpdateOrderFragment"

    private val MAPS_API_KEY = "google_maps_api_key"
    private var viewModel: RoomViewModel? = null
    private var cookingDate : CookingDateAndCookingDateDishesWithOrder? = null
    private var userUpdateOrder : UserAndSocialMedia? = null
    private var selectedQtty = 1
    private var editItemBtn : MenuItem? = null
    private var btnClick = true

    private var bag = CompositeDisposable()

    private val args : UpdateOrderFragmentArgs by navArgs()

    private var _binding: FragmentUpdateorderBinding? = null
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
        //Subscribing to user
        viewModel?.user?.observe(viewLifecycleOwner, Observer {
            Log.d(TAG, "user returned $it")
            viewModel?.getCookingDates()
            if(!it.user.email.isNullOrEmpty()){
                userUpdateOrder = it
            }else{
                showAlert("It was not possible to retrieve information from this app's database. Please restart the app.","${getString(R.string.database_error)}")
            }
        })
        //Subscribing to cookingD
        viewModel?.cookingDates?.subscribe({
            var cdArray = it.filter { c -> c.cookingDateAndDishes.cookingDate.cookingDateId == args.cookingDateId }
            cookingDate = cdArray[0]
            cookingDate?.let { cd ->
                //updating date
//                val splitDate = (cd.cookingDateAndDishes.cookingDate.cookingDate.split(" ")[0]).split("-")
//                val cal = Calendar.getInstance()
//                cal.set(splitDate[0].toInt(), splitDate[1].toInt()-1, splitDate[2].toInt())
//                val dateStrParts = cal.time.toString().split(" ")
                binding.updateOrderDate.text = FormatObject.returnEventTime()
                //updating status
                binding.updateOrderStatus.text = cd.cookingDateAndDishes.cookingDate.cookingStatus
                //updating menu
//                var menuT = ""
//                var menuIndex = 1
//                var mealsSum = 0.0
//                for(m in cd.cookingDateAndDishes.cookingDateDishes){
//                    menuT = "${menuT}${menuIndex}- ${m.dishName} - U$${m.dishPrice}\n"
//                    menuIndex += 1
//                    mealsSum += m.dishPrice.toDouble()
//                }
//                binding.updateOrderMenu.text = menuT
                binding.updateOrderMenu.text = Html.fromHtml(FormatObject.formatDishesListForMenuScrollViews(cd.cookingDateAndDishes.cookingDateDishes), Html.FROM_HTML_MODE_COMPACT)
                //updating maps
                binding.updateOrderLocationText.text = FormatObject.returnAddress()
                binding.updateOrderLocationMap.getMapAsync(this)
                var priceString = "U$ ${String.format("%.2f",FormatObject.returnMealBoxTotalAmount())}"
                //updating meal price
                binding.updateOrderMealPrice.text = priceString
                //updating total meal price
                binding.updateOrderNumberOfMeals.value = cd.order[0].dishes[0].dishQuantity
                selectedQtty = cd.order[0].dishes[0].dishQuantity
                binding.updateOrderTotalPrice.text = "U$ ${String.format("%.2f",FormatObject.returnMealBoxTotalAmount()*cd.order[0].dishes[0].dishQuantity)}"
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
            showAlert("It was not possible to retrieve information from this app's database. Please restart the app.","${getString(R.string.database_error)}")
        }
        _binding = FragmentUpdateorderBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initGoogleMap(savedInstanceState)
        binding.updateOrderMenu.movementMethod = ScrollingMovementMethod()
        binding.updateOrderNumberOfMeals.minValue = 1
        binding.updateOrderNumberOfMeals.maxValue = 100
        binding.updateOrderNumberOfMeals.wrapSelectorWheel = true
        binding.updateOrderNumberOfMeals.isEnabled = false
        //click listeners
        binding.updateOrderNumberOfMeals.setOnValueChangedListener{_,_,newVal ->
            selectedQtty = newVal
            binding.updateOrderTotalPrice.text = returnTotalAmount(newVal)
        }
        binding.updateOrderCancelBtn.setOnClickListener {
            showUpdateOrderBtns(false)
            binding.updateOrderNumberOfMeals.value = cookingDate!!.order[0].dishes[0].dishQuantity
            selectedQtty = cookingDate!!.order[0].dishes[0].dishQuantity
            binding.updateOrderTotalPrice.text = returnTotalAmount(selectedQtty)
        }
        binding.updateOrderUpdateOrderBtn.setOnClickListener {
            updateOrder()
        }
        binding.updateOrderDeleteOrder.setOnClickListener {
            deleteOrderAlert()
        }
        super.onViewCreated(view, savedInstanceState)
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.getItem(0).isVisible = false
        menu.getItem(1).isVisible = false
        menu.getItem(2).isVisible = true
        menu.getItem(3).isVisible = false
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.editMenuBtn -> {
                editItemBtn = item
                showUpdateOrderBtns(true)
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }
    override fun onStart() {
        super.onStart()
        binding.updateOrderLocationMap.onStart()
    }
    override fun onResume() {
        super.onResume()
        binding.updateOrderLocationMap.onResume()
        if(!btnClick){
            val action = HomeFragmentDirections.callHome(true)
            findNavController().navigate(action)
        }
        btnClick = false
    }
    override fun onPause() {
        super.onPause()
        binding.updateOrderLocationMap.onPause()
    }
    override fun onStop() {
        super.onStop()
        binding.updateOrderLocationMap.onStop()
    }
    override fun onDestroy() {
        super.onDestroy()
        bag.clear()
        bag.dispose()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.updateOrderLocationMap.onSaveInstanceState(outState)
    }
    override fun onLowMemory() {
        super.onLowMemory()
        binding.updateOrderLocationMap.onLowMemory()
    }
    //===================================================
    // maps
    private fun initGoogleMap(savedInstanceState: Bundle?) {
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPS_API_KEY)
        }
        binding.updateOrderLocationMap.onCreate(mapViewBundle)
        binding.updateOrderLocationMap.getMapAsync(this)
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
    *   updateorder http requests
    * */
    private fun deleteOrderAlert() {
        var dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setMessage("Are you sure you want to cancel this order? This action will take you out of this cooking date's distribuition list and cannot be undone. As soon as you cancel, the system will request another user on the waiting list to take your place on the distribution list.")
            .setCancelable(true)
            .setNegativeButton("Cancel",DialogInterface.OnClickListener{
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
        showSpinner(true)
        val body = okhttp3.FormBody.Builder()
            .add("email",userUpdateOrder!!.user.email)
            .add("id",userUpdateOrder!!.user.userId.toString())
            .add("order_id", cookingDate!!.order[0].order.orderId.toString())
            .build()
        Log.d(TAG,"Body is ${body.toString()}")
        HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/api/order/deleteOrder",body,userUpdateOrder!!.user.token)).enqueue(object :
            Callback {
            override fun onFailure(call: Call, e: IOException) {
                showSpinner(false)
                e.printStackTrace()
                showAlert("The attempt to delete your order from KungfuBBQ server failed with generalized message: ${e.localizedMessage}","Error!")
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
    private fun updateOrder(){
        if(cookingDate!!.order[0].dishes[0].dishQuantity == selectedQtty){
            showAlert("Nothing was changed.","Update cancelled!")
            return
        }
        val body = okhttp3.FormBody.Builder()
            .add("email",userUpdateOrder!!.user.email)
            .add("id",userUpdateOrder!!.user.userId.toString())
            .add("order_id", cookingDate!!.order[0].order.orderId.toString())
            .add("new_qtty",selectedQtty.toString())
            .build()
        Log.d(TAG,"Body is ${body.toString()}")
        HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/api/order/updateOrder",body,userUpdateOrder!!.user.token)).enqueue(object :
            Callback {
            override fun onFailure(call: Call, e: IOException) {
                showSpinner(false)
                e.printStackTrace()
                showAlert("The attempt to update your order on KungfuBBQ's server failed with generalized message: ${e.localizedMessage}","Error!")
            }
            override fun onResponse(call: Call, response: Response) {
                showSpinner(false)
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val json = JSONObject(response.body!!.string())
                    if(!json.getBoolean("hasErrors")){
                        showAlert("Pre-order successfully updated on KungfuBBQ's server","${getString(R.string.success)}")
                    }else{
                        when {
                            json.getInt("errorCode")==-1 -> {
                                showAlert("${getString(R.string.not_logged_in)}","${getString(R.string.not_logged_in)}")
                            }
                            else -> {
                                showAlert("The attempt to update your order on KungfuBBQ's server failed with server message: ${json.getString("msg")}","Error!")
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
    private fun showUpdateOrderBtns(value:Boolean){
        binding.updateOrderNumberOfMeals.isEnabled = value
        binding.updateOrderUpdateOrderBtns.isVisible = value
        binding.updateOrderDeleteOrder.isVisible = !value
        editItemBtn!!.isVisible = !value
    }
    private fun returnTotalAmount(qttyChosen:Int):String{
//        val mealsPrice = (binding.updateOrderMealPrice.text.toString().split(" ")[1].toDouble())
        val total = FormatObject.returnMealBoxTotalAmount() * qttyChosen
        return "U$ ${String.format("%.2f",total)}"
    }
    private fun showSpinner(value: Boolean){
        Handler(Looper.getMainLooper()).post {
            binding.updateOrderSpinerLayout.visibility =  when(value){
                true -> View.VISIBLE
                else -> View.INVISIBLE
            }
        }
    }
    private fun showAlert(message:String,title:String) {
        Handler(Looper.getMainLooper()).post {
            var dialogBuilder = AlertDialog.Builder(requireContext())
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
            val alert = dialogBuilder.create()
            alert.setTitle(title)
            alert.show()
        }
    }
}