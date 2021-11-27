package me.dgmieth.kungfubbq

import androidx.appcompat.app.AlertDialog
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
                        Handler(Looper.getMainLooper()).post{
                            showSpinner(false)
                            Toast.makeText(requireActivity(),"Register attempt failed. Please try again in some minutes",Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            {
                Handler(Looper.getMainLooper()).post{
                    showSpinner(false)
                    Toast.makeText(requireActivity(),"Register attempt failed. Please try again in some minutes",Toast.LENGTH_LONG).show()
                }
            },{})?.let{ bag.add(it)}
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var dialogBuilder = AlertDialog.Builder(requireContext())
        setHasOptionsMenu(true)
        dialogBuilder.setMessage("In order to register with Kungfu BBQ you need to have an INVITATION CODE. If you don't have one, please message Kungfu BBQ requesting one. IMPORTANT: on the message, you MUST send the e-mail you want to create the account with.")
            .setCancelable(false)
            .setPositiveButton("ok", DialogInterface.OnClickListener{
                    _, _ ->    })
        val alert = dialogBuilder.create()
        alert.setTitle("Invitation code needed")
        alert.show()
        //setting click listeners
        binding.registerCancelBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
        binding.registerRegisterBtn.setOnClickListener {
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
        if(!binding.registerInvitationCodeEditText.text.toString().isNullOrEmpty() &&
                !binding.registerConfirmPasswordEditText.text.toString().isNullOrEmpty() &&
                !binding.registerPasswordEditText.text.toString().isNullOrEmpty() &&
                !binding.registerEmailEditText.text.toString().isNullOrEmpty()){
            val body = FormBody.Builder()
                .add("code",binding.registerInvitationCodeEditText.text.toString())
                .add("email",binding.registerEmailEditText.text.toString())
                .add("password",binding.registerPasswordEditText.text.toString())
                .add("confirmPassword",binding.registerConfirmPasswordEditText.text.toString())
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
                            userId = user.userId
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
            binding.registerSpinerLayout.visibility =  when(value){
                true -> View.VISIBLE
                else -> View.INVISIBLE
            }
        }
    }
}