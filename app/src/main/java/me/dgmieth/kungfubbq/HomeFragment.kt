package me.dgmieth.kungfubbq

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.fragment.app.Fragment
import android.view.View
import androidx.core.view.get
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_home.*
import me.dgmieth.kungfubbq.datatabase.KungfuBBQViewModel

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val args : HomeFragmentArgs by navArgs()
    private val viewModel by lazy { ViewModelProvider(this).get(KungfuBBQViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        Log.d("HomeFragment","onCreate starts -> ${args.loggedIn}")
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("HomeFragment","onViewCreated starts")
        if(!args.loggedIn){
            Log.d("HomeFragment","onViewCreated starts -> not loggedIn")
            homeLoginBtn.setVisibility(View.VISIBLE)
            homeCalendarBtn.setVisibility(View.INVISIBLE)
        }else{
            Log.d("HomeFragment","onViewCreated starts -> loggedIn")
            homeLoginBtn.setVisibility(View.INVISIBLE)
            homeCalendarBtn.setVisibility(View.VISIBLE)
        }
        homeLoginBtn.setOnClickListener { goToLoginFragment() }
        homeCateringBtn.setOnClickListener { goToCateringFragment() }
        homeCalendarBtn.setOnClickListener { goToCalendarFragment() }
        Log.d("HomeFragment","onViewCreated starts -> end")
        viewModel.loadTasks()
    }
    private fun goToLoginFragment(){
        val action = NavGraphDirections.actionGlobalLoginFragment()
        findNavController().navigate(action)
    }
    private fun goToCateringFragment(){
        val action = HomeFragmentDirections.callCatering()
        findNavController().navigate(action)
    }
    private fun goToCalendarFragment(){
        val action = HomeFragmentDirections.callCalendar()
        findNavController().navigate(action)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val userinfo = menu.getItem(0)
        userinfo.setVisible(args.loggedIn)

    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.userInfoMenuBtn){
            val action = HomeFragmentDirections.callUserInfoGlobal()
            findNavController().navigate(action)
            return true
        }else if (item.itemId == R.id.aboutAppMenuBtn) {
            val action = HomeFragmentDirections.callAbout()
            findNavController().navigate(action)
            return true
        }
        else{
            return  super.onOptionsItemSelected(item)
        }
    }
}