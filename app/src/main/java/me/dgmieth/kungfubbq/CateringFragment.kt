package me.dgmieth.kungfubbq

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.fragment.app.Fragment
import android.view.View
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_catering.*
import kotlinx.android.synthetic.main.fragment_home.*

class CateringFragment : Fragment(R.layout.fragment_catering) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        cateringCancelBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }
}