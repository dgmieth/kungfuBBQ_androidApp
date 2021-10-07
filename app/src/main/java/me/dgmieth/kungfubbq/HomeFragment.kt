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
import kotlinx.android.synthetic.main.fragment_home.*

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val args : HomeFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(!args.loggedIn){
            homeLoginBtn.isVisible = !args.loggedIn
            homeCalendarBtn.isVisible = args.loggedIn
        }else{
            homeLoginBtn.isVisible = args.loggedIn
            homeCalendarBtn.isVisible = !args.loggedIn
        }
        homeLoginBtn.setOnClickListener { goToLoginFragment() }
    }
    private fun goToLoginFragment(){
        val action = NavGraphDirections.actionGlobalLoginFragment()
        findNavController().navigate(action)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.getItem(0).isEnabled = args.loggedIn
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.userInfoMenuBtn){
            val action = HomeFragmentDirections.callUserInfo()
            findNavController().navigate(action)
            return true
        }else{
            return  super.onOptionsItemSelected(item)
        }
    }
}