package me.dgmieth.kungfubbq

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.core.view.isVisible
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import me.dgmieth.kungfubbq.databinding.FragmentHomeBinding
import me.dgmieth.kungfubbq.datatabase.roomEntities.*
import me.dgmieth.kungfubbq.httpCtrl.HttpCtrl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
import java.time.LocalDate

var USER_LOGGED = false

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val TAG = "HomeFragment"
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val args : HomeFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkVersionCode()
//        checkSauseFundingCampaignStatus()
        selectedDate = LocalDate.now()
        Log.d(TAG,"checkVersionCode called, ${getString(R.string.kungfuServerUrlNoSchema)}")
        binding.homeLoginBtn.isVisible = !args.loggedIn
        binding.homeCalendarBtn.isVisible = args.loggedIn
        binding.homeLoginBtn.setOnClickListener { goToLoginFragment() }
        binding.homeCateringBtn.setOnClickListener { goToCateringFragment() }
        binding.homeCalendarBtn.setOnClickListener { goToCalendarFragment() }
        binding.homeSauseFunding.setOnClickListener { goToSauseFunding() }
        binding.homeCallButton.setOnClickListener{
            Log.d(TAG,"callBtn called")
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${getString(R.string.kungfuPhoneNumber)}")
            }
            requireContext().startActivity(intent)
        }
        binding.homeDMButton.setOnClickListener {
            try {
                requireContext().packageManager.getPackageInfo("com.facebook.katana", 0)
                var intent = Intent(Intent.ACTION_VIEW, Uri.parse("${getString(R.string.kungfuFacebookPage)}"))
                requireContext().startActivity(intent)
            } catch (e: Exception) {
                var intent = Intent(Intent.ACTION_VIEW, Uri.parse("${getString(R.string.kungfuFacebookLink)}"))
                requireContext().startActivity(intent)

            }
        }
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val userinfo = menu.getItem(0)
        if(!USER_LOGGED&&!args.loggedIn){
            userinfo.isVisible = false
        }else if (USER_LOGGED || args.loggedIn){
            userinfo.isVisible = true
        }
        if(getString(R.string.development)=="true"){
            binding.developmentFlag.isVisible = true
        }
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
        userNavController(action)
    }
    private fun goToCateringFragment(){
        val action = HomeFragmentDirections.callCatering(args.loggedIn)
        userNavController(action)
    }
    private fun goToCalendarFragment(){
        binding.homeCalendarBtn.isEnabled = false
        val action = HomeFragmentDirections.callCalendar()
        userNavController(action)
    }
    private fun goToSauseFunding(){
        val action = HomeFragmentDirections.callSauseFunding()
        userNavController(action)
    }
    private fun userNavController(action : NavDirections){
        findNavController().navigate(action)
    }
    //=====================================================================
    //checking version code
    private fun checkVersionCode() {
        var httpUrl = HttpUrl.Builder()
            .scheme("https")
            .host("${getString(R.string.kungfuServerUrlNoSchema)}")
            .addQueryParameter("version_code","${BuildConfig.VERSION_CODE}")
            .addQueryParameter("mobileOS","android")
            .addPathSegments("api/osVersion/checkVersion")
            .build()
        HttpCtrl.shared.newCall(HttpCtrl.get("","",httpUrl,"")).enqueue(object :
            Callback {
            override fun onFailure(call: Call, e: IOException) {
                showAlert("KungfuBBQ server cannot be reached. Try again in some minutes. If the problem persists, please contact KungfuBBQ.","Error!")
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        showAlert("KungfuBBQ server cannot be reached. Try again in some minutes. If the problem persists, please contact KungfuBBQ.","Error!")
                        throw IOException("Unexpected code $response")
                    }
                    val json = JSONObject(response.body!!.string())
                    Log.d(TAG,"values are $json")
                    if(json.getBoolean("hasErrors")){
                       showAlert("${json.getString("msg")}","App update required!")
                    }
                }
            }
        })
    }
    //=====================================================================
//checking version code
    private fun checkSauseFundingCampaignStatus() {
        if(!args.loggedIn){
            showHideSauseFundingBtn("off")
            return
        }
        var httpUrl = HttpUrl.Builder()
            .scheme("https")
            .host("${getString(R.string.kungfuServerUrlNoSchema)}")
            .addPathSegments("api/sause/checkstatus")
            .build()
        HttpCtrl.shared.newCall(HttpCtrl.get("","",httpUrl,"")).enqueue(object :
            Callback {
            override fun onFailure(call: Call, e: IOException) {
                showAlert("KungfuBBQ server cannot be reached. Try again in some minutes. If the problem persists, please contact KungfuBBQ.","Error!")
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        showHideSauseFundingBtn("off")
                        throw IOException("Unexpected code $response")
                    }
                    val json = JSONObject(response.body!!.string())
                    Log.d(TAG,"checkSauseFundingCampaignStatus -> values are $json ${json.getJSONObject("msg").getString("status")}")
                    showHideSauseFundingBtn(json.getJSONObject("msg").getString("status"))
                    if(json.getBoolean("hasErrors")){
                        showHideSauseFundingBtn(json.getJSONObject("msg").getString("status"))
                    }
                }
            }
        })
    }
    private fun showHideSauseFundingBtn(status : String){
        if(status=="off"){
            Handler(Looper.getMainLooper()).post {
                binding.homeSauseFunding.isVisible = false
            }
        }
    }
    //=====================================================================
    private fun showAlert(message:String,title:String){
        Handler(Looper.getMainLooper()).post{
            var dialogBuilder = AlertDialog.Builder(requireContext())
            dialogBuilder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Ok", DialogInterface.OnClickListener{
                        _, _ ->
                    if(title=="App update required!"){
                        binding.homeLoginBtn.isVisible = false
                    }else{
                        binding.homeLoginBtn.isVisible = false
                        binding.homeCateringBtn.isVisible = false
                    }
                })
            val alert = dialogBuilder.create()
            alert.setTitle(title)
            alert.show()
        }
    }
}