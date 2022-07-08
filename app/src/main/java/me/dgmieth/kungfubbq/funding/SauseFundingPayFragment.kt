package me.dgmieth.kungfubbq.funding

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
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
import android.widget.NumberPicker
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import me.dgmieth.kungfubbq.NavGraphDirections
import me.dgmieth.kungfubbq.R
import me.dgmieth.kungfubbq.TipState
import me.dgmieth.kungfubbq.USER_LOGGED
import me.dgmieth.kungfubbq.databinding.FragmentSauseFundingPayBinding
import me.dgmieth.kungfubbq.httpCtrl.HttpCtrl
import me.dgmieth.kungfubbq.support.formatter.FormatObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class SauseFundingPayFragment : Fragment(R.layout.fragment_sause_funding_pay) {

    private val TAG = "PayFragment"
    private val args : SauseFundingPayFragmentArgs by navArgs()
    private var cardNumber : String? = null
    private var cardCode : String? = null
    private var btnClick = true
    private var tipState : TipState = TipState.NONE
    private var tipAmountGiven = 0.0
    private var selectedSize = ""

    var yearsFirst = arrayListOf<String>()
    private val cardNumberWatcher = object: TextWatcher {
        override fun afterTextChanged(s: Editable?) { }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            formatCreditCardInformation()
        }
    }
    private val codeNumberWatcher = object: TextWatcher {
        override fun afterTextChanged(s: Editable?) { }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if(binding.sauceFundingPayCardCode.text.toString().length>=3){
                cardCode = binding.sauceFundingPayCardCode.text.toString()
                return
            }
            cardCode = null
        }
    }

    private var _binding: FragmentSauseFundingPayBinding? = null
    private val binding get() = _binding!!

    private val INFO_ERROR = "Infomration error!"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if(args.bottleQtty==0 || args.price == 0.0f || args.userEmail.isNullOrEmpty() || args.userId == 0 || args.token.isNullOrEmpty()) {
            showAlert("Internal system error. Not possible to process your payment at this moment.", INFO_ERROR)
        }else{
            FormatObject.setTotalAmount(args.price.toDouble())
            showAlert(getString(R.string.funding_alert_tip_information),"Tip information")
        }
        _binding = FragmentSauseFundingPayBinding.inflate(inflater, container, false)
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
        binding.sauceFundingPayCardYear.minValue = 0
        binding.sauceFundingPayCardYear.maxValue = 20
        binding.sauceFundingPayCardYear.displayedValues = years
        binding.sauceFundingPayCardMonth.minValue = 0
        binding.sauceFundingPayCardMonth.maxValue = months.size -1
        binding.sauceFundingPayCardMonth.displayedValues = months

        binding.sauceFundingPayQttyBottlesAmount.text = args.bottleQtty.toString()
        binding.sauceFundingPayBottlesAmount.text = "U$ ${String.format("%.2f",FormatObject.returnTotalAmountDue(args.bottleQtty))}"
        binding.sauceFundingPayCardNumber.addTextChangedListener(cardNumberWatcher)
        binding.sauceFundingPayCardCode.addTextChangedListener(codeNumberWatcher)

        binding.sauceFundingPayCardMonth.setOnValueChangedListener{_,_,_ ->
        }
        binding.sauceFundingPayCardMonth.setOnValueChangedListener{_,_,_ ->
        }
        binding.sauceFundingPayCancelBtn.setOnClickListener {
            val action = NavGraphDirections.callHome(true)
            findNavController().navigate(action)
        }
        binding.sauceFundingPayPayBtn.setOnClickListener {
            payOrder()
        }
        //tip states
        binding.sauceFundingPayTip15.setOnTouchListener { v, event ->
            onTouchListenerFunction(event,v,TipState.FIFTEEN)
        }
        binding.sauceFundingPayTip20.setOnTouchListener { v, event ->1
            onTouchListenerFunction(event,v,TipState.TWENTY)
        }
        binding.sauceFundingPayTipCustom.setOnTouchListener { v, event ->
            onTouchListenerFunction(event,v,TipState.CUSTOM)
        }
    }
    private fun onTouchListenerFunction(event: MotionEvent, v:View, tipNewState:TipState):Boolean{
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
                binding.sauceFundingPayTipAmount.text = "U$ 0.00"
                binding.sauceFundingPayTotalAmount.text = "U$ ${String.format("%.2f", FormatObject.returnTotalAmountDue(args.bottleQtty))}"
                tipAmountGiven = 0.0
            }
            TipState.FIFTEEN -> {
                var amount = FormatObject.returnTotalAmountDue(args.bottleQtty)
                tipAmountGiven = amount*TipState.FIFTEEN.getTipPercentage()
                var totalAmount = amount+tipAmountGiven
                binding.sauceFundingPayTipAmount.text =  "U$ ${String.format("%.2f", tipAmountGiven)}"
                binding.sauceFundingPayTotalAmount.text = "U$ ${String.format("%.2f", totalAmount)}"
            }
            TipState.TWENTY -> {
                var amount = FormatObject.returnTotalAmountDue(args.bottleQtty)
                tipAmountGiven = amount*TipState.TWENTY.getTipPercentage()
                var totalAmount = amount+tipAmountGiven
                binding.sauceFundingPayTipAmount.text =  "U$ ${String.format("%.2f", tipAmountGiven)}"
                binding.sauceFundingPayTotalAmount.text = "U$ ${String.format("%.2f", totalAmount)}"
            }
            else -> {
                binding.sauceFundingPayTipCustom.isPressed = true

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
                    .setNegativeButton("Cancel", DialogInterface.OnClickListener{ _, _ ->

                    })
                    .setPositiveButton("Ok", DialogInterface.OnClickListener { _, _ ->
                        if(!textField.text.toString().isNullOrEmpty()){
                            var amount = FormatObject.returnTotalAmountDue(args.bottleQtty)
                            tipAmountGiven = textField.text.toString().toDouble()
                            var totalAmount = amount+tipAmountGiven
                            binding.sauceFundingPayTipAmount.text = "U$ ${String.format("%.2f", tipAmountGiven)}"
                            binding.sauceFundingPayTotalAmount.text = "U$ ${String.format("%.2f", totalAmount)}"
                            binding.sauceFundingPayTipCustom.isPressed = true
                            Log.d(TAG,"tipAmountGive ist $tipAmountGiven")
                        }
                    })
                val alert = dialogBuilder.create()
                alert.setTitle("Custom tip amount")
                alert.show()
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED)
            }
        }
        Log.d(TAG,"tipAmountGive ist $tipAmountGiven")
    }
    private fun updatePressedActionTipButtons() {
        Log.d(TAG,"tipState is ${tipState}")
        when (tipState){
            TipState.FIFTEEN -> {
                binding.sauceFundingPayTip20.isPressed = false
                binding.sauceFundingPayTipCustom.isPressed = false
            }
            TipState.TWENTY -> {
                binding.sauceFundingPayTip15.isPressed = false
                binding.sauceFundingPayTipCustom.isPressed = false
            }
            TipState.CUSTOM -> {
                binding.sauceFundingPayTip15.isPressed = false
                binding.sauceFundingPayTip20.isPressed = false
            }
            else -> {
                binding.sauceFundingPayTip15.isPressed = false
                binding.sauceFundingPayTip20.isPressed = false
                binding.sauceFundingPayTipCustom.isPressed = false
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
            val action = NavGraphDirections.callHome(true)
            findNavController().navigate(action)
        }
        btnClick = false
    }
    private fun formatCreditCardInformation() {
        if(binding.sauceFundingPayCardNumber.text.toString().length==16){
            binding.sauceFundingPayCardNumber.removeTextChangedListener(cardNumberWatcher)
            var number = binding.sauceFundingPayCardNumber.text.toString()
            var newNumber = ""
            for(i in 0..15 step 4){
                newNumber = "$newNumber${number.substring(i, i+4)} "
            }
            cardNumber = number
            binding.sauceFundingPayCardNumber.setText(newNumber)
            binding.sauceFundingPayCardNumber.setSelection(binding.sauceFundingPayCardNumber.text.toString().length)
            binding.sauceFundingPayCardNumber.addTextChangedListener(cardNumberWatcher)
        }else{
            cardNumber = null
            var text = binding.sauceFundingPayCardNumber.text.toString().replace(""" """.toRegex(),"")
            binding.sauceFundingPayCardNumber.removeTextChangedListener(cardNumberWatcher)
            binding.sauceFundingPayCardNumber.setText(text)
            binding.sauceFundingPayCardNumber.setSelection(binding.sauceFundingPayCardNumber.text.toString().length)
            binding.sauceFundingPayCardNumber.addTextChangedListener(cardNumberWatcher)
        }
    }
    private fun payOrder(){
        if(cardNumber.isNullOrEmpty()||cardCode.isNullOrEmpty()||binding.sauceFundingPayCardMonth.value == 0 || binding.sauceFundingPayCardYear.value == 0){
            if(cardNumber.isNullOrEmpty()){
                animateViews(binding.sauceFundingPayCardNumber)
            }
            if(cardCode.isNullOrEmpty()){
                animateViews(binding.sauceFundingPayCardCode)
            }
            if(binding.sauceFundingPayCardMonth.value == 0 ){
                animateViews(binding.sauceFundingPayCardMonth)
            }
            if(binding.sauceFundingPayCardYear.value == 0 ){
                animateViews(binding.sauceFundingPayCardYear)
            }
            return
        }
        showSpinner(true)
        var eDate = "${yearsFirst[binding.sauceFundingPayCardYear.value]}-${if(binding.sauceFundingPayCardMonth.value<=9) 0 else String()}${binding.sauceFundingPayCardMonth.value}"
        val body = okhttp3.FormBody.Builder()
            .add("email",args.userEmail)
            .add("id",args.userId.toString())
            .add("qtty",args.bottleQtty.toString())
            .add("cardNumber",cardNumber.toString().trim())
            .add("cardCode",cardCode.toString().trim())
            .add("expirationDate",eDate)
            .add("tip",tipAmountGiven.toString())
            .build()
        HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/api/sause/payCampaignOrder",body,args.token)).enqueue(object :
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
                        Log.d(TAG,"message is ${json.getString("msg")}}")
                        if(json.getJSONObject("msg").getString("hasInformedShirtSize") == "n"){
                            showAlert("${json.getJSONObject("msg").getString("msg")} ${getString(R.string.extra_text_for_tshirt_size)}","${getString(R.string.shirt_size_alert_title)}")
                        }else{
                            showAlert("${json.getJSONObject("msg").getString("msg")}","${getString(R.string.success)}")
                        }
                    }else{
                        when {
                            json.getInt("errorCode")==-1 -> {
                                showAlert("${getString(R.string.not_logged_in_message)}","${getString(R.string.not_logged_in)}")
                            }
                            else -> {
                                showAlert(json.getJSONObject("msg").getString("msg"),"Error!")
                            }
                        }
                    }
                }
            }
        })
    }
    private fun informTShirtSize(){
       if(selectedSize.isNullOrEmpty()){
           return
       }
        showSpinner(true)
        val body = okhttp3.FormBody.Builder()
            .add("email",args.userEmail)
            .add("id",args.userId.toString())
            .add("size",selectedSize)
            .build()
        HttpCtrl.shared.newCall(HttpCtrl.post(getString(R.string.kungfuServerUrl),"/api/sause/informShirtSize",body,args.token)).enqueue(object :
            Callback {
            override fun onFailure(call: Call, e: IOException) {
                showSpinner(false)
                e.printStackTrace()
                showAlert("The attempt to inform your size failed with generalized message: ${e.localizedMessage}","Error!")
            }
            override fun onResponse(call: Call, response: Response) {
                showSpinner(false)
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val json = JSONObject(response.body!!.string())
                    if(!json.getBoolean("hasErrors")){
                        Log.d(TAG,"message is ${json.getString("msg")}}")
                        showAlert("${json.getJSONObject("msg").getString("msg")}","${getString(R.string.success)}")
                    }else{
                        when {
                            json.getInt("errorCode")==-1 -> {
                                showAlert("${getString(R.string.not_logged_in_message)}","${getString(R.string.not_logged_in)}")
                            }
                            else -> {
                                showAlert(json.getString("msg"),getString(R.string.shirt_size_error))
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
            binding.sauceFundingPaySpinnerLayout.visibility =  when(value){
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
            if(title==getString(R.string.shirt_size_alert_title)){
                val array = arrayOf("Select size", "small", "medium","large","xl","xxl")
                val npicker = NumberPicker(requireContext())
                npicker.maxValue = array.size - 1
                npicker.minValue = 0
                npicker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                npicker.displayedValues = array
                npicker.setOnValueChangedListener { picker, oldVal, newVal ->
                    if(newVal>0){
                        selectedSize = array[newVal]
                    }else{
                        selectedSize = ""
                    }
                    Log.d(TAG,"picker value is $selectedSize")

                }
                dialogBuilder.setView(npicker)
            }
            dialogBuilder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton(if(title==getString(R.string.shirt_size_alert_title)) "Select" else "Ok", DialogInterface.OnClickListener { _, _ ->
                    if (title == "${getString(R.string.not_logged_in)}") {
                        USER_LOGGED = false
                        val action = NavGraphDirections.callHome(false)
                        findNavController().navigate(action)
                    }
                    if (title == "${getString(R.string.success)}" || title == getString(R.string.shirt_size_error)) {
                        val action = NavGraphDirections.callHome(true)
                        findNavController().navigate(action)
                    }
                    if(title!=getString(R.string.success)){
                        tipState = TipState.NONE
                        updateMealTipTotalAmount()
                    }
                    if(title==getString(R.string.shirt_size_alert_title)){
                        informTShirtSize()
                    }
                })
            val alert = dialogBuilder.create()
            alert.setTitle(title)
            alert.show()
            alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
            alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED)
        }
    }
}