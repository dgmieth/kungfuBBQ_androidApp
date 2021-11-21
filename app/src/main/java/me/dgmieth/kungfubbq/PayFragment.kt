package me.dgmieth.kungfubbq

import android.animation.ObjectAnimator
import android.app.AlertDialog
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
import kotlinx.android.synthetic.main.fragment_pay.*
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
            if(payCardCode.text.toString().length>=3){
                cardCode = payCardCode.text.toString()
                return
            }
            cardCode = null
        }
    }

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
            var dialogBuilder = AlertDialog.Builder(activity)
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
        return super.onCreateView(inflater, container, savedInstanceState)
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
        payCardYear.minValue = 0
        payCardYear.maxValue = 20
        payCardYear.displayedValues = years
        payCardMonth.minValue = 0
        payCardMonth.maxValue = months.size -1
        payCardMonth.displayedValues = months
        payCardYear.setOnValueChangedListener{_,_,_ ->
        }
        payCardMonth.setOnValueChangedListener{_,_,_ ->
        }
        payCancelBtn.setOnClickListener {
            val action = PayOrderFragmentDirections.callPayOrderFragmentGlobal(args.coookingDateId)
            findNavController().navigate(action)
        }
        payPayBtn.setOnClickListener {
            payOrder()
        }
        payCardNumber.addTextChangedListener(cardNumberWatcher)
        payCardCode.addTextChangedListener(codeNumberWatcher)
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
    private fun formatPhoneNumber() {
        if(payCardNumber.text.toString().length==16){
            payCardNumber.removeTextChangedListener(cardNumberWatcher)
            var number = payCardNumber.text.toString()
            var newNumber = ""
            for(i in 0..15 step 4){
                newNumber = "$newNumber${number.substring(i, i+4)} "
            }
            cardNumber = number
            payCardNumber.setText(newNumber)
            payCardNumber.setSelection(payCardNumber.text.toString().length)
            payCardNumber.addTextChangedListener(cardNumberWatcher)
        }else{
            cardNumber = null
            var text = payCardNumber.text.toString().replace(""" """.toRegex(),"")
            payCardNumber.removeTextChangedListener(cardNumberWatcher)
            payCardNumber.setText(text)
            payCardNumber.setSelection(payCardNumber.text.toString().length)
            payCardNumber.addTextChangedListener(cardNumberWatcher)
        }
    }
    private fun payOrder(){
        if(cardNumber.isNullOrEmpty()||cardCode.isNullOrEmpty()||payCardMonth.value == 0 || payCardYear.value == 0){
            if(cardNumber.isNullOrEmpty()){
                animateViews(payCardNumber)
            }
            if(cardCode.isNullOrEmpty()){
                animateViews(payCardCode)
            }
            if(payCardMonth.value == 0 ){
                animateViews(payCardMonth)
            }
            if(payCardYear.value == 0 ){
                animateViews(payCardYear)
            }
            return
        }
        showSpinner(true)
        var eDate = "${yearsFirst[payCardYear.value]}-${if(payCardMonth.value<=9) 0 else String()}${payCardMonth.value}"
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
                        Log.d(TAG, "values are $json")
                        Handler(Looper.getMainLooper()).post{
                            var dialogBuilder = AlertDialog.Builder(activity)
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
            paySpinnerLayout.visibility =  when(value){
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
    private fun animateViews(viewObject:Any){
        ObjectAnimator
            .ofFloat(viewObject,"translationX",0f,30f,-30f,30f,-30f,0f)
            .apply {
                duration = 1000
            }
            .start()
    }
}