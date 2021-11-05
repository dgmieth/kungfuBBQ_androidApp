package me.dgmieth.kungfubbq

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneNumberUtils
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.fragment_userinfo.*
import me.dgmieth.kungfubbq.datatabase.room.Actions
import me.dgmieth.kungfubbq.datatabase.room.KungfuBBQRoomDatabase
import me.dgmieth.kungfubbq.datatabase.room.RoomViewModel
import me.dgmieth.kungfubbq.datatabase.roomEntities.CookingDateAndCookingDateDishesWithOrder
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

    private val MAPS_API_KEY = "google_maps_api_key"
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
                Log.d("ObservableTest", "value is $it")
                when(it){
                    Actions.UserComplete ->{
                        viewModel?.getUser()
                    }
                    else -> {
                        Handler(Looper.getMainLooper()).post{
                            loginSpinerLayout.visibility = View.INVISIBLE
                            Toast.makeText(requireActivity(),"The attemp to retrieve user information from the database failed",Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            {
                Log.d("ObservableTest", "error value is $it")
                Handler(Looper.getMainLooper()).post{
                    loginSpinerLayout.visibility = View.INVISIBLE
                    Toast.makeText(requireActivity(),"Log in attempt failed. Please try again in some minutes",Toast.LENGTH_LONG).show()
                }
            },{})?.let{ bag.add(it)}
        //Subscribing to user
        viewModel?.user?.observe(viewLifecycleOwner, Observer {
            Log.d(TAG, "user returned $it")
            viewModel?.getCookingDates()
            if(!it.user.email.isNullOrEmpty()){
                Log.d(TAG,"userUpdateOrder called -> value $it")
                userInfo = it
                userInfo?.let {
                    Log.d(TAG,"user refresh infor called")
                    var user = it
                    setUIElements(user)
                    showSpinner(false)
                    showSaveBtn(false)
                }
                Log.d(TAG, "user is $it")

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
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        userInfoCancelBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
        userInfoUpdatePasswordBtn.setOnClickListener {
            val action = UserInfoFragmentDirections.callUpdatePassword()
            findNavController().navigate(action)
        }
        userInfoCancelBtn.setOnClickListener {
            showSaveBtn(false)
        }
        userInfoSaveBtn.setOnClickListener {
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
    private fun showSaveBtn(value:Boolean){
        Handler(Looper.getMainLooper()).post {
            editItemBtn!!.isVisible = !value
            userInfoBtnGroup.isVisible = value
            userInfoUpdatePasswordBtn.isVisible = !value
            userInfoName.isEnabled = value
            userInfoPhone.isEnabled = value
            userInfoFacebookName.isEnabled = value
            userInfoInstagramName.isEnabled = value
            if(value){
                userInfoName.setBackgroundColor(Color.WHITE)
                userInfoPhone.setBackgroundColor(Color.WHITE)
                userInfoFacebookName.setBackgroundColor(Color.WHITE)
                userInfoInstagramName.setBackgroundColor(Color.WHITE)
            }else{
                userInfoName.setBackgroundColor(Color.TRANSPARENT)
                userInfoPhone.setBackgroundColor(Color.TRANSPARENT)
                userInfoFacebookName.setBackgroundColor(Color.TRANSPARENT)
                userInfoInstagramName.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }
    private fun validateInfo():Boolean{
        if(noFormatPhoneNbm==null){
            Handler(Looper.getMainLooper()).post{
                Toast.makeText(requireActivity(),"Incorrect phone number",Toast.LENGTH_LONG).show()
            }
            return false
        }else if(userInfoName.text.toString()==userInfo!!.user.name.toString() &&
            noFormatPhoneNbm==userInfo!!.user.phoneNumber.toString() &&
                userInfoFacebookName.text.toString()==userInfo!!.socialMedias[0].socialMediaName.toString() &&
            userInfoInstagramName.text.toString()==userInfo!!.socialMedias[1].socialMediaName.toString() ){
            Handler(Looper.getMainLooper()).post{
                Toast.makeText(requireActivity(),"No user information was changed",Toast.LENGTH_LONG).show()
            }
            return false
        }
        Log.d(TAG, "returning true")
        return true
    }
    private fun updateInfo(){
        val body = FormBody.Builder()
            .add("email",if(userInfoUserName.text.toString().isNullOrEmpty()) "none" else userInfoUserName.text.toString())
            .add("name",if(userInfoName.text.toString().isNullOrEmpty()) "none" else userInfoName.text.toString())
            .add("id",userInfo!!.user.userId.toString())
            .add("phoneNumber", if(noFormatPhoneNbm.isNullOrEmpty()) "none" else noFormatPhoneNbm!!)
            .add("facebookName", if(userInfoFacebookName.text.toString().isNullOrEmpty()) "none" else userInfoFacebookName.text.toString())
            .add("instagramName",if(userInfoInstagramName.text.toString().isNullOrEmpty()) "none" else userInfoInstagramName.text.toString())
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
                    for ((name, value) in response.headers) {
                        println("$name: $value")
                    }
                    val json = JSONObject(response.body!!.string())
                    Log.d(TAG, "update return response is $json")
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
                        println(json)
                        Handler(Looper.getMainLooper()).post{
                            Toast.makeText(requireActivity(),"The attempt to update your user information failed with server message: ${json.getString("msg")}",
                                Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        })
    }
    private fun formatPhoneNumber() {
        Log.d(TAG, "formatPhoneNumberCalled")
        if(userInfoPhone.text.toString().length==10){
            userInfoPhone.removeTextChangedListener(phoneTextWatcher)
            noFormatPhoneNbm = userInfoPhone.text.toString()
            var number = PhoneNumberUtils.formatNumber(userInfoPhone.text.toString(),"US")
            userInfoPhone.setText(number)
            userInfoPhone.setSelection(userInfoPhone.text.toString().length)
            userInfoPhone.addTextChangedListener(phoneTextWatcher)
            Log.d(TAG, "length==10 $number and ${noFormatPhoneNbm!!}")
        }else{
            var text = userInfoPhone.text.toString().replace("""[^0-9]""".toRegex(),"")
            noFormatPhoneNbm = null
            userInfoPhone.removeTextChangedListener(phoneTextWatcher)
            userInfoPhone.setText(text)
            userInfoPhone.setSelection(userInfoPhone.text.toString().length)
            userInfoPhone.addTextChangedListener(phoneTextWatcher)
            Log.d(TAG, "length==10 $text ")
        }
    }
    private fun setUIElements(user: UserAndSocialMedia){
        Log.d(TAG, "setUIElements callled")
        Handler(Looper.getMainLooper()).post {
            userInfoPhone.removeTextChangedListener(phoneTextWatcher)
            userInfoUserName.text = user.user.email
            userInfoMemberSince.text = user.user.memberSince
            userInfoName.setText(user.user.name)
            if(user.user.phoneNumber.toString().length==10){
                var number = PhoneNumberUtils.formatNumber(user.user.phoneNumber.toString(),"US")
                userInfoPhone.setText(number)
                noFormatPhoneNbm = user.user.phoneNumber.toString()
            }else{
                userInfoPhone.setText(user.user.phoneNumber)
            }
            userInfoFacebookName.setText(user.socialMedias[0].socialMediaName)
            userInfoInstagramName.setText(user.socialMedias[1].socialMediaName)
            userInfoPhone.addTextChangedListener(phoneTextWatcher)
        }
    }
    private fun showSpinner(value: Boolean){
        Handler(Looper.getMainLooper()).post {
            userInfoSpinnerLayout.visibility =  when(value){
                true -> View.VISIBLE
                else -> View.INVISIBLE
            }
        }
    }
}