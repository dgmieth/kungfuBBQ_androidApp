package me.dgmieth.kungfubbq

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.fragment_payorder.*
import kotlinx.android.synthetic.main.fragment_preorder.*

class PayOrderFragment : Fragment(R.layout.fragment_payorder) {
    private val MAPS_API_KEY = "google_maps_api_key"

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
        Log.d("preOrderFragment", "onCreateViewStarts")
        Log.d("preOrderFragment", "onCreateViewEnds")
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("preOrderFragment", "onViewCreated starts")

        Log.d("preOrderFragment", "onViewCreated ends")
        payOrderPayBtn.setOnClickListener {
            val action = PayOrderFragmentDirections.callPay()
            findNavController().navigate(action)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
//    private fun initGoogleMap(savedInstanceState: Bundle?) {
//        // *** IMPORTANT ***
//        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
//        // objects or sub-Bundles.
//        Log.d("preOrderFragment", "initGoogleMap")
//        var mapViewBundle: Bundle? = null
//        if (savedInstanceState != null) {
//            Log.d("preOrderFragment", "savedInstance is not null")
//            mapViewBundle = savedInstanceState.getBundle(MAPS_API_KEY)
//            Log.d("preOrderFragment", "$mapViewBundle")
//        }else{
//            Log.d("preOrderFragment", "savedInstance is null")
//            Log.d("preOrderFragment", "$mapViewBundle")
//        }
//            mapView.onCreate(mapViewBundle)
//            mapView.getMapAsync(this)
//
//    }
//
//    override fun onMapReady(map: GoogleMap) {
//        Log.d("PreOrderFragment","mapMethod called")
//        val dayton = LatLng(0.0, 0.0)
//
//        map.addMarker(
//            MarkerOptions()
//            .position(dayton)
//            .title("Datyon")
//        )
//        map.moveCamera(CameraUpdateFactory.newLatLng(dayton))
//    }
//
//    override fun onStart() {
//        super.onStart()
//        Log.d("preOrderFragment", "onStart starts")
//        mapView.onStart()
//        Log.d("preOrderFragment", "onStart ends")
//    }
//
//    override fun onResume() {
//        super.onResume()
//        mapView.onResume()
//    }
//
//    override fun onPause() {
//        super.onPause()
//        mapView.onPause()
//    }
//    override fun onStop() {
//        super.onStop()
//        mapView.onStop()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        mapView.onDestroy()
//    }
//
//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//        mapView.onSaveInstanceState(outState)
//    }
//
//    override fun onLowMemory() {
//        super.onLowMemory()
//        mapView.onLowMemory()
//    }

}