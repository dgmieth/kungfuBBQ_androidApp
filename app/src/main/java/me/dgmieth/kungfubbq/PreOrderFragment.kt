package me.dgmieth.kungfubbq

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.fragment_about.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_preorder.*

class PreOrderFragment : Fragment(R.layout.fragment_preorder), OnMapReadyCallback {
    private val MAPS_API_KEY = "MAPS_API_KEY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)

    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initGoogleMap(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
    private fun initGoogleMap(savedInstanceState: Bundle?) {
        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPS_API_KEY)
        }
        Log.d("preOrderFragment", "$mapViewBundle")
        preOrderLocationMap.onCreate(Bundle())
        preOrderLocationMap.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d("PreOrderFragment","mapMethod called")
        val dayton = LatLng(-34.0, 151.0)

        map.addMarker(
            MarkerOptions()
            .position(dayton)
            .title("Datyon")
        )
        map.moveCamera(CameraUpdateFactory.newLatLng(dayton))
    }

}