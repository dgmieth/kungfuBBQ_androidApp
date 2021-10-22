package me.dgmieth.kungfubbq

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.fragment.app.Fragment
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_home.*
import me.dgmieth.kungfubbq.datatabase.room.KungfuBBQRoomDatabase
import me.dgmieth.kungfubbq.datatabase.room.RoomViewModel
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserAndSocialMedia
import me.dgmieth.kungfubbq.httpCtrl.HttpCtrl
import me.dgmieth.kungfubbq.httpRequets.LoginBodyData
import me.dgmieth.kungfubbq.httpRequets.responsesObjects.userValidation.LoggedUserInfo
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

private const val TAG = "HomeFragment"

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val args : HomeFragmentArgs by navArgs()
    private var roomViewMode: RoomViewModel? = null
    private val bag = CompositeDisposable()

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
        roomViewMode = ViewModelProviders.of(this).get(RoomViewModel::class.java)
        var db = KungfuBBQRoomDatabase.getInstance(requireActivity())
        roomViewMode?.getDbInstance(db)
        roomViewMode?.user?.observe(viewLifecycleOwner, Observer {
            if(!it.user.email.isNullOrEmpty()){
                handleIt(it)
            }else{
                handleNullCase()
            }
        })
        roomViewMode?.getUser()
        //viewModel.loadTasks()
        val userCredential = JSONObject()
        userCredential.put("email", "dgmieth@gmail.com")
        userCredential.put("password", "12312345")
        val newUserAuth = LoginBodyData("12312345","dgmieth@gmail.com")
        Log.d("HttpRequestCtrl", "jsonObject is $newUserAuth")


            HttpCtrl.shared.newCall(HttpCtrl.get("https://gorest.co.in/public/v1/users","")).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        for ((name, value) in response.headers) {
                            println("$name: $value")
                        }
                        val json = JSONObject(response.body!!.string())
                        println(json.getJSONArray("data").getJSONObject(0).getInt("id")==3878)
                    }
                }
            })
    }

    private fun handleNullCase() {
        Log.d(TAG, "no data returned")
    }

    private fun handleIt(it: UserAndSocialMedia) {
        Log.d(TAG, "dataReturn was ${it.user.email} and ${it.user.userId}")

    }

    private fun onResponse(responseLogged: LoggedUserInfo?) {
        responseLogged?.let{
            Log.d("HttpRequestCtrl", "values are $it")
        }
    }

    private fun onFailure(t: Throwable?) {
        Log.d("HttpRequestCtrl", "failed $t")
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