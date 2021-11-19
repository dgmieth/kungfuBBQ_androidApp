package me.dgmieth.kungfubbq

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.fragment.app.Fragment
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_home.*


class HomeFragment : Fragment(R.layout.fragment_home) {

    private val TAG = "HomeFragment"

    private val args : HomeFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homeLoginBtn.isVisible = !args.loggedIn
        homeCalendarBtn.isVisible = args.loggedIn
        homeLoginBtn.setOnClickListener { goToLoginFragment() }
        homeCateringBtn.setOnClickListener { goToCateringFragment() }
        homeCalendarBtn.setOnClickListener { goToCalendarFragment() }
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val userinfo = menu.getItem(0)
        userinfo.isVisible = args.loggedIn

    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.userInfoMenuBtn -> {
                val action = HomeFragmentDirections.callUserInfoGlobal()
                findNavController().navigate(action)
                true
            }
            R.id.aboutAppMenuBtn -> {
                val action = HomeFragmentDirections.callAbout()
                findNavController().navigate(action)
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }
    //======================================================================
    //button click listeners
    private fun goToLoginFragment(){
        val action = NavGraphDirections.actionGlobalLoginFragment()
        findNavController().navigate(action)
    }
    private fun goToCateringFragment(){
        val action = HomeFragmentDirections.callCatering(args.loggedIn)
        findNavController().navigate(action)
    }
    private fun goToCalendarFragment(){
        homeCalendarBtn.isEnabled = false
        val action = HomeFragmentDirections.callCalendar()
        findNavController().navigate(action)
    }
}