package me.dgmieth.kungfubbq

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import me.dgmieth.kungfubbq.databinding.FragmentUpdatepasswordBinding
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

    private var _binding: FragmentUpdatepasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUpdatepasswordBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        //click listeners
        binding.updatePasswordCancelBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
        binding.updatePasswordSaveBtn.setOnClickListener {
            Log.d(TAG,"saveCliked")
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
        val currentP = !binding.updatePasswordCurrentPassword.text.toString().isNullOrEmpty() && checkLength(binding.updatePasswordCurrentPassword.text.toString())
        val newP = !binding.updatePasswordNewPassword.text.toString().isNullOrBlank() && checkLength(binding.updatePasswordNewPassword.text.toString())
        val confirmP = !binding.updatePasswordNewPasswordConfirmation.text.toString().isNullOrEmpty() && checkLength(binding.updatePasswordNewPasswordConfirmation.text.toString())
        if(currentP&&newP&&confirmP){
            return true
        }
        showAlert("You must inform your current password, choose a new password and confirm your new password. All passwords must be 3-20 characters long and must include at least one UPPER case, one lower case and one number.","Update failed!")
        return false
    }
    private fun updatePassword(){
        val body = FormBody.Builder()
            .add("email",args.email)
            .add("id",args.id)
            .add("currentPassword",binding.updatePasswordCurrentPassword.text.toString())
            .add("newPassword",binding.updatePasswordNewPassword.text.toString())
            .add("confirmPassword", binding.updatePasswordNewPasswordConfirmation.text.toString())
            .build()
        HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/api/user/changePassword",body,args.token)).enqueue(object :
            Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                showSpinner(false)
                showAlert("The attempt to update your password failed failed with server message: ${e.localizedMessage}","Update failed!")
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    showSpinner(false)
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val json = JSONObject(response.body!!.string())
                    if(!json.getBoolean("hasErrors")){
                        showAlert("${json.getString("msg")}","Update successful!")
                    }else{
                        if(json.getInt("errorCode")==-1){
                            showAlert("${getString(R.string.not_logged_in_message)}","${getString(R.string.not_logged_in)}")
                        }else{
                            showAlert("The attempt to update your password failed with server message: ${json.getString("msg")}","Update failed!")
                        }
                    }
                }
            }
        })
    }
    //==================================
    //support funcionts
    private fun checkLength(text:String):Boolean{
        if(text.length in 3..20){
            return true
        }
        return false
    }
    //==================================
    // ui elements
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
                    if(title=="Update successful!"){
                        val action = UpdatePasswordFragmentDirections.callBackUserInfoFragment()
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
            binding.updatePasswordSpinnerLayout.visibility =  when(value){
                true -> View.VISIBLE
                else -> View.INVISIBLE
            }
        }
    }
}