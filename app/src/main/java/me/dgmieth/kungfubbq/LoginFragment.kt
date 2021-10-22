package me.dgmieth.kungfubbq

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_login.*
import me.dgmieth.kungfubbq.datatabase.room.KungfuBBQRoomDatabase
import me.dgmieth.kungfubbq.datatabase.room.RoomViewModel
import me.dgmieth.kungfubbq.datatabase.roomEntities.SocialMediaInfo
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserAndSocialMedia
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserDB
import me.dgmieth.kungfubbq.httpCtrl.HttpCtrl
import me.dgmieth.kungfubbq.httpRequets.Endpoints
import me.dgmieth.kungfubbq.httpRequets.ForgotPasswordBodyData
import me.dgmieth.kungfubbq.httpRequets.HttpRequestCtrl
import me.dgmieth.kungfubbq.httpRequets.LoginBodyData
import me.dgmieth.kungfubbq.httpRequets.responsesObjects.userValidation.ForgotPasswordResponseValidation
import me.dgmieth.kungfubbq.httpRequets.responsesObjects.userValidation.LoggedUserInfo
import okhttp3.*
import org.json.JSONObject
import java.io.IOException


class LoginFragment : Fragment(R.layout.fragment_login) {

    private val TAG = "LoginFragment"

    private var viewModel: RoomViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProviders.of(this).get(RoomViewModel::class.java)
        var db = KungfuBBQRoomDatabase.getInstance(requireActivity())
        viewModel?.getDbInstance(db)
        viewModel?.user?.observe(viewLifecycleOwner, Observer {
            if(!it.user.email.isNullOrEmpty()){
                returnUserFromDBSuccess(it)
            }else{
                returnUserFromDBNull()
            }
        })
        viewModel?.getUser()
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        loginResetPassword.setOnClickListener{
            println("clicked")
            callResetPasswordAlert()
        }
        loginRegisterBtn.setOnClickListener {
            val action = LoginFragmentDirections.callRegisterFragment()
            findNavController().navigate(action)
        }
        loginCancelBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
        loginLoginBtn.setOnClickListener {
            logUserIn()
        }
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
    //========================================================
    //HTTP REQUEST METHODS
    private fun logUserIn() {
        if(!loginUserEmail.text.toString().isNullOrEmpty()&&!loginPassword.text.toString().isNullOrEmpty()){
            loginSpinerLayout.visibility = View.VISIBLE
            val newUserAuth = LoginBodyData(loginPassword.text.toString(),loginUserEmail.text.toString())
            Log.d("HttpRequestCtrl", "jsonObject is $newUserAuth")
            val body = FormBody.Builder()
                .add("email",loginUserEmail.text.toString())
                .add("password",loginPassword.text.toString())
                .build()
            println(body)
            HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/login/login",body)).enqueue(object :
                Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    Handler(Looper.getMainLooper()).post{
                        Toast.makeText(requireActivity(),"Log in attempt failed with error message: ${e.localizedMessage}",Toast.LENGTH_LONG).show()
                    }
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        loginSpinerLayout.visibility = View.INVISIBLE
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        for ((name, value) in response.headers) {
                            println("$name: $value")
                        }
                        val json = JSONObject(response.body!!.string())
                        if(!json.getBoolean("hasErrors")){
                            println(json)
                            val u = json.getJSONObject("data")
                            val user = UserDB(u.getInt("id"),u.getString("email"),u.getString("memberSince"),u.getString("name"),u.getString("phoneNumber"),u.getString("token"),1)
                            var socialM : MutableList<SocialMediaInfo> = arrayListOf()
                            for (i in 0 until u.getJSONArray("socialMediaInfo").length()){
                                val s = u.getJSONArray("socialMediaInfo").getJSONObject(0)
                                var sMInfo = SocialMediaInfo(s.getString("socialMedia"),s.getString("socialMediaName"),u.getInt("id"))
                                socialM.add(sMInfo)
                            }
                            loginSuccess(user,socialM)
                        }else{
                            println(json.getString("msg"))
                           Handler(Looper.getMainLooper()).post{
                                Toast.makeText(requireActivity(),"Log in attempt failed with server message: ${json.getString("msg").toString()}",Toast.LENGTH_LONG).show()
                           }
                        }
                    }
                }
            })
        }else{
            Log.d(TAG, "loginButton not validated")
            Toast.makeText(requireActivity(),"You must inform your e-mail and your password",Toast.LENGTH_LONG).show()
        }
    }
    private fun recoverPasswordEmail(email:String){
        val body = FormBody.Builder()
            .add("email",email)
            .build()
        println(body)
        HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/api/user/forgotPassword",body)).enqueue(object :
            Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post{
                    Toast.makeText(requireActivity(),"The attempt to recover password failed.",Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    loginSpinerLayout.visibility = View.INVISIBLE
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    for ((name, value) in response.headers) {
                        println("$name: $value")
                    }
                    val json = JSONObject(response.body!!.string())
                    println(json)
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
        Log.d(TAG, "dataReturn was ${it.user.email} and ${it.user.userId}")
        loginUserEmail.setText(it.user.email)
    }
    //========================================================
    //HTTP RETURN CALLBACKS
    private fun loginSuccess(user: UserDB, socialMediaInfo: MutableList<SocialMediaInfo>) {
        loginSpinerLayout.visibility = View.INVISIBLE
                viewModel?.deleteAllUserInfo()
                viewModel?.deleteAllSocialMediaInfo()
                viewModel?.insertUserInfo(user)
                viewModel?.insertSocialMediaInfo(socialMediaInfo)
            Handler(Looper.getMainLooper()).post{
                val action = NavGraphDirections.callHome(true)
                findNavController().navigate(action)
            }
    }
    //OTHER UI METHODS
    private fun callResetPasswordAlert(){
        var textField = EditText(activity)
        textField.hint = "johndoe@mail.com"
        textField.setPadding(16,16,8,0)

        textField.setBackgroundResource(android.R.color.transparent)
        var dialogBuilder = AlertDialog.Builder(activity)
        dialogBuilder.setMessage("Please inform you user account e-mail address and click on Send.")
            .setView(textField)
            .setCancelable(false)
            .setPositiveButton("Send", DialogInterface.OnClickListener{
                dialog, id ->
                print(dialog)
                print(id)
                Log.d(TAG, "ForgotPasswordAlert with $dialog $id ${textField.text.toString()}")
                if(!textField.text.toString().isNullOrEmpty()){
                    loginSpinerLayout.visibility = View.VISIBLE
                    recoverPasswordEmail(textField.text.toString())
                }else{
                    Toast.makeText(requireActivity(),"You must inform the e-mail",Toast.LENGTH_LONG).show()
                }
            })
            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> print(dialog) })
        val alert = dialogBuilder.create()
        alert.setTitle("Password recovery")
        alert.show()
    }


}