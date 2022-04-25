package me.dgmieth.kungfubbq

import android.annotation.SuppressLint
import me.dgmieth.kungfubbq.support.encryption.CryptLib
import androidx.appcompat.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.EditText
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.Observer
import io.reactivex.disposables.CompositeDisposable
import me.dgmieth.kungfubbq.datatabase.room.Actions
import me.dgmieth.kungfubbq.datatabase.room.KungfuBBQRoomDatabase
import me.dgmieth.kungfubbq.datatabase.room.RoomViewModel
import me.dgmieth.kungfubbq.datatabase.roomEntities.SocialMediaInfo
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserAndSocialMedia
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserDB
import me.dgmieth.kungfubbq.httpCtrl.HttpCtrl
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import com.onesignal.OneSignal
import me.dgmieth.kungfubbq.databinding.FragmentLoginBinding
import me.dgmieth.kungfubbq.datatabase.roomEntities.RememberMeInfo
import me.dgmieth.kungfubbq.support.extensions.onRightDrawableClicked


class LoginFragment : Fragment(R.layout.fragment_login) {

    private val TAG = "LoginFragment"

    private var viewModel: RoomViewModel? = null
    private var bag = CompositeDisposable()
    private var userId = 0

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //view model set up
        viewModel = ViewModelProviders.of(this).get(RoomViewModel::class.java)
        viewModel?.getDbInstance(KungfuBBQRoomDatabase.getInstance(requireActivity()))
        //rxjava observer
        viewModel?.returnMsg?.subscribe(
            {
                when (it) {
                    Actions.UserComplete -> {
                        Handler(Looper.getMainLooper()).post {
                            OneSignal.setExternalUserId(userId.toString())
                            binding.loginSpinerLayout.visibility = View.INVISIBLE
                            val action = NavGraphDirections.callHome(true)
                            findNavController().navigate(action)
                        }
                    }
                    Actions.RememberComplete -> {
                        Log.d(TAG,"rememberComplete")
                    }
                    else -> {
                    Log.d(TAG,"viewModeReturn is $it")
                        showAlert("Log in attempt failed. Please try again in some minutes","Log-in failed!")
                        Handler(Looper.getMainLooper()).post {
                            binding.loginSpinerLayout.visibility = View.INVISIBLE
                        }
                    }
                }
            },
            {
                showAlert("Log in attempt failed. Please try again in some minutes","Log-in failed!")
                Handler(Looper.getMainLooper()).post {
                    binding.loginSpinerLayout.visibility = View.INVISIBLE
                }
            }, {})?.let { bag.add(it) }
        viewModel?.rememberMe?.subscribe {
            if (it.remember == 1) {
                Log.d(TAG,"rememberMeInfo is $it")
                binding.loginPassword.setText(it.password)
                binding.rememberMe.isChecked = true
            }
        }
        viewModel?.getRememberMe()
        //android mutable live data observer
        viewModel?.user?.observe(viewLifecycleOwner, Observer {
            if (!it.user.email.isNullOrEmpty()) {
                returnUserFromDBSuccess(it)
            } else {
                returnUserFromDBNull()
            }
        })
        //db getUser information request
        viewModel?.getUser()
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        //setting button click observers
        binding.loginResetPassword.setOnClickListener{
            callResetPasswordAlert()
        }
        binding.loginRegisterBtn.setOnClickListener {
            val action = LoginFragmentDirections.callRegisterFragment()
            findNavController().navigate(action)
        }
//        binding.loginCancelBtn.setOnClickListener {
//            requireActivity().onBackPressed()
//        }
        binding.loginLoginBtn.setOnClickListener {
            logUserIn()
        }
        binding.loginUserEmail.onRightDrawableClicked {
            it.text.clear()
        }
        binding.loginPassword.setOnClickListener{
            binding.loginPassword.text.clear()
        }
        binding.rememberMe.setOnClickListener {
            Log.d(TAG, "switch is ${binding.rememberMe.isChecked}")
            viewModel?.deleteRememberMe()
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
    //HTTP REQUEST METHODS
    private fun logUserIn() {
        val plainText = "this is my plain text"
        val key = "your key"

        val cryptLib = CryptLib()

        val cipherText = cryptLib.encryptPlainTextWithRandomIV(binding.loginUserEmail.text.toString(), key)
        Log.d(TAG,"cipherText $cipherText")
        if(!binding.loginUserEmail.text.toString().isNullOrEmpty()&&!binding.loginPassword.text.toString().isNullOrEmpty()){
            showSpinner(true)
            if(binding.rememberMe.isChecked){
                var dtObj = RememberMeInfo(if(binding.rememberMe.isChecked) 1 else 0,binding.loginPassword.text.toString())
                viewModel?.insertRememberMe(dtObj)
            }
            val body = FormBody.Builder()
                .add("email",binding.loginUserEmail.text.toString())
                .add("password",binding.loginPassword.text.toString())
                .add("mobileOS","android")
                .add("version_code","${BuildConfig.VERSION_CODE}")
                .build()
            Log.d(TAG,body.toString())

            HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/login/login",body)).enqueue(object :
                Callback {
                override fun onFailure(call: Call, e: IOException) {
                    showSpinner(false)
                    e.printStackTrace()
                    showAlert("Log in attempt failed. Please try again in some minutes","Log-in failed!")
                }
                override fun onResponse(call: Call, response: Response) {
                    showSpinner(false)
                    response.use {
//                        if (!response.isSuccessful) {
//                            showAlert("Log in attempt failed. Please try again in some minutes","Log-in failed!")
//                        }
                        Log.d(TAG,"jsonREsponse is ${response.body!!}")
                        val json = JSONObject(response.body!!.string())
                        Log.d(TAG,"jsonREsponse is ${json}")
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
                            Log.d(TAG,"jsonREsponse is ${user}")
                            viewModel?.insertAllUserInfo(user,socialM)
                        }else{
                            showAlert("Log in attempt failed with server message: ${json.getString("msg").toString()}","Log-in failed!")
                        }
                    }
                }
            })
        }else{
            showAlert("You must inform your e-mail and your password","Log-in failed!")
        }
    }
    private fun recoverPasswordEmail(email:String){
        val body = FormBody.Builder()
            .add("email",email)
            .build()
        HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/api/user/forgotPassword",body)).enqueue(object :
            Callback {
            override fun onFailure(call: Call, e: IOException) {
                showSpinner(false)
                e.printStackTrace()
                showAlert("The attempt to recover password failed.","Password recovery failed!")
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    showSpinner(false)
                    if (!response.isSuccessful) {
                        showAlert("KungfuBBQ server is not online.","Password recovery failed!")
                    }
                    val json = JSONObject(response.body!!.string())
                    if(!json.getBoolean("hasErrors")){
                        USER_LOGGED=true
                        showAlert("Success! ${json.getString("msg")}","Success!")
                    }else{
                        showAlert("The attempt to recover your password failed with server message: ${json.getString("msg")}","Password recovery failed!")
                    }
                }
            }
        })
    }
    //========================================================
    //DATABASE RETURN CALLBACKS
    private fun returnUserFromDBNull() {
        Log.d(TAG, "no data returned")
    }
    private fun returnUserFromDBSuccess(it: UserAndSocialMedia) {
        binding.loginUserEmail.setText(it.user.email)
    }
    //========================================================
    //OTHER UI METHODS
    private fun callResetPasswordAlert(){
        var textField = EditText(activity)
        textField.hint = "johndoe@mail.com"
        textField.setPadding(16,16,8,0)
        textField.setBackgroundResource(android.R.color.transparent)
        textField.setHintTextColor(resources.getColor(R.color.textEditHint))
        textField.height = 135
        textField.setPadding(60,0,60,0)
        textField.textSize  = 22.0F

        var dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setMessage("Please inform you user account e-mail address and click on Send.")
            .setView(textField)
            .setCancelable(false)
            .setPositiveButton("Send", DialogInterface.OnClickListener{
                _, _ ->
                if(!textField.text.toString().isNullOrEmpty()){
                    showSpinner(true)
                    recoverPasswordEmail(textField.text.toString())
                }else{
                    showAlert("You must inform the e-mail","Password recovery failed!")
                }
            })
            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, _ -> print(dialog) })

        val alert = dialogBuilder.create()
        alert.setTitle("Password recovery")
        alert.show()
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
    private fun showSpinner(value: Boolean){
        Handler(Looper.getMainLooper()).post {
            binding.loginSpinerLayout.visibility =  when(value){
                true -> View.VISIBLE
                else -> View.INVISIBLE
            }
        }
    }
}