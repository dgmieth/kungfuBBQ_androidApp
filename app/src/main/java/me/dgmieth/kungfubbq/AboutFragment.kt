package me.dgmieth.kungfubbq

import android.content.Intent
import android.net.Uri
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
import kotlinx.android.synthetic.main.fragment_about.*
import kotlinx.android.synthetic.main.fragment_home.*

class AboutFragment : Fragment(R.layout.fragment_about) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        aboutLInk.setOnClickListener {
            val url = "https://dgmieth.me"
            val i = Intent(Intent.ACTION_VIEW)
            i.setData(Uri.parse(url))
            startActivity(i)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
}