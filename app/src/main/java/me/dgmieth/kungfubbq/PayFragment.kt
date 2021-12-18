package me.dgmieth.kungfubbq

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import androidx.appcompat.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import me.dgmieth.kungfubbq.databinding.FragmentPayBinding
import me.dgmieth.kungfubbq.httpCtrl.HttpCtrl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class PayFragment : Fragment(R.layout.fragment_pay) {

    private val TAG = "PayFragment"
    private val args : PayFragmentArgs by navArgs()
    private var cardNumber : String? = null
    private var cardCode : String? = null
    private var btnClick = true
    var yearsFirst = arrayListOf<String>()
    private val cardNumberWatcher = object: TextWatcher {
        override fun afterTextChanged(s: Editable?) { }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            formatPhoneNumber()
        }
    }
    private val codeNumberWatcher = object: TextWatcher {
        override fun afterTextChanged(s: Editable?) { }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if(binding.payCardCode.text.toString().length>=3){
                cardCode = binding.payCardCode.text.toString()
                return
            }
            cardCode = null
        }
    }

    private var _binding: FragmentPayBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if(args.coookingDateId == 0 && args.orderId ==0 && args.userEmail == "noValue" && args.userId == 0 && args.userToken == "noValue") {
            var dialogBuilder = AlertDialog.Builder(requireContext())
            dialogBuilder.setMessage("Communication with this apps's database failed. Please restart the app.")
                .setCancelable(false)
                .setPositiveButton("Ok", DialogInterface.OnClickListener{
                        _, _ ->
                    Handler(Looper.getMainLooper()).post {
                        var action = CalendarFragmentDirections.callCalendarFragmentGlobal()
                        findNavController().navigate(action)
                    }
                })
            val alert = dialogBuilder.create()
            alert.setTitle("Database communication failure")
            alert.show()
        }
        _binding = FragmentPayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val today = Date()
        val dateFormatter = SimpleDateFormat()
        dateFormatter.applyPattern("y")
        val year = (dateFormatter.format(today).toString()).toInt()
        yearsFirst.add("Year")
        for(i in 0..20){
            yearsFirst.add("${year+i}")
        }
        val years = yearsFirst.toTypedArray()
        val months = arrayOf("Month","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Set","Oct","Nov","Dec")
        binding.payCardYear.minValue = 0
        binding.payCardYear.maxValue = 20
        binding.payCardYear.displayedValues = years
        binding.payCardMonth.minValue = 0
        binding.payCardMonth.maxValue = months.size -1
        binding.payCardMonth.displayedValues = months
        binding.payCardYear.setOnValueChangedListener{_,_,_ ->
        }
        binding.payCardMonth.setOnValueChangedListener{_,_,_ ->
        }
        binding.payCancelBtn.setOnClickListener {
            val action = PayOrderFragmentDirections.callPayOrderFragmentGlobal(args.coookingDateId)
            findNavController().navigate(action)
        }
        binding.payPayBtn.setOnClickListener {
            payOrder()
        }
        binding.payCardNumber.addTextChangedListener(cardNumberWatcher)
        binding.payCardCode.addTextChangedListener(codeNumberWatcher)
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
    override fun onResume() {
        super.onResume()
        if(!btnClick){
            val action = HomeFragmentDirections.callHome(true)
            findNavController().navigate(action)
        }
        btnClick = false
    }
    private fun formatPhoneNumber() {
        if(binding.payCardNumber.text.toString().length==16){
            binding.payCardNumber.removeTextChangedListener(cardNumberWatcher)
            var number = binding.payCardNumber.text.toString()
            var newNumber = ""
            for(i in 0..15 step 4){
                newNumber = "$newNumber${number.substring(i, i+4)} "
            }
            cardNumber = number
            binding.payCardNumber.setText(newNumber)
            binding.payCardNumber.setSelection(binding.payCardNumber.text.toString().length)
            binding.payCardNumber.addTextChangedListener(cardNumberWatcher)
        }else{
            cardNumber = null
            var text = binding.payCardNumber.text.toString().replace(""" """.toRegex(),"")
            binding.payCardNumber.removeTextChangedListener(cardNumberWatcher)
            binding.payCardNumber.setText(text)
            binding.payCardNumber.setSelection(binding.payCardNumber.text.toString().length)
            binding.payCardNumber.addTextChangedListener(cardNumberWatcher)
        }
    }
    private fun payOrder(){
        if(cardNumber.isNullOrEmpty()||cardCode.isNullOrEmpty()||binding.payCardMonth.value == 0 || binding.payCardYear.value == 0){
            if(cardNumber.isNullOrEmpty()){
                animateViews(binding.payCardNumber)
            }
            if(cardCode.isNullOrEmpty()){
                animateViews(binding.payCardCode)
            }
            if(binding.payCardMonth.value == 0 ){
                animateViews(binding.payCardMonth)
            }
            if(binding.payCardYear.value == 0 ){
                animateViews(binding.payCardYear)
            }
            return
        }
        showSpinner(true)
        var eDate = "${yearsFirst[binding.payCardYear.value]}-${if(binding.payCardMonth.value<=9) 0 else String()}${binding.payCardMonth.value}"
        val body = okhttp3.FormBody.Builder()
            .add("email",args.userEmail)
            .add("id",args.userId.toString())
            .add("cookingDate_id",args.coookingDateId.toString())
            .add("order_id", args.orderId.toString())
            .add("cardNumber",cardNumber.toString().trim())
            .add("cardCode",cardCode.toString().trim())
            .add("expirationDate",eDate)
            .build()
        HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/api/order/payOrder",body,args.userToken)).enqueue(object :
            Callback {
            override fun onFailure(call: Call, e: IOException) {
                showSpinner(false)
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post{
                    Toast.makeText(requireActivity(),"The attempt to pay your pre-order failed with generalized message: ${e.localizedMessage}",
                        Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                showSpinner(false)
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val json = JSONObject(response.body!!.string())
                    if(!json.getBoolean("hasErrors")){
                        Handler(Looper.getMainLooper()).post{
                            var dialogBuilder = AlertDialog.Builder(requireContext())
                            dialogBuilder.setMessage("${json.getString("msg")}")
                                .setCancelable(false)
                                .setPositiveButton("Ok", DialogInterface.OnClickListener{
                                        _, _ ->
                                    Handler(Looper.getMainLooper()).post {
                                        var action = CalendarFragmentDirections.callCalendarFragmentGlobal()
                                        findNavController().navigate(action)
                                    }
                                })
                            val alert = dialogBuilder.create()
                            alert.setTitle("Payment successful")
                            alert.show()
                        }
                    }else{
                        when {
                            json.getInt("errorCode")==-1 -> {
                                notLoggedIntAlert()
                            }
                            json.getInt("errorCode")<=-2 -> {
                                showWarningMessage(json.getString("msg"))
                            }
                            else -> {
                                Handler(Looper.getMainLooper()).post{
                                    Toast.makeText(requireActivity(),"The attempt to pay your pre-order failed with server message: ${json.getString("msg")}",
                                        Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
            }
        })
    }
    //========================================
    // ui elements
    private fun showSpinner(value: Boolean){
        Handler(Looper.getMainLooper()).post {
            binding.paySpinnerLayout.visibility =  when(value){
                true -> View.VISIBLE
                else -> View.INVISIBLE
            }
        }
    }
    private fun showWarningMessage(text:String){
        //showWarningMessage(json.getString("msg"))
        Handler(Looper.getMainLooper()).post{
            Toast.makeText(requireActivity(),"$text", Toast.LENGTH_LONG).show()
            val action = NavGraphDirections.callCalendarFragmentGlobal()
            findNavController().navigate(action)
        }
    }
    private fun notLoggedIntAlert(){
        Handler(Looper.getMainLooper()).post{
            Toast.makeText(requireActivity(),"You are not authenticated in Kungfu BBQ server anylonge. Please log in again.",
                Toast.LENGTH_LONG).show()
            val action = NavGraphDirections.callHome(false)
            findNavController().navigate(action)
        }
    }
    @SuppressLint("ObjectAnimatorBinding")
    private fun animateViews(viewObject:Any){
        ObjectAnimator
            .ofFloat(viewObject,"translationX",0f,30f,-30f,30f,-30f,0f)
            .apply {
                duration = 1000
            }
            .start()
    }
}