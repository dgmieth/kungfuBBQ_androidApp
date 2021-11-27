package me.dgmieth.kungfubbq

import androidx.appcompat.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.marginLeft
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
                    else -> {
                        Handler(Looper.getMainLooper()).post {
                            binding.loginSpinerLayout.visibility = View.INVISIBLE
                            Toast.makeText(
                                requireActivity(),
                                "Log in attempt failed. Please try again in some minutes",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            },
            {
                Handler(Looper.getMainLooper()).post {
                    binding.loginSpinerLayout.visibility = View.INVISIBLE
                    Toast.makeText(
                        requireActivity(),
                        "Log in attempt failed. Please try again in some minutes",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }, {})?.let { bag.add(it) }
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
        binding.loginCancelBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
        binding.loginLoginBtn.setOnClickListener {
            logUserIn()
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
        if(!binding.loginUserEmail.text.toString().isNullOrEmpty()&&!binding.loginPassword.text.toString().isNullOrEmpty()){
            showSpinner(true)
            val body = FormBody.Builder()
                .add("email",binding.loginUserEmail.text.toString())
                .add("password",binding.loginPassword.text.toString())
                .add("mobileOS","android")
                .build()
            println(body)
            HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/login/login",body)).enqueue(object :
                Callback {
                override fun onFailure(call: Call, e: IOException) {
                    showSpinner(false)
                    e.printStackTrace()
                    Handler(Looper.getMainLooper()).post{
                        Toast.makeText(requireActivity(),"Log in attempt failed with error message: ${e.localizedMessage}",Toast.LENGTH_LONG).show()
                    }
                }
                override fun onResponse(call: Call, response: Response) {
                    showSpinner(false)
                    response.use {
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
                           Handler(Looper.getMainLooper()).post{
                               binding.loginSpinerLayout.visibility = View.INVISIBLE
                                Toast.makeText(requireActivity(),"Log in attempt failed with server message: ${json.getString("msg").toString()}",Toast.LENGTH_LONG).show()
                           }
                        }
                    }
                }
            })
        }else{
            Toast.makeText(requireActivity(),"You must inform your e-mail and your password",Toast.LENGTH_LONG).show()
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
                Handler(Looper.getMainLooper()).post{
                    Toast.makeText(requireActivity(),"The attempt to recover password failed.",Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    showSpinner(false)
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val json = JSONObject(response.body!!.string())
                    if(!json.getBoolean("hasErrors")){
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(requireActivity(),"Success! ${json.getString("msg")}",Toast.LENGTH_LONG).show()
                        }
                    }else{
                        Handler(Looper.getMainLooper()).post{
                            Toast.makeText(requireActivity(),"The attempt to recover your password failed",Toast.LENGTH_LONG).show()
                        }
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
                    Toast.makeText(requireActivity(),"You must inform the e-mail",Toast.LENGTH_LONG).show()
                }
            })
            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, _ -> print(dialog) })

        val alert = dialogBuilder.create()
        alert.setTitle("Password recovery")
        alert.show()
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