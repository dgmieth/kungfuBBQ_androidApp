package me.dgmieth.kungfubbq

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.fragment_register.*
import me.dgmieth.kungfubbq.datatabase.room.KungfuBBQRoomDatabase
import me.dgmieth.kungfubbq.datatabase.room.RoomViewModel
import me.dgmieth.kungfubbq.datatabase.roomEntities.SocialMediaInfo
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserAndSocialMedia
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserDB
import me.dgmieth.kungfubbq.httpCtrl.HttpCtrl
import me.dgmieth.kungfubbq.httpRequets.Endpoints
import me.dgmieth.kungfubbq.httpRequets.HttpRequestCtrl
import me.dgmieth.kungfubbq.httpRequets.RegisterBodyData
import me.dgmieth.kungfubbq.httpRequets.responsesObjects.userValidation.LoggedUserInfo
import me.dgmieth.kungfubbq.httpRequets.responsesObjects.userValidation.RegisteredUserInfo
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
    private var loggedUserInfo : RegisteredUserInfo? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProviders.of(this).get(RoomViewModel::class.java)
        var db = KungfuBBQRoomDatabase.getInstance(requireActivity())
        viewModel?.getDbInstance(db)
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var dialogBuilder = AlertDialog.Builder(activity)
        setHasOptionsMenu(true)
        dialogBuilder.setMessage("In order to register with Kungfu BBQ you need to have an INVITATION CODE. If you don't have one, please message Kungfu BBQ requesting one. IMPORTANT: on the message, you MUST send the e-mail you want to create the account with.")
            .setCancelable(false)
            .setPositiveButton("ok", DialogInterface.OnClickListener{
                    dialog, id ->
            })
        val alert = dialogBuilder.create()
        alert.setTitle("Invitation code needed")
        alert.show()

        registerCancelBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
        registerRegisterBtn.setOnClickListener {
            registerUser()
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
        registerSpinerLayout.visibility = View.VISIBLE
        if(!registerInvitationCodeEditText.text.toString().isNullOrEmpty() &&
                !registerConfirmPasswordEditText.text.toString().isNullOrEmpty() &&
                !registerPasswordEditText.text.toString().isNullOrEmpty() &&
                !registerEmailEditText.text.toString().isNullOrEmpty()){
            val body = FormBody.Builder()
                .add("code",registerInvitationCodeEditText.text.toString())
                .add("email",registerEmailEditText.text.toString())
                .add("password",registerPasswordEditText.text.toString())
                .add("confirmPassword",registerConfirmPasswordEditText.text.toString())
                .build()
            HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/login/register",body)).enqueue(object :
                Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    Handler(Looper.getMainLooper()).post{
                        Toast.makeText(requireActivity(),"The attempt to register this user failed with error message ${e.localizedMessage}",Toast.LENGTH_LONG).show()
                    }
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        registerSpinerLayout.visibility = View.INVISIBLE
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        for ((name, value) in response.headers) {
                            println("$name: $value")
                        }
                        val json = JSONObject(response.body!!.string())
                        if(!json.getBoolean("hasErrors")){
                            println(json)
                            Handler(Looper.getMainLooper()).post{
                                val action = NavGraphDirections.callHome(true)
                                findNavController().navigate(action)
                            }
                        }else{
                            println(json)
                            Handler(Looper.getMainLooper()).post{
                                Toast.makeText(requireActivity(),"The attempt to register this user failed with server message: ${json.getString("msg")}",Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            })
        }else{
            Toast.makeText(requireActivity(),"You must inform your invitation code, your email, your password and confirm your password.", Toast.LENGTH_LONG).show()
        }
    }
    //========================================================
    //HTTP REQUEST CALLBACKS
    private fun registerFailed(t: Throwable) {
        Toast.makeText(requireActivity(),"The attempt to register the user failed with message ${t.localizedMessage}.", Toast.LENGTH_LONG).show()
    }
    private fun registerSuccessful(it: RegisteredUserInfo) {
        if (!it.hasErros) {
            loggedUserInfo = it
//            viewModel?.deleteAllUserInfo()
//            viewModel?.deleteAllSocialMediaInfo()

            val u = it.msg
            val sm = u!!.socialMediaInfo
            var user = UserDB(u.id, u.email, u.memberSince, u.name, u.phoneNumber, u.token, 1)
            var socialM: MutableList<SocialMediaInfo> = arrayListOf()
            for (i in sm) {
                var sMInfo = SocialMediaInfo(i.socialMedia, i.sociaMediaName, u.id)
                socialM.add(sMInfo)
            }
//            viewModel?.insertUserInfo(user)
//            viewModel?.insertSocialMediaInfo(socialM)
            viewModel?.insertAllUserInfo(user,socialM)
            val action = NavGraphDirections.callHome(true)
            findNavController().navigate(action)
        }
    }
    //========================================================
    //DATABASE RETURN CALLBACKS
//    private fun returnUserFromDBNull() {
//        Log.d(TAG, "no data returned")
//    }
//
//    private fun returnUserFromDBSuccess(it: UserAndSocialMedia) {
//        Log.d(TAG, "dataReturn was ${it.user.email} and ${it.user.userId}")
//        loginUserEmail.setText(it.user.email)
//    }
}