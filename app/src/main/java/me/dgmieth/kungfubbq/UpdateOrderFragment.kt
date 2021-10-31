package me.dgmieth.kungfubbq

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import kotlinx.android.synthetic.main.fragment_preorder.*
import kotlinx.android.synthetic.main.fragment_updateorder.*
import me.dgmieth.kungfubbq.datatabase.room.KungfuBBQRoomDatabase
import me.dgmieth.kungfubbq.datatabase.room.RoomViewModel
import me.dgmieth.kungfubbq.datatabase.roomEntities.CookingDateAndCookingDateDishesWithOrder
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserAndSocialMedia
import java.util.*

class UpdateOrderFragment : Fragment(R.layout.fragment_updateorder), OnMapReadyCallback {

    private val TAG = "UpdateOrderFragment"

    private val MAPS_API_KEY = "google_maps_api_key"
    private var viewModel: RoomViewModel? = null
    private var cookingDate : CookingDateAndCookingDateDishesWithOrder? = null
    private var userUpdateOrder : UserAndSocialMedia? = null
    private var selectedQtty = 1

    private var bag = CompositeDisposable()

    private val args : UpdateOrderFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG,"onCreateView called")
        viewModel = ViewModelProviders.of(this).get(RoomViewModel::class.java)
        var db = KungfuBBQRoomDatabase.getInstance(requireActivity())
        viewModel?.getDbInstance(db)
        //Subscribing to cookingD
        viewModel?.cookingDates?.subscribe({
            Log.d(TAG,"cookingDates called")
            var cdArray = it.filter { c -> c.cookingDateAndDishes.cookingDate.cookingDateId == args.cookingDateId }
            cookingDate = cdArray[0]
            cookingDate?.let { cd ->
                Log.d(TAG,"cookingDates called -> value $cd")
                //updating date
                val splitDate = (cd.cookingDateAndDishes.cookingDate.cookingDate.split(" ")[0]).split("-")
                val cal = Calendar.getInstance()
                cal.set(splitDate[0].toInt(), splitDate[1].toInt()-1, splitDate[2].toInt())
                val dateStrParts = cal.time.toString().split(" ")
                updateOrderDate.text = "${dateStrParts[1]} ${dateStrParts[2]}"
                //updating status
                updateOrderStatus.text = cd.cookingDateAndDishes.cookingDate.cookingStatus
                //updating menu
                var menuT = ""
                var menuIndex = 1
                var mealsSum = 0.0
                for(m in cd.cookingDateAndDishes.cookingDateDishes){
                    menuT = "${menuT}${menuIndex}- ${m.dishName} - U$${m.dishPrice}\n"
                    menuIndex += 1
                    mealsSum += m.dishPrice.toDouble()
                }
                updateOrderMenu.setText(menuT)
                //updating maps
                updateOrderLocationText.text = "${cd.cookingDateAndDishes.cookingDate.street}, ${cd.cookingDateAndDishes.cookingDate.city}"
                //updateOrderLocationMap.getMapAsync(this)
                var priceString = "U$ ${String.format("%.2f",mealsSum)}"
                //updating meal price
                updateOrderMealPrice.text = priceString
                //updating total meal price
                updateOrderNumberOfMeals.value = cd.order[0].dishes[0].dishQuantity
                updateOrderTotalPrice.text = "U$ ${String.format("%.2f",mealsSum*cd.order[0].dishes[0].dishQuantity)}"
            }
        },{
            Log.d("CookingDateObservable","error is $it")
        },{})?.let {
            bag.add(it)
        }
        viewModel?.user?.observe(viewLifecycleOwner, Observer {
            Log.d(TAG, "user returned $it")
            if(!it.user.email.isNullOrEmpty()){
                Log.d(TAG,"userUpdateOrder called -> value $it")
                userUpdateOrder = it
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
        Log.d(TAG,"onCreateView -> getting user")
        //db getUser information request
        viewModel?.getUser()
        Log.d(TAG,"onCreateView ends")
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initGoogleMap(savedInstanceState)
        updateOrderNumberOfMeals.minValue = 1
        updateOrderNumberOfMeals.maxValue = 100
        updateOrderNumberOfMeals.wrapSelectorWheel = true
        updateOrderNumberOfMeals.setOnValueChangedListener{picker,oldVal,newVal ->
            Log.d("preOrderFragment", "picker $picker oldValue $oldVal newValue $newVal")
            selectedQtty = newVal
            val mealsPrice = (updateOrderMealPrice.text.toString().split(" ")[1].toDouble())
            Log.d("preOrderFragment", "value ${updateOrderMealPrice.text.toString()} array ${updateOrderMealPrice.text.split(" ")} first value ${updateOrderMealPrice.text.split(" ")[0]} ")
            val total = mealsPrice * newVal
            updateOrderTotalPrice.text = "U$ ${String.format("%.2f",total)}"
        }
        updateOrderCancelBtn.setOnClickListener {

        }
        updateOrderPreOrderBtn.setOnClickListener {
//            updateOrderSpinerLayout.visibility = View.VISIBLE

        }
        updateOrderDeleteOrder.setOnClickListener {

        }
        super.onViewCreated(view, savedInstanceState)
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
    private fun initGoogleMap(savedInstanceState: Bundle?) {
        Log.d("preOrderFragment", "initGoogleMap")
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            Log.d("preOrderFragment", "savedInstance is not null")
            mapViewBundle = savedInstanceState.getBundle(MAPS_API_KEY)
            Log.d("preOrderFragment", "$mapViewBundle")
        }else{
            Log.d("preOrderFragment", "savedInstance is null")
            Log.d("preOrderFragment", "$mapViewBundle")
        }
        updateOrderLocationMap.onCreate(mapViewBundle)
        updateOrderLocationMap.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d("PreOrderFragment","mapMethod called")
        cookingDate?.let {
            Log.d("PreOrderFragment","mapMethod called - inside")
            var position = LatLng(it.cookingDateAndDishes.cookingDate.lat, it.cookingDateAndDishes.cookingDate.lng)
            val dayton = LatLng(39.758949, -84.191605)
            Log.d("PreOrderFragment","mapMethod called - inside - $position")
            map.addMarker(
                com.google.android.gms.maps.model.MarkerOptions()
                    .position(position)
                    .title("KungfuBBQ")
            )
            map.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(position,16.0f))
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("preOrderFragment", "onStart starts")
        updateOrderLocationMap.onStart()
        Log.d("preOrderFragment", "onStart ends")
    }

    override fun onResume() {
        super.onResume()
        updateOrderLocationMap.onResume()
    }

    override fun onPause() {
        super.onPause()
        updateOrderLocationMap.onPause()
    }
    override fun onStop() {
        super.onStop()
        updateOrderLocationMap.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        bag.clear()
        bag.dispose()
        //updateOrderLocationMap.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        updateOrderLocationMap.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        updateOrderLocationMap.onLowMemory()
    }
}