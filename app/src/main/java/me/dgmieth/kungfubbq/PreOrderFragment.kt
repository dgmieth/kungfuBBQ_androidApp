package me.dgmieth.kungfubbq

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.fragment_preorder.*

class PreOrderFragment : Fragment(R.layout.fragment_preorder),OnMapReadyCallback {
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
        initGoogleMap(savedInstanceState)
        Log.d("preOrderFragment", "onViewCreated starts")
        preOrderNumberOfMeals.minValue = 1
        preOrderNumberOfMeals.maxValue = 100
        preOrderNumberOfMeals.wrapSelectorWheel = true
        preOrderNumberOfMeals.setOnValueChangedListener{picker,oldVal,newVal ->
            Log.d("preOrderFragment", "picker $picker oldValue $oldVal newValue $newVal")
        }

        Log.d("preOrderFragment", "onViewCreated ends")

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
    private fun initGoogleMap(savedInstanceState: Bundle?) {
        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.
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
        preOrderLocationMap.onCreate(mapViewBundle)
        preOrderLocationMap.getMapAsync(this)

    }

    override fun onMapReady(map: GoogleMap) {
        Log.d("PreOrderFragment","mapMethod called")
        val dayton = LatLng(39.758949, -84.191605)

        map.addMarker(
            MarkerOptions()
            .position(dayton)
            .title("Datyon")
        )
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(dayton,16.0f))
    }

    override fun onStart() {
        super.onStart()
        Log.d("preOrderFragment", "onStart starts")
        preOrderLocationMap.onStart()
        Log.d("preOrderFragment", "onStart ends")
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
        preOrderLocationMap.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        preOrderLocationMap.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        preOrderLocationMap.onLowMemory()
    }

}