package me.dgmieth.kungfubbq

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.fragment.app.Fragment
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_home.*
import me.dgmieth.kungfubbq.datatabase.KungfuBBQViewModel
import me.dgmieth.kungfubbq.httpRequets.Endpoints
import me.dgmieth.kungfubbq.httpRequets.HttpRequestCtrl
import me.dgmieth.kungfubbq.httpRequets.UserAuthentication
import me.dgmieth.kungfubbq.httpRequets.responsesObjects.userValidation.UserResponseValidation
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val args : HomeFragmentArgs by navArgs()
    //private val viewModel by lazy { ViewModelProvider(this).get(KungfuBBQViewModel::class.java) }
    private val viewModel: KungfuBBQViewModel by activityViewModels()
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
        viewModel.loadTasks()
        val userCredential = JSONObject()
        userCredential.put("email", "dgmieth@gmail.com")
        userCredential.put("password", "12312345")
        val newUserAuth = UserAuthentication("12312345","dgmieth@gmail.com")
        Log.d("HttpRequestCtrl", "jsonObject is $newUserAuth")
        bag.add(
            HttpRequestCtrl.buildService((Endpoints::class.java), getString(R.string.kungfuServerUrl)).login(newUserAuth)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({response -> onResponse(response)}, {t -> onFailure(t) })
        )
    }
    private fun onResponse(response: UserResponseValidation?) {
        response?.let{
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