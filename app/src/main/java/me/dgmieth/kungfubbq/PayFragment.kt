package me.dgmieth.kungfubbq

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import androidx.appcompat.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.EditText
import androidx.core.view.marginLeft
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import me.dgmieth.kungfubbq.databinding.FragmentPayBinding
import me.dgmieth.kungfubbq.httpCtrl.HttpCtrl
import me.dgmieth.kungfubbq.support.formatter.FormatObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

enum class TipState {
    NONE (0.0),
    FIFTEEN (0.15) ,
    TWENTY (0.20),
    CUSTOM (0.0);

    private var tipPercent: Double? = null

    constructor(tipPercent:Double){
        this.tipPercent = tipPercent
    }
    fun getTipPercentage():Double{
        return tipPercent!!
    }
}
@SuppressLint("SetTextI18n")
class PayFragment : Fragment(R.layout.fragment_pay) {

    private val TAG = "PayFragment"
    private val args : PayFragmentArgs by navArgs()
    private var cardNumber : String? = null
    private var cardCode : String? = null
    private var btnClick = true
    private var tipState : TipState = TipState.NONE
    private var tipAmoutGiven = 0.0
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
            showAlert("Communication with this apps's database failed. Please restart the app.", getString(R.string.database_error))
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
        val months = arrayOf("Month","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
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
        binding.payMealsAmount.text = "U$ ${String.format("%.2f",FormatObject.returnTotalAmountDue(args.qttyOfMeals))}"
        binding.payCardNumber.addTextChangedListener(cardNumberWatcher)
        binding.payCardCode.addTextChangedListener(codeNumberWatcher)
        //tip states
        binding.payTip15.setOnTouchListener { v, event ->
            onTouchListenerFunction(event,v,TipState.FIFTEEN)
        }
        binding.payTip20.setOnTouchListener { v, event ->1
            onTouchListenerFunction(event,v,TipState.TWENTY)
        }
        binding.payTipCustom.setOnTouchListener { v, event ->
            onTouchListenerFunction(event,v,TipState.CUSTOM)
        }
    }
    private fun onTouchListenerFunction(event:MotionEvent,v:View,tipNewState:TipState):Boolean{
        Log.d(TAG,"${event?.action}" )
        Log.d(TAG,"${MotionEvent.ACTION_DOWN}" )
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                v.isPressed = !v.isPressed
                updateTipState(tipNewState)
            }
        }
        return true
    }
    private fun updateTipState(newState:TipState){
        tipState = if(newState==tipState){
            TipState.NONE
        }else{
            newState
        }
        updatePressedActionTipButtons()
        updateMealTipTotalAmount()
    }
    private fun updateMealTipTotalAmount(){
        when (tipState){
            TipState.NONE -> {
                binding.payTipAmount.text = "U$ 0.00"
                binding.payTotalAmount.text = "U$ ${String.format("%.2f", FormatObject.returnTotalAmountDue(args.qttyOfMeals))}"
                tipAmoutGiven = 0.0
            }
            TipState.FIFTEEN -> {
                var amount = FormatObject.returnTotalAmountDue(args.qttyOfMeals)
                tipAmoutGiven = amount*TipState.FIFTEEN.getTipPercentage()
                var totalAmount = amount+tipAmoutGiven
                binding.payTipAmount.text =  "U$ ${String.format("%.2f", tipAmoutGiven)}"
                binding.payTotalAmount.text = "U$ ${String.format("%.2f", totalAmount)}"
            }
            TipState.TWENTY -> {
                var amount = FormatObject.returnTotalAmountDue(args.qttyOfMeals)
                tipAmoutGiven = amount*TipState.TWENTY.getTipPercentage()
                var totalAmount = amount+tipAmoutGiven
                binding.payTipAmount.text =  "U$ ${String.format("%.2f", tipAmoutGiven)}"
                binding.payTotalAmount.text = "U$ ${String.format("%.2f", totalAmount)}"
            }
            else -> {
                binding.payTipCustom.isPressed = true

                var textField = EditText(activity)
                textField.hint = "0.00"
                textField.setBackgroundResource(android.R.color.transparent)
                textField.setHintTextColor(resources.getColor(R.color.textEditHint))
                textField.height = 135
                textField.setPadding(0,0,475,0)
                textField.textSize  = 22.0F
                textField.gravity = Gravity.RIGHT
                textField.inputType =
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                textField.filters = arrayOf(InputFilter.LengthFilter(6))
                textField.addTextChangedListener(object : TextWatcher{
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
                    override fun afterTextChanged(s: Editable?) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        textField.removeTextChangedListener(this)
                        Log.d(TAG, "justNumbers is ${s.toString()}")
                        var justNumbers = s.toString().replace("""[^0-9]""".toRegex(),"")
                        Log.d(TAG, "justNumbers is $justNumbers replaceOne")
                        justNumbers = justNumbers.toInt().toString()
                        Log.d(TAG, "justNumbers is $justNumbers replaceTwo")
                        when {
                            justNumbers.length > 2 -> {
                                Log.d(TAG, "justNumbers is $justNumbers ${justNumbers.substring(0,justNumbers.length-2)}  ${justNumbers.substring(justNumbers.length-2)} 33333333")
                                textField.text = Editable.Factory.getInstance().newEditable("${justNumbers.substring(0,justNumbers.length-2)}.${justNumbers.substring(justNumbers.length-2)}")
                            }
                            justNumbers.length==1 -> {
                                Log.d(TAG, "justNumbers is $justNumbers 11111")
                                textField.text = Editable.Factory.getInstance().newEditable("0.0${justNumbers}")
                            }
                            justNumbers.length==2 -> {
                                Log.d(TAG, "justNumbers is $justNumbers 22222")
                                textField.text = Editable.Factory.getInstance().newEditable("0.${justNumbers}")
                            }
                        }
                        textField.addTextChangedListener(this)
                        textField.setSelection(textField.text.toString().length)
                    }

                })
                Log.d(TAG,"textEdit value is ${textField.text.toString()}")

                var dialogBuilder = AlertDialog.Builder(requireContext())
                dialogBuilder.setMessage("How much do you want to tip?")
                    .setView(textField)
                    .setCancelable(true)
                    .setNegativeButton("Cancel", DialogInterface.OnClickListener{_,_ ->

                    })
                    .setPositiveButton("Ok", DialogInterface.OnClickListener { _, _ ->
                        if(!textField.text.toString().isNullOrEmpty()){
                            var amount = FormatObject.returnTotalAmountDue(args.qttyOfMeals)
                            tipAmoutGiven = textField.text.toString().toDouble()
                            var totalAmount = amount+tipAmoutGiven
                            binding.payTipAmount.text = "U$ ${String.format("%.2f", tipAmoutGiven)}"
                            binding.payTotalAmount.text = "U$ ${String.format("%.2f", totalAmount)}"
                            binding.payTipCustom.isPressed = true
                            Log.d(TAG,"tipAmountGive ist $tipAmoutGiven")
                        }
                    })
                val alert = dialogBuilder.create()
                alert.setTitle("Custom tip amount")
                alert.show()
            }
        }
        Log.d(TAG,"tipAmountGive ist $tipAmoutGiven")
    }
    private fun updatePressedActionTipButtons() {
        Log.d(TAG,"tipState is ${tipState}")
        when (tipState){
            TipState.FIFTEEN -> {
                binding.payTip20.isPressed = false
                binding.payTipCustom.isPressed = false
            }
            TipState.TWENTY -> {
                binding.payTip15.isPressed = false
                binding.payTipCustom.isPressed = false
            }
            TipState.CUSTOM -> {
                binding.payTip15.isPressed = false
                binding.payTip20.isPressed = false
            }
            else -> {
                binding.payTip15.isPressed = false
                binding.payTip20.isPressed = false
                binding.payTipCustom.isPressed = false
            }
        }
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
            .add("tip",tipAmoutGiven.toString())
            .build()
        HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/api/order/payOrder",body,args.userToken)).enqueue(object :
            Callback {
            override fun onFailure(call: Call, e: IOException) {
                showSpinner(false)
                e.printStackTrace()
                showAlert("The attempt to pay your pre-order failed with generalized message: ${e.localizedMessage}","Error!")
            }
            override fun onResponse(call: Call, response: Response) {
                showSpinner(false)
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val json = JSONObject(response.body!!.string())
                    if(!json.getBoolean("hasErrors")){
                        showAlert("${json.getString("msg")}","${getString(R.string.success)}")
                    }else{
                        when {
                            json.getInt("errorCode")==-1 -> {
                                showAlert("${getString(R.string.not_logged_in_message)}","${getString(R.string.not_logged_in)}")
                            }
                            else -> {
                                showAlert("The attempt to pay your pre-order failed with server message: ${json.getString("msg")}","Error!")
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
    @SuppressLint("ObjectAnimatorBinding")
    private fun animateViews(viewObject:Any){
        ObjectAnimator
            .ofFloat(viewObject,"translationX",0f,30f,-30f,30f,-30f,0f)
            .apply {
                duration = 1000
            }
            .start()
    }
    private fun showAlert(message:String,title:String) {
        Handler(Looper.getMainLooper()).post {
            var dialogBuilder = AlertDialog.Builder(requireContext())
            dialogBuilder.setMessage(message)
                .setCancelable(title != "${getString(R.string.not_logged_in)}")
                .setPositiveButton("Ok", DialogInterface.OnClickListener { _, _ ->
                    if (title == "${getString(R.string.not_logged_in)}") {
                        USER_LOGGED = false
                        val action = NavGraphDirections.callHome(false)
                        findNavController().navigate(action)
                    }
                    if (title == "${getString(R.string.database_error)}") {
                        var action = CalendarFragmentDirections.callCalendarFragmentGlobal()
                        findNavController().navigate(action)
                    }
                    if (title == "${getString(R.string.success)}") {
                        var action = CalendarFragmentDirections.callCalendarFragmentGlobal()
                        findNavController().navigate(action)
                    }
                    if(title!=getString(R.string.success)){
                        tipState = TipState.NONE
                        updateMealTipTotalAmount()
                    }
                })
            val alert = dialogBuilder.create()
            alert.setTitle(title)
            alert.show()
        }
    }
}