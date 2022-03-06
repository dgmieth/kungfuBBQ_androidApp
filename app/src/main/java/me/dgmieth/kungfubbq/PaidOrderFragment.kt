package me.dgmieth.kungfubbq

import androidx.appcompat.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
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
import me.dgmieth.kungfubbq.databinding.FragmentPaidorderBinding
import me.dgmieth.kungfubbq.datatabase.room.KungfuBBQRoomDatabase
import me.dgmieth.kungfubbq.datatabase.room.RoomViewModel
import me.dgmieth.kungfubbq.datatabase.roomEntities.CookingDateAndCookingDateDishesWithOrder
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserAndSocialMedia
import me.dgmieth.kungfubbq.support.formatter.FormatObject
import java.util.*

class PaidOrderFragment : Fragment(R.layout.fragment_paidorder),OnMapReadyCallback {
    private val TAG = "PaidOrderFragment"

    private val MAPS_API_KEY = "google_maps_api_key"
    private var viewModel: RoomViewModel? = null
    private var cookingDate : CookingDateAndCookingDateDishesWithOrder? = null
    private var userPaidOrder : UserAndSocialMedia? = null
    private var btnClick = true

    private var bag = CompositeDisposable()

    private val args : PayOrderFragmentArgs by navArgs()

    private var _binding: FragmentPaidorderBinding? = null
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
        viewModel = ViewModelProviders.of(this).get(RoomViewModel::class.java)
        viewModel?.getDbInstance(KungfuBBQRoomDatabase.getInstance(requireActivity()))
        //Subscribing to user
        viewModel?.user?.observe(viewLifecycleOwner, Observer {
            viewModel?.getCookingDates()
            if(!it.user.email.isNullOrEmpty()){
                userPaidOrder = it
            }else{
                showAlert("It was not possible to retrieve information from this app's database. Please restart the app.",getString(R.string.database_error))
            }
        })
        //Subscribing to cookingD
        viewModel?.cookingDates?.subscribe({
            var cdArray = it.filter { c -> c.cookingDateAndDishes.cookingDate.cookingDateId == args.cookingDateId }
            cookingDate = cdArray[0]
            cookingDate?.let { cd ->
                Log.d(TAG,"cookingDates called -> value $cd")
                //updating date
//                val splitDate = (cd.cookingDateAndDishes.cookingDate.cookingDate.split(" ")[0]).split("-")
//                val cal = Calendar.getInstance()
//                cal.set(splitDate[0].toInt(), splitDate[1].toInt()-1, splitDate[2].toInt())
//                val dateStrParts = cal.time.toString().split(" ")
                binding.paidOrderDate.text = FormatObject.returnEventTime()
                binding.orderNr.text = cd.order[0].order.orderId.toString()
                //updating status
                binding.paidOrderStatus.text = cd.cookingDateAndDishes.cookingDate.cookingStatus
                //updating menu
//                var menuT = ""
//                var menuIndex = 1
//                var mealsSum = 0.0
//                for(m in cd.cookingDateAndDishes.cookingDateDishes){
//                    menuT = "${menuT}${menuIndex}- ${m.dishName} - U$${m.dishPrice}\n"
//                    menuIndex += 1
//                    mealsSum += m.dishPrice.toDouble()
//                }
//                binding.paidOrderMenu.text = menuT
                binding.paidOrderMenu.text = Html.fromHtml(FormatObject.formatDishesListForMenuScrollViews(cd.cookingDateAndDishes.cookingDateDishes), Html.FROM_HTML_MODE_COMPACT)
                //updating maps
                binding.paidOrderLocationText.text = FormatObject.returnAddress()
                binding.paidOrderLocationMap.getMapAsync(this)
                var priceString = "U$ ${String.format("%.2f",FormatObject.returnMealBoxTotalAmount())}"
                //updating meal price
                binding.paidOrderMealPrice.text = priceString
                //updating total meal price
                binding.paidOrderNumberOfMeals.text = cd.order[0].dishes[0].dishQuantity.toString()
                binding.paidOrderTip.text = "U$ ${String.format("%.2f",cd.order[0].order.tipAmount)}"
                binding.paidOrderTotalPrice.text = "U$ ${String.format("%.2f",(FormatObject.returnMealBoxTotalAmount()*cd.order[0].dishes[0].dishQuantity)+cd.order[0].order.tipAmount)}"
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
            var dialogBuilder = AlertDialog.Builder(requireContext())
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
        _binding = FragmentPaidorderBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initGoogleMap(savedInstanceState)
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
    //===================================================
    // maps
    private fun initGoogleMap(savedInstanceState: Bundle?) {
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPS_API_KEY)
        }
        binding.paidOrderLocationMap.onCreate(mapViewBundle)
        binding.paidOrderLocationMap.getMapAsync(this)
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
    override fun onResume() {
        super.onResume()
        binding.paidOrderLocationMap.onResume()
        if(!btnClick){
            val action = HomeFragmentDirections.callHome(true)
            findNavController().navigate(action)
        }
        btnClick = false
    }
    override fun onPause() {
        super.onPause()
        binding.paidOrderLocationMap.onPause()
    }
    override fun onStop() {
        super.onStop()
        binding.paidOrderLocationMap.onStop()
    }
    override fun onDestroy() {
        super.onDestroy()
        bag.clear()
        bag.dispose()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.paidOrderLocationMap.onSaveInstanceState(outState)
    }
    override fun onLowMemory() {
        super.onLowMemory()
        binding.paidOrderLocationMap.onLowMemory()
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