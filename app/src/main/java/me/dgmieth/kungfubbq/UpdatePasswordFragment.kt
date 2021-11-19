package me.dgmieth.kungfubbq

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_updatepassword.*
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserAndSocialMedia
import me.dgmieth.kungfubbq.httpCtrl.HttpCtrl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


class UpdatePasswordFragment : Fragment(R.layout.fragment_updatepassword) {

    private val TAG = "UpdatePasswordFragment"
    private val args : UpdatePasswordFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        //click listeners
        updatePasswordCancelBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
        updatePasswordSaveBtn.setOnClickListener {
            if(validateInfo()){
                showSpinner(true)
                updatePassword()
            }
        }
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
    //==================================
    // validation of editTexts
    private fun validateInfo():Boolean{
        val currentP = !updatePasswordCurrentPassword.text.toString().isNullOrEmpty() && updatePasswordCurrentPassword.text.toString().length==8
        val newP = !updatePasswordNewPassword.text.toString().isNullOrBlank() && updatePasswordNewPassword.text.toString().length==8
        val confirmP = !updatePasswordNewPasswordConfirmation.text.toString().isNullOrEmpty() && updatePasswordNewPasswordConfirmation.text.toString().length==8
        if(currentP&&newP&&confirmP){
            return true
        }
        Handler(Looper.getMainLooper()).post{
            Toast.makeText(requireActivity(),"You must inform your current password, choose a new password and confirm your new password. All passwords must be 8 alphanumerical characters with at least one uppercase letter.", Toast.LENGTH_LONG).show()
        }
        return false
    }
    private fun updatePassword(){
        val body = FormBody.Builder()
            .add("email",args.email)
            .add("id",args.id)
            .add("currentPassword",updatePasswordCurrentPassword.text.toString())
            .add("newPassword",updatePasswordNewPassword.text.toString())
            .add("confirmPassword", updatePasswordNewPasswordConfirmation.text.toString())
            .build()
        HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/api/user/changePassword",body,args.token)).enqueue(object :
            Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                showSpinner(false)
                Handler(Looper.getMainLooper()).post{
                    Toast.makeText(requireActivity(),"The attempt to update your passowrd failed failed with server message: ${e.localizedMessage}",
                        Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    showSpinner(false)
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val json = JSONObject(response.body!!.string())
                    if(!json.getBoolean("hasErrors")){
                        Handler(Looper.getMainLooper()).post{
                            Toast.makeText(requireActivity(),"${json.getString("msg")}",
                                Toast.LENGTH_LONG).show()
                            val action = UpdatePasswordFragmentDirections.callBackUserInfoFragment()
                            findNavController().navigate(action)
                        }
                    }else{
                        if(json.getInt("errorCode")==-1){
                            Handler(Looper.getMainLooper()).post{
                                Toast.makeText(requireActivity(),"${json.getString("msg")}",
                                    Toast.LENGTH_LONG).show()
                                val action = NavGraphDirections.callHome(false)
                                findNavController().navigate(action)
                            }
                        }else{
                            Handler(Looper.getMainLooper()).post{
                                Toast.makeText(requireActivity(),"The attempt to update your password failed with server message: ${json.getString("msg")}",
                                    Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        })
    }
    //==================================
    // ui elements
    private fun showSpinner(value: Boolean){
        Handler(Looper.getMainLooper()).post {
            updatePasswordSpinnerLayout.visibility =  when(value){
                true -> View.VISIBLE
                else -> View.INVISIBLE
            }
        }
    }
}