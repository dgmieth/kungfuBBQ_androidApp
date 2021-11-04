package me.dgmieth.kungfubbq

import android.app.AlertDialog
import android.content.DialogInterface
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_paidorder.*
import me.dgmieth.kungfubbq.datatabase.room.KungfuBBQRoomDatabase
import me.dgmieth.kungfubbq.datatabase.room.RoomViewModel
import me.dgmieth.kungfubbq.datatabase.roomEntities.CookingDateAndCookingDateDishesWithOrder
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserAndSocialMedia
import java.util.*

class PaidOrderFragment : Fragment(R.layout.fragment_paidorder),OnMapReadyCallback {
    private val TAG = "PaidOrderFragment"

    private val MAPS_API_KEY = "google_maps_api_key"
    private var viewModel: RoomViewModel? = null
    private var cookingDate : CookingDateAndCookingDateDishesWithOrder? = null
    private var userPaidOrder : UserAndSocialMedia? = null

    private var bag = CompositeDisposable()

    private val args : PayOrderFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        Log.d("preOrderFragment", "onCreate starts")
        Log.d("preOrderFragment", "onCreate ends")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProviders.of(this).get(RoomViewModel::class.java)
        var db = KungfuBBQRoomDatabase.getInstance(requireActivity())
        viewModel?.getDbInstance(db)
        //Subscribing to user
        viewModel?.user?.observe(viewLifecycleOwner, Observer {
            Log.d(TAG, "user returned $it")
            viewModel?.getCookingDates()
            if(!it.user.email.isNullOrEmpty()){
                Log.d(TAG,"userUpdateOrder called -> value $it")
                userPaidOrder = it
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
            Log.d(TAG,"cookingDates called with cookingDateId ${args.cookingDateId}")
            var cdArray = it.filter { c -> c.cookingDateAndDishes.cookingDate.cookingDateId == args.cookingDateId }
            cookingDate = cdArray[0]
            cookingDate?.let { cd ->
                Log.d(TAG,"cookingDates called -> value $cd")
                //updating date
                val splitDate = (cd.cookingDateAndDishes.cookingDate.cookingDate.split(" ")[0]).split("-")
                val cal = Calendar.getInstance()
                cal.set(splitDate[0].toInt(), splitDate[1].toInt()-1, splitDate[2].toInt())
                val dateStrParts = cal.time.toString().split(" ")
                paidOrderDate.text = "${dateStrParts[1]} ${dateStrParts[2]}"
                //updating status
                paidOrderStatus.text = cd.cookingDateAndDishes.cookingDate.cookingStatus
                //updating menu
                var menuT = ""
                var menuIndex = 1
                var mealsSum = 0.0
                for(m in cd.cookingDateAndDishes.cookingDateDishes){
                    menuT = "${menuT}${menuIndex}- ${m.dishName} - U$${m.dishPrice}\n"
                    menuIndex += 1
                    mealsSum += m.dishPrice.toDouble()
                }
                paidOrderMenu.text = menuT
                //updating maps
                paidOrderLocationText.text = "${cd.cookingDateAndDishes.cookingDate.street}, ${cd.cookingDateAndDishes.cookingDate.city}"
                paidOrderLocationMap.getMapAsync(this)
                var priceString = "U$ ${String.format("%.2f",mealsSum)}"
                //updating meal price
                paidOrderMealPrice.text = priceString
                //updating total meal price
                paidOrderNumberOfMeals.text = cd.order[0].dishes[0].dishQuantity.toString()
                paidOrderTotalPrice.text = "U$ ${String.format("%.2f",mealsSum*cd.order[0].dishes[0].dishQuantity)}"
            }
        },{
            Log.d("CookingDateObservable","error is $it")
        },{})?.let {
            bag.add(it)
        }
        Log.d(TAG,"onCreateView -> getting user")
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
        Log.d(TAG,"onCreateView ends")
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("preOrderFragment", "onViewCreated starts")
        initGoogleMap(savedInstanceState)
        Log.d("preOrderFragment", "onViewCreated ends")

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
        paidOrderLocationMap.onCreate(mapViewBundle)
        paidOrderLocationMap.getMapAsync(this)
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
//        paidOrderLocationMap.onStart()
        Log.d("preOrderFragment", "onStart ends")
    }

    override fun onResume() {
        super.onResume()
        paidOrderLocationMap.onResume()
    }

    override fun onPause() {
        super.onPause()
        paidOrderLocationMap.onPause()
    }
    override fun onStop() {
        super.onStop()
        paidOrderLocationMap.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        bag.clear()
        bag.dispose()
        //updateOrderLocationMap.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        paidOrderLocationMap.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        paidOrderLocationMap.onLowMemory()
    }

}