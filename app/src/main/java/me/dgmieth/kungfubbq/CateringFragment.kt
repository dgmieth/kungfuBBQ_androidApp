package me.dgmieth.kungfubbq

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import me.dgmieth.kungfubbq.databinding.FragmentCateringBinding
import me.dgmieth.kungfubbq.httpCtrl.HttpCtrl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class CateringFragment : Fragment(R.layout.fragment_catering) {

    private val args : CateringFragmentArgs by navArgs()
    private val TAG = "CateringFragment"
    private var noFormatPhoneNbm : String? = null
    private val phoneTextWatcher = object: TextWatcher{
        override fun afterTextChanged(s: Editable?) {   }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {        }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            formatPhoneNumber()
        }
    }

    private var _binding: FragmentCateringBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCateringBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        //click listeners
        binding.cateringCancelBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
        binding.cateringSendBtn.setOnClickListener {
            sendMessage()
        }
        binding.cateringPhone.addTextChangedListener(phoneTextWatcher)
    }
    //===============================================================
    // ui interface
    private fun formatPhoneNumber() {
        if(binding.cateringPhone.text.toString().length==10){
            binding.cateringPhone.removeTextChangedListener(phoneTextWatcher)
            noFormatPhoneNbm = binding.cateringPhone.text.toString()
            var number = binding.cateringPhone.text.toString()
            var formattedNumber = "(${number.subSequence(0,3)}) ${number.subSequence(3,6)}-${number.subSequence(6,number.length)}"
            binding.cateringPhone.setText(formattedNumber)
            binding.cateringPhone.setSelection(binding.cateringPhone.text.toString().length)
            binding.cateringPhone.addTextChangedListener(phoneTextWatcher)
        }else{
            var text = binding.cateringPhone.text.toString()
            binding.cateringPhone.removeTextChangedListener(phoneTextWatcher)
            binding.cateringPhone.setText(text)
            noFormatPhoneNbm = null
            binding.cateringPhone.setSelection(binding.cateringPhone.text.toString().length)
            binding.cateringPhone.addTextChangedListener(phoneTextWatcher)
        }
    }
    private fun showSpinner(value: Boolean){
        Handler(Looper.getMainLooper()).post {
            binding.cateringSpinerLayout.visibility =  when(value){
                true -> View.VISIBLE
                else -> View.INVISIBLE
            }
        }
    }
    //===============================================================
    // http request
    private fun sendMessage(){
        if(!validateInformation()){
            return
        }
        if(!binding.cateringEmail.text.toString().isNullOrEmpty() && !binding.cateringName.text.toString().isNullOrEmpty()&&
                !binding.cateringPhone.text.toString().isNullOrEmpty()&&!binding.cateringDescription.text.toString().isNullOrEmpty()){
            showSpinner(true)
            val body = FormBody.Builder()
                .add("email",binding.cateringEmail.text.toString())
                .add("name",binding.cateringName.text.toString())
                .add("phoneNumber",binding.cateringPhone.text.toString().replace("""[^0-9]""".toRegex(),""))
                .add("orderDescription",binding.cateringDescription.text.toString())
                .build()
            HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/api/catoring/saveContact",body)).enqueue(object :
                Callback {
                override fun onFailure(call: Call, e: IOException) {
                    showSpinner(false)
                    e.printStackTrace()
                    showAlert("Sending your message failed with error message: ${e.localizedMessage}","Error!")
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        showSpinner(false)
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        val json = JSONObject(response.body!!.string())
                        if(!json.getBoolean("hasErrors")){
                            showAlert("${json.getString("msg")}","Success!")
                        }else{
                            showAlert("Sending your message failed with server message: ${json.getString("msg")}","Error!")
                        }
                    }
                }
            })
        }else{
            showAlert("You must inform your name, a valid e-email, phone number and describe what you need in order to send a catering request.","Error!")
        }
    }
    //=======================================
    private fun showAlert(message:String,title:String){
        Handler(Looper.getMainLooper()).post{
            var dialogBuilder = AlertDialog.Builder(requireContext())
            dialogBuilder.setMessage(message)
                .setCancelable(title != "${getString(R.string.not_logged_in)}")
                .setPositiveButton("Ok", DialogInterface.OnClickListener{
                        _, _ ->
                    if(title=="Success!"){
                        val action = NavGraphDirections.callHome(args.homeLoggedArgument)
                        findNavController().navigate(action)
                    }
                })
            val alert = dialogBuilder.create()
            alert.setTitle(title)
            alert.show()
        }
    }
    private fun validateInformation():Boolean{
        return when {
            binding.cateringName.text.toString().isEmpty() -> {
                showAlert("You must inform your name.","Error!")
                false
            }
            !binding.cateringEmail.text.toString().matches("""^(?=.*[@])(?=.*[.])(\S*@\S*\.\s*\.?\S*)""".toRegex()) -> {
                showAlert("You must inform a valid e-mail address.","Error!")
                false
            }
            binding.cateringPhone.text.toString().isEmpty() -> {
                showAlert("You must inform a valid phone number.","Error!")
                false
            }
            noFormatPhoneNbm==null -> {
                showAlert("You must inform a valid phone number.","Error!")
                false
            }
            else -> true
        }
    }
}