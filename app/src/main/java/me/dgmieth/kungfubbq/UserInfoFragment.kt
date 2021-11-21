package me.dgmieth.kungfubbq

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneNumberUtils
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import io.reactivex.disposables.CompositeDisposable
import me.dgmieth.kungfubbq.databinding.FragmentUserinfoBinding
import me.dgmieth.kungfubbq.datatabase.room.Actions
import me.dgmieth.kungfubbq.datatabase.room.KungfuBBQRoomDatabase
import me.dgmieth.kungfubbq.datatabase.room.RoomViewModel
import me.dgmieth.kungfubbq.datatabase.roomEntities.SocialMediaInfo
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserAndSocialMedia
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserDB
import me.dgmieth.kungfubbq.httpCtrl.HttpCtrl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class UserInfoFragment : Fragment(R.layout.fragment_userinfo) {

    private val TAG = "UserInfoFragment"

    private var viewModel: RoomViewModel? = null
    private var userInfo : UserAndSocialMedia? = null
    private var editItemBtn : MenuItem? = null
    private var noFormatPhoneNbm : String? = null

    private var bag = CompositeDisposable()
    private val phoneTextWatcher = object: TextWatcher {
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            formatPhoneNumber()
        }
    }

    private var _binding: FragmentUserinfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProviders.of(this).get(RoomViewModel::class.java)
        var db = KungfuBBQRoomDatabase.getInstance(requireActivity())
        viewModel?.getDbInstance(db)
        //subscribing to returnMsg
        viewModel?.returnMsg?.subscribe(
            {
                when(it){
                    Actions.UserComplete ->{
                        viewModel?.getUser()
                    }
                    else -> {
                        Handler(Looper.getMainLooper()).post{
                            showSpinner(false)
                            Toast.makeText(requireActivity(),"The attempt to retrieve user information from the database failed",Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            {
                Handler(Looper.getMainLooper()).post{
                    showSpinner(false)
                    Toast.makeText(requireActivity(),"Log in attempt failed. Please try again in some minutes",Toast.LENGTH_LONG).show()
                }
            },{})?.let{ bag.add(it)}
        //Subscribing to user
        viewModel?.user?.observe(viewLifecycleOwner, Observer {
            viewModel?.getCookingDates()
            if(!it.user.email.isNullOrEmpty()){
                userInfo = it
                userInfo?.let {
                    var user = it
                    setUIElements(user)
                    showSpinner(false)
                    showSaveBtn(false)
                }
            }else{
                showSpinner(false)
                Handler(Looper.getMainLooper()).post{
                    Toast.makeText(requireActivity(),"It was not possible to retrieve information from this app's database. Please restart the app.",
                        Toast.LENGTH_LONG).show()
                    var action = CalendarFragmentDirections.callCalendarFragmentGlobal()
                    findNavController().navigate(action)
                }
            }
        })
        viewModel?.getUser()
        _binding = FragmentUserinfoBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        //click listeners
        binding.userInfoCancelBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
        binding.userInfoUpdatePasswordBtn.setOnClickListener {
            val action = UserInfoFragmentDirections.callUpdatePassword(userInfo!!.user.token.toString(),userInfo!!.user.email,userInfo!!.user.userId.toString())
            findNavController().navigate(action)
        }
        binding.userInfoCancelBtn.setOnClickListener {
            showSaveBtn(false)
        }
        binding.userInfoSaveBtn.setOnClickListener {
            if(validateInfo()){
                showSpinner(true)
                updateInfo()
            }
        }
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.getItem(0).isVisible = false
        menu.getItem(1).isVisible = true
        menu.getItem(2).isVisible = true
        menu.getItem(3).isVisible = false
        editItemBtn = menu.getItem(2)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.logOutMenuBtn -> {
                val action = HomeFragmentDirections.callHome(false)
                findNavController().navigate(action)
                true
            }
            R.id.editMenuBtn -> {
                showSaveBtn(true)
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }
    //===========================================================
    // validation
    private fun validateInfo():Boolean{
        if(binding.userInfoPhone.text.toString().isEmpty()){
            return true
        }else if(noFormatPhoneNbm==null){
            Handler(Looper.getMainLooper()).post{
                Toast.makeText(requireActivity(),"Incorrect phone number",Toast.LENGTH_LONG).show()
            }
            return false
        }else if(binding.userInfoName.text.toString()==userInfo!!.user.name.toString() &&
            noFormatPhoneNbm==userInfo!!.user.phoneNumber.toString() &&
            binding.userInfoFacebookName.text.toString()==userInfo!!.socialMedias[0].socialMediaName.toString() &&
            binding.userInfoInstagramName.text.toString()==userInfo!!.socialMedias[1].socialMediaName.toString() ){
            Handler(Looper.getMainLooper()).post{
                Toast.makeText(requireActivity(),"No user information was changed",Toast.LENGTH_LONG).show()
            }
            return false
        }
        return true
    }
    //===========================================================
    // http requests
    private fun updateInfo(){
        val body = FormBody.Builder()
            .add("email",if(binding.userInfoUserName.text.toString().isNullOrEmpty()) "none" else binding.userInfoUserName.text.toString())
            .add("name",if(binding.userInfoName.text.toString().isNullOrEmpty()) "none" else binding.userInfoName.text.toString())
            .add("id",userInfo!!.user.userId.toString())
            .add("phoneNumber", if(noFormatPhoneNbm.isNullOrEmpty()) "none" else noFormatPhoneNbm!!)
            .add("facebookName", if(binding.userInfoFacebookName.text.toString().isNullOrEmpty()) "none" else binding.userInfoFacebookName.text.toString())
            .add("instagramName",if(binding.userInfoInstagramName.text.toString().isNullOrEmpty()) "none" else binding.userInfoInstagramName.text.toString())
            .build()
        HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/api/user/updateInfo",body,userInfo!!.user.token.toString())).enqueue(object :
            Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                showSpinner(false)
                Handler(Looper.getMainLooper()).post{
                    Toast.makeText(requireActivity(),"The attempt to update your user information failed with server message: ${e.localizedMessage}",
                        Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    showSpinner(false)
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val json = JSONObject(response.body!!.string())
                    if(!json.getBoolean("hasErrors")){
                        val u = json.getJSONObject("data")
                        val user = UserDB(u.getInt("id"),u.getString("email"),u.getString("memberSince"),u.getString("name"),u.getString("phoneNumber"),u.getString("token"),1)
                        var socialM : MutableList<SocialMediaInfo> = arrayListOf()
                        for (i in 0 until u.getJSONArray("socialMediaInfo").length()){
                            val s = u.getJSONArray("socialMediaInfo").getJSONObject(i)
                            var sMInfo = SocialMediaInfo(s.getString("socialMedia"),s.getString("socialMediaName"),u.getInt("id"))
                            socialM.add(sMInfo)
                        }
                        Log.d(TAG, "socialMedia $socialM")
                        viewModel?.insertAllUserInfo(user,socialM)
                    }else{
                        if(json.getInt("errorCode")==-1){
                            Handler(Looper.getMainLooper()).post{
                                Toast.makeText(requireActivity(),"You are not authenticated in Kungfu BBQ server anylonger. Please log in again.",
                                    Toast.LENGTH_LONG).show()
                                val action = NavGraphDirections.callHome(false)
                                findNavController().navigate(action)
                            }
                        }else{
                            Handler(Looper.getMainLooper()).post{
                                Toast.makeText(requireActivity(),"The attempt to update your user information failed with server message: ${json.getString("msg")}",
                                    Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        })
    }
    //===========================================
    //data manipulation
    private fun formatPhoneNumber() {
        if(binding.userInfoPhone.text.toString().length==10){
            binding.userInfoPhone.removeTextChangedListener(phoneTextWatcher)
            noFormatPhoneNbm = binding.userInfoPhone.text.toString()
            var number = PhoneNumberUtils.formatNumber(binding.userInfoPhone.text.toString(),"US")
            binding.userInfoPhone.setText(number)
            binding.userInfoPhone.setSelection(binding.userInfoPhone.text.toString().length)
            binding.userInfoPhone.addTextChangedListener(phoneTextWatcher)
        }else{
            var text = binding.userInfoPhone.text.toString().replace("""[^0-9]""".toRegex(),"")
            noFormatPhoneNbm = null
            binding.userInfoPhone.removeTextChangedListener(phoneTextWatcher)
            binding.userInfoPhone.setText(text)
            binding.userInfoPhone.setSelection(binding.userInfoPhone.text.toString().length)
            binding.userInfoPhone.addTextChangedListener(phoneTextWatcher)
        }
    }
    //===========================================================
    // ui elements
    private fun showSaveBtn(value:Boolean){
        Handler(Looper.getMainLooper()).post {
            editItemBtn!!.isVisible = !value
            binding.userInfoBtnGroup.isVisible = value
            binding.userInfoUpdatePasswordBtn.isVisible = !value
            binding.userInfoName.isEnabled = value
            binding.userInfoPhone.isEnabled = value
            binding.userInfoFacebookName.isEnabled = value
            binding.userInfoInstagramName.isEnabled = value
            if(value){
                binding.userInfoName.setBackgroundColor(Color.WHITE)
                binding.userInfoPhone.setBackgroundColor(Color.WHITE)
                binding.userInfoFacebookName.setBackgroundColor(Color.WHITE)
                binding.userInfoInstagramName.setBackgroundColor(Color.WHITE)
            }else{
                binding.userInfoName.setBackgroundColor(Color.TRANSPARENT)
                binding.userInfoPhone.setBackgroundColor(Color.TRANSPARENT)
                binding.userInfoFacebookName.setBackgroundColor(Color.TRANSPARENT)
                binding.userInfoInstagramName.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }
    private fun setUIElements(user: UserAndSocialMedia){
        Handler(Looper.getMainLooper()).post {
            binding.userInfoPhone.removeTextChangedListener(phoneTextWatcher)
            binding.userInfoUserName.text = user.user.email
            binding.userInfoMemberSince.text = user.user.memberSince
            binding.userInfoName.setText(user.user.name)
            if(user.user.phoneNumber.toString().length==10){
                var number = PhoneNumberUtils.formatNumber(user.user.phoneNumber.toString(),"US")
                binding.userInfoPhone.setText(number)
                noFormatPhoneNbm = user.user.phoneNumber.toString()
            }else{
                binding.userInfoPhone.setText(user.user.phoneNumber)
            }
            binding.userInfoFacebookName.setText(user.socialMedias[0].socialMediaName)
            binding.userInfoInstagramName.setText(user.socialMedias[1].socialMediaName)
            binding.userInfoPhone.addTextChangedListener(phoneTextWatcher)
        }
    }
    private fun showSpinner(value: Boolean){
        Handler(Looper.getMainLooper()).post {
            binding.userInfoSpinnerLayout.visibility =  when(value){
                true -> View.VISIBLE
                else -> View.INVISIBLE
            }
        }
    }
}