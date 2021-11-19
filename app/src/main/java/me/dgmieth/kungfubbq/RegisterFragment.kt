package me.dgmieth.kungfubbq

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.fragment_register.*
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
                            registerSpinerLayout.visibility = View.INVISIBLE
                            val action = NavGraphDirections.callHome(true)
                            findNavController().navigate(action)
                        }
                    }
                    else -> {
                        Handler(Looper.getMainLooper()).post{
                            registerSpinerLayout.visibility = View.INVISIBLE
                            Toast.makeText(requireActivity(),"Register attempt failed. Please try again in some minutes",Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            {
                Handler(Looper.getMainLooper()).post{
                    loginSpinerLayout.visibility = View.INVISIBLE
                    Toast.makeText(requireActivity(),"Register attempt failed. Please try again in some minutes",Toast.LENGTH_LONG).show()
                }
            },{})?.let{ bag.add(it)}
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var dialogBuilder = AlertDialog.Builder(activity)
        setHasOptionsMenu(true)
        dialogBuilder.setMessage("In order to register with Kungfu BBQ you need to have an INVITATION CODE. If you don't have one, please message Kungfu BBQ requesting one. IMPORTANT: on the message, you MUST send the e-mail you want to create the account with.")
            .setCancelable(false)
            .setPositiveButton("ok", DialogInterface.OnClickListener{
                    _, _ ->    })
        val alert = dialogBuilder.create()
        alert.setTitle("Invitation code needed")
        alert.show()
        //setting click listeners
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
        showSpinner(true)
        if(!registerInvitationCodeEditText.text.toString().isNullOrEmpty() &&
                !registerConfirmPasswordEditText.text.toString().isNullOrEmpty() &&
                !registerPasswordEditText.text.toString().isNullOrEmpty() &&
                !registerEmailEditText.text.toString().isNullOrEmpty()){
            val body = FormBody.Builder()
                .add("code",registerInvitationCodeEditText.text.toString())
                .add("email",registerEmailEditText.text.toString())
                .add("password",registerPasswordEditText.text.toString())
                .add("confirmPassword",registerConfirmPasswordEditText.text.toString())
                .add("mobileOS","android")
                .build()
            HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/login/register",body)).enqueue(object :
                Callback {
                override fun onFailure(call: Call, e: IOException) {
                    showSpinner(false)
                    e.printStackTrace()
                    Handler(Looper.getMainLooper()).post{
                        Toast.makeText(requireActivity(),"The attempt to register this user failed with error message ${e.localizedMessage}",Toast.LENGTH_LONG).show()
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
                            viewModel?.insertAllUserInfo(user,socialM)
                        }else{
                            if(json.getInt("errorCode")==-3){
                                Handler(Looper.getMainLooper()).post{
                                    Toast.makeText(requireActivity(),"The attempt to register this user failed with server message: ${json.getString("msg")}",Toast.LENGTH_LONG).show()
                                    val action = RegisterFragmentDirections.returnToLoginFragment()
                                    findNavController().navigate(action)
                                }
                            }else{
                                Handler(Looper.getMainLooper()).post{
                                    Toast.makeText(requireActivity(),"The attempt to register this user failed with server message: ${json.getString("msg")}",Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
            })
        }else{
            Toast.makeText(requireActivity(),"You must inform your invitation code, your email, your password and confirm your password.", Toast.LENGTH_LONG).show()
        }
    }
    //===============================================
    //UI elements functions
    private fun showSpinner(value: Boolean){
        Handler(Looper.getMainLooper()).post {
            registerSpinerLayout.visibility =  when(value){
                true -> View.VISIBLE
                else -> View.INVISIBLE
            }
        }
    }
}