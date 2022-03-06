package me.dgmieth.kungfubbq

import androidx.appcompat.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneNumberUtils
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.onesignal.OneSignal
import io.reactivex.disposables.CompositeDisposable
import me.dgmieth.kungfubbq.databinding.FragmentRegisterBinding
import me.dgmieth.kungfubbq.datatabase.room.Actions
import me.dgmieth.kungfubbq.datatabase.room.KungfuBBQRoomDatabase
import me.dgmieth.kungfubbq.datatabase.room.RoomViewModel
import me.dgmieth.kungfubbq.datatabase.roomEntities.SocialMediaInfo
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserDB
import me.dgmieth.kungfubbq.httpCtrl.HttpCtrl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


class RegisterFragment : Fragment(R.layout.fragment_register) {

    private val TAG = "RegisterFragment"

    private var viewModel: RoomViewModel? = null
    private val bag = CompositeDisposable()
    private var userId = 0

    private var noFormatPhoneNbm : String? = null

    private val phoneTextWatcher = object: TextWatcher {
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            formatPhoneNumber()
        }
    }

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProviders.of(this).get(RoomViewModel::class.java)
        var db = KungfuBBQRoomDatabase.getInstance(requireActivity())
        viewModel?.getDbInstance(db)
        viewModel?.returnMsg?.subscribe(
            {
                when(it){
                    Actions.UserComplete ->{
                        Handler(Looper.getMainLooper()).post{
                            showSpinner(false)
                            OneSignal.setExternalUserId(userId.toString())
                            val action = NavGraphDirections.callHome(true)
                            findNavController().navigate(action)
                        }
                    }
                    else -> {
                        showAlert("Please try again in some minutes.","Register attempt failed!")
                    }
                }
            },
            {
                showAlert("Please try again in some minutes.","Register attempt failed!")
            },{})?.let{ bag.add(it)}
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        var dialogBuilder = AlertDialog.Builder(requireContext())
//        setHasOptionsMenu(true)
//        dialogBuilder.setMessage("In order to register with Kungfu BBQ you need to have an INVITATION CODE. If you don't have one, please message Kungfu BBQ requesting one. IMPORTANT: on the message, you MUST send the e-mail you want to create the account with.")
//            .setCancelable(false)
//            .setPositiveButton("ok", DialogInterface.OnClickListener{
//                    _, _ ->    })
//        enableHideSecondViewButtons(false)
//        val alert = dialogBuilder.create()
//        alert.setTitle("Invitation code needed")
//        alert.show()
        //setting click listeners
        binding.registerPhone.addTextChangedListener(phoneTextWatcher)
        binding.registerCancelBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
        binding.registerRegisterBtn.setOnClickListener {
            Log.d(TAG,"registerClicked")
            if(validateInfo()){
                registerUser()
            }
        }
        binding.registerBackBtn.setOnClickListener{
            binding.personalInfo.animate().withEndAction{
                enableHideSecondViewButtons(false)
                binding.personalInfo.visibility = View.INVISIBLE
            }.alpha(0f).duration = 300
        }
        binding.registerNextBtn.setOnClickListener{
            Log.d(TAG, "nextClick")
            if(/*!binding.registerInvitationCodeEditText.text.toString().isNullOrEmpty() &&*/
                !binding.registerConfirmPasswordEditText.text.toString().isNullOrEmpty() &&
                !binding.registerPasswordEditText.text.toString().isNullOrEmpty() &&
                !binding.registerEmailEditText.text.toString().isNullOrEmpty()){
                binding.personalInfo.visibility = View.VISIBLE
                binding.personalInfo.animate().alpha(1f).duration = 300
                enableHideSecondViewButtons(true)
            }else{
                showSpinner(false)
                showAlert("You must inform your email, your password and confirm your password.","Register attempt failed!")
            }

        }
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
    override fun onDestroy() {
        super.onDestroy()
        bag.clear()
        bag.dispose()
    }
    //========================================================
    //EVENT LISTENER FOR BUTTONS
    private fun registerUser() {
        var face = if(binding.registerFacebook.text.toString().isNullOrEmpty()) "none" else binding.registerFacebook.text.toString()
        var inst = if(binding.registerInstagram.text.toString().isNullOrEmpty()) "none" else binding.registerInstagram.text.toString()

        showSpinner(true)
        if(!binding.registerName.text.toString().isNullOrEmpty() &&
                !binding.registerPhone.text.toString().isNullOrEmpty() && noFormatPhoneNbm != null){
            val body = FormBody.Builder()
                .add("code",/*binding.registerInvitationCodeEditText.text.toString()*/"none")
                .add("email",binding.registerEmailEditText.text.toString())
                .add("password",binding.registerPasswordEditText.text.toString())
                .add("confirmPassword",binding.registerConfirmPasswordEditText.text.toString())
                .add("phoneNumber",noFormatPhoneNbm.toString())
                .add("name",binding.registerName.text.toString())
                .add("facebookName",face)
                .add("instagramName",inst)
                .add("mobileOS","android")
                .add("version_code","${BuildConfig.VERSION_CODE}")
                .build()
            HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/login/register",body)).enqueue(object :
                Callback {
                override fun onFailure(call: Call, e: IOException) {
                    showSpinner(false)
                    e.printStackTrace()
                    showAlert("The attempt to register this user failed with error message ${e.localizedMessage}","Register attempt failed!")
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
                            userId = user.userId
                            viewModel?.insertAllUserInfo(user,socialM)
                        }else{
                            showAlert("The attempt to register this user failed with server message: ${json.getString("msg")}","Register attempt failed!")
//                            if(json.getInt("errorCode")==-3){
//                                Handler(Looper.getMainLooper()).post{
//                                    Toast.makeText(requireActivity(),"The attempt to register this user failed with server message: ${json.getString("msg")}",Toast.LENGTH_LONG).show()
//                                    val action = RegisterFragmentDirections.returnToLoginFragment()
//                                    findNavController().navigate(action)
//                                }
//                            }else{
//                                Handler(Looper.getMainLooper()).post{
//                                    Toast.makeText(requireActivity(),"The attempt to register this user failed with server message: ${json.getString("msg")}",Toast.LENGTH_LONG).show()
//                                }
//                            }
                        }
                    }
                }
            })
        }else{
            showSpinner(false)
            showAlert("You must inform your name and a valid phone number","Register attempt failed!")
        }
    }
    //===============================================
    //UI elements functions
    private fun showSpinner(value: Boolean){
        Handler(Looper.getMainLooper()).post {
            binding.registerSpinerLayout.visibility =  when(value){
                true -> View.VISIBLE
                else -> View.INVISIBLE
            }
        }
    }
    private fun formatPhoneNumber() {
        if(binding.registerPhone.text.toString().length==10){
            binding.registerPhone.removeTextChangedListener(phoneTextWatcher)
            noFormatPhoneNbm = binding.registerPhone.text.toString()
            var number = binding.registerPhone.text.toString()
            var formattedNumber = "(${number.subSequence(0,3)}) ${number.subSequence(3,6)}-${number.subSequence(6,number.length)}"
            binding.registerPhone.setText(formattedNumber)
            binding.registerPhone.setSelection(binding.registerPhone.text.toString().length)
            binding.registerPhone.addTextChangedListener(phoneTextWatcher)
        }else{
            var text = binding.registerPhone.text.toString().replace("""[^0-9]""".toRegex(),"")
            noFormatPhoneNbm = null
            binding.registerPhone.removeTextChangedListener(phoneTextWatcher)
            binding.registerPhone.setText(text)
            binding.registerPhone.setSelection(binding.registerPhone.text.toString().length)
            binding.registerPhone.addTextChangedListener(phoneTextWatcher)
        }
    }
    private fun validateInfo():Boolean{
        when {
            binding.registerName.text.toString().isEmpty() -> {
                showAlert("You must inform your name.","Register attempt failed!")
                return false
            }
            binding.registerPhone.text.toString().isEmpty() -> {
                showAlert("You must inform a valid phone number.","Register attempt failed!")
                return false
            }
            noFormatPhoneNbm==null -> {
                showAlert("You must inform a valid phone number.","Register attempt failed!")
                return false
            }
            else -> return true
        }
    }
    private fun showAlert(message:String,title:String){
        Handler(Looper.getMainLooper()).post{
            var dialogBuilder = AlertDialog.Builder(requireContext())
            dialogBuilder.setMessage(message)
                .setCancelable(title != "${getString(R.string.not_logged_in)}")
                .setPositiveButton("Ok", DialogInterface.OnClickListener{
                        _, _ ->
                    if(title=="${getString(R.string.not_logged_in)}"){
                        USER_LOGGED = false
                        val action = NavGraphDirections.callHome(false)
                        findNavController().navigate(action)
                    }
                })
            val alert = dialogBuilder.create()
            alert.setTitle(title)
            alert.show()
        }
    }
    private fun enableHideSecondViewButtons(enable:Boolean){
        binding.registerBackBtn.isEnabled = enable
        binding.registerRegisterBtn.isEnabled = enable
    }
}