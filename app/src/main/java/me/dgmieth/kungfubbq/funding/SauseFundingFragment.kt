package me.dgmieth.kungfubbq.funding

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Half.toFloat
import android.util.Log
import android.view.*
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.reactivex.disposables.CompositeDisposable
import me.dgmieth.kungfubbq.HomeFragmentDirections
import me.dgmieth.kungfubbq.NavGraphDirections
import me.dgmieth.kungfubbq.R
import me.dgmieth.kungfubbq.USER_LOGGED
import me.dgmieth.kungfubbq.databinding.FragmentCalendarBinding
import me.dgmieth.kungfubbq.databinding.FragmentSauseFundingBinding
import me.dgmieth.kungfubbq.datatabase.room.KungfuBBQRoomDatabase
import me.dgmieth.kungfubbq.datatabase.room.RoomViewModel
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserAndSocialMedia
import me.dgmieth.kungfubbq.httpCtrl.HttpCtrl
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.DecimalFormat

class SauseFundingFragment : Fragment(R.layout.fragment_sause_funding) {

    private val TAG = "SauceFundingFragment"

    private var _binding: FragmentSauseFundingBinding? = null
    private val binding get() = _binding!!

    private var user: UserAndSocialMedia? = null
    private var price = 0.00

    private var npicker : NumberPicker? = null

    private val DATA_FAILURE = "Data Request Failed"
    private val QTTY = "Quantity"
    private var SAUCE_BATCH_COST : Double = 5709.00

    private var viewModel: RoomViewModel? = null
    private var bag = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        showSpinner(true)
        viewModel = ViewModelProviders.of(this).get(RoomViewModel::class.java)
        viewModel?.getDbInstance(KungfuBBQRoomDatabase.getInstance(requireActivity()))

        viewModel?.user?.observe(viewLifecycleOwner, Observer {
            if(!it.user.email.isNullOrEmpty()){
                Log.d(TAG, "user returned")
                user = it
                Log.d(TAG, "calling getSauceFundingInfo")
                getSauceFundingInfo(it)
            }else{
                Log.d(TAG, "no userReturned")
                returnUserFromDBNull()
            }
        })
        viewModel?.getUser()
        _binding = FragmentSauseFundingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        formatWhatTextView()

        binding.fundingProgressBar.progress = 00.0F
        binding.fundingProgressBar.progressText = "0%"
        binding.fundingProgressBar.secondaryProgress = 0F

        binding.fundingCancelBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
        binding.fundingSendBtn.setOnClickListener {
            val dec = DecimalFormat("#,###.00")
            showAlert("Select how many bottles you would like to purchase. Each bottle costs $${dec.format(price)}.",QTTY)
        }
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
    //==========================================================
    //========== HTTP REQUEST
    private fun getSauceFundingInfo(user: UserAndSocialMedia?){
        Log.d(TAG, "getSauceFundingInfo called")
        user?.let {
            var httpUrl = HttpUrl.Builder()
                .scheme("https")
                .host("${getString(R.string.kungfuServerUrlNoSchema)}")
                .addPathSegments("api/sause/getCampaignInformation")
                .build()
            HttpCtrl.shared.newCall(HttpCtrl.get("","",httpUrl,it.user.token.toString())).enqueue(object :
                Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    showSpinner(false)
                    showAlert("The attempt to retrieve/save data failed with server message: ${e.localizedMessage}.",DATA_FAILURE)
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        val json = JSONObject(response.body!!.string())
                        Log.d(TAG, "values are $json")
                        if(!json.getBoolean("hasErrors")){
                            val msg = json.getJSONObject("msg")
                            Handler(Looper.getMainLooper()).post {
                                SAUCE_BATCH_COST = msg.getDouble("batchPrice")
                                Log.d(TAG,"batchPrice is $SAUCE_BATCH_COST")
                                val dec = DecimalFormat("#.##")
                                val value = dec.format((msg.getDouble("totalAmount")/SAUCE_BATCH_COST)*100)
                                formatWhatTextView()
                                binding.fundingProgressBar.progressText = "${(value).toFloat()}%"
                                binding.fundingProgressBar.progress = ((msg.getDouble("totalAmount")/SAUCE_BATCH_COST)*100).toFloat()
                                binding.fundingPreOrdersAmount.text = "U$ ${msg.getDouble("preOrders")}"
                                binding.fundingTipsAmount.text = "U$ ${msg.getDouble("tips")}"
                                price = msg.getDouble("price")
                                Log.d(TAG,"Price is $price")
                            }
                            showSpinner(false)
                        }else{
                            showSpinner(false)
                            if(json.getInt("errorCode")==-1){
                                showAlert("${getString(R.string.not_logged_in_message)}","${getString(R.string.not_logged_in)}")
                            }else{
                                showAlert(json.getString("msg"),DATA_FAILURE)
                            }
                        }
                    }
                }
            })
        }
    }
    //==========================================================
    //========== DB METHODS
    private fun returnUserFromDBNull() {
        showAlert("It was not possible to recover data from app`s database. Please restart the app.","Database error!")
    }
    //==========================================================
    //========== UI METHODS
    private fun formatWhatTextView(){
        Handler(Looper.getMainLooper()).post{
            binding.sauseFundingWhatText.text = String.format(getString(R.string.what_text),SAUCE_BATCH_COST)
        }
    }
    private fun showSpinner(value: Boolean){
        Handler(Looper.getMainLooper()).post {
            binding.sauceFundingSpinnerLayout.visibility =  when(value){
                true -> View.VISIBLE
                else -> View.INVISIBLE
            }
        }
    }
    private fun showAlert(message:String,title:String){
        Handler(Looper.getMainLooper()).post{
            var dialogBuilder = AlertDialog.Builder(requireContext())
            dialogBuilder.setMessage(message)
                .setCancelable(title != DATA_FAILURE)
                .setPositiveButton(if(title==QTTY) "Purchase" else "Ok", DialogInterface.OnClickListener{
                        _, _ ->
                    if(title == DATA_FAILURE){
                        val action = NavGraphDirections.callHome(false)
                        findNavController().navigate(action)
                    }
                    if(title==QTTY){
                        npicker?.let{
                            Log.d(TAG,"selectedNumber is ${it.value}")
                            user?.let{ u ->
                                val action = SauseFundingFragmentDirections.goToSauseFundingPayFragment(it.value,price.toFloat(),u.user.email,u.user.userId,u.user.token.toString())
                                findNavController().navigate(action)
                            }
                        }
                    }
                })
            if(title == QTTY){
                npicker = NumberPicker(requireContext())
                npicker!!.maxValue = 100
                npicker!!.minValue = 1
                npicker!!.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                dialogBuilder.setView(npicker!!)
            }
            val alert = dialogBuilder.create()
            alert.setTitle(title)
            alert.show()
        }
    }
}