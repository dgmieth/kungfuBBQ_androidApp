package me.dgmieth.kungfubbq

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_preorder.*
import kotlinx.android.synthetic.main.fragment_updateorder.*

class UpdateOrderFragment : Fragment(R.layout.fragment_updateorder) {
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
        updateOrderNumberOfMeals.minValue = 1
        updateOrderNumberOfMeals.maxValue = 100
        updateOrderNumberOfMeals.wrapSelectorWheel = true
        updateOrderNumberOfMeals.setOnValueChangedListener{picker,oldVal,newVal ->
            Log.d("preOrderFragment", "picker $picker oldValue $oldVal newValue $newVal")
        }
        updateOrderNumberOfMeals.isEnabled = false
        Log.d("preOrderFragment", "onViewCreated ends")

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.getItem(0).isVisible = false
        menu.getItem(1).isVisible = false
        menu.getItem(2).isVisible = true
        menu.getItem(3).isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)

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