package me.dgmieth.kungfubbq

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_login.*
import me.dgmieth.kungfubbq.datatabase.room.Actions
import me.dgmieth.kungfubbq.datatabase.room.KungfuBBQRoomDatabase
import me.dgmieth.kungfubbq.datatabase.room.RoomViewModel
import me.dgmieth.kungfubbq.datatabase.roomEntities.SocialMediaInfo
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserAndSocialMedia
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserDB
import me.dgmieth.kungfubbq.httpRequets.Endpoints
import me.dgmieth.kungfubbq.httpRequets.ForgotPasswordBodyData
import me.dgmieth.kungfubbq.httpRequets.HttpRequestCtrl
import me.dgmieth.kungfubbq.httpRequets.UserAuthentication
import me.dgmieth.kungfubbq.httpRequets.responsesObjects.userValidation.ForgotPasswordResponseValidation
import me.dgmieth.kungfubbq.httpRequets.responsesObjects.userValidation.UserResponseValidation
import org.json.JSONObject

private const val TAG = "LoginFragment"

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var viewModel: RoomViewModel? = null
    private val bag = CompositeDisposable()
    private var userResponseValidation : UserResponseValidation? = null
    private var countChecker = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProviders.of(this).get(RoomViewModel::class.java)
        var db = KungfuBBQRoomDatabase.getInstance(requireActivity())
        viewModel?.getDbInstance(db)
        viewModel?.user?.observe(viewLifecycleOwner, Observer {
            if(!it.user.email.isNullOrEmpty()){
                handleIt(it)
            }else{
                handleNullCase()
            }
        })
        viewModel?.returnMsg?.observe(viewLifecycleOwner, Observer {
            handleReturnMessage(it)
        })
        viewModel?.getUser()
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        loginResetPassword.setOnClickListener{
            println("clicked")
            callResetPasswordAlert()
        }
        loginRegisterBtn.setOnClickListener {
            val action = LoginFragmentDirections.callRegisterFragment()
            findNavController().navigate(action)
        }
        loginCancelBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
        loginLoginBtn.setOnClickListener {
            Log.d(TAG, "loginButton clicked")
            logUserIn()
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
    //DATA HANDLING PROCESSES
    private fun handleReturnMessage(it: Actions) {
        Log.d(TAG, "handleReturnMessage with actions $it ${it==Actions.UserInsert}")
        when(it){
            Actions.UserInsert -> checkDBProcessEnded()
            Actions.UserInsError -> {
                Toast.makeText(requireActivity(),"Error while attempting to save information to database",Toast.LENGTH_LONG).show()
            }
            Actions.SocialMediaInsert -> checkDBProcessEnded()
            Actions.SocialMediaInsError -> {
                Toast.makeText(requireActivity(),"Error while attempting to save information to database",Toast.LENGTH_LONG).show()
            }
            Actions.UserDeletion -> {
                val u = userResponseValidation!!.data
                var user = UserDB(u.id, u.email,u.memberSince,u.name,u.phoneNumber,u.token,1)
                viewModel?.insertUserInfo(user)
            }
            Actions.UserDelError -> {
                Toast.makeText(requireActivity(),"Error while attempting to save information to database",Toast.LENGTH_LONG).show()
            }
            Actions.SocialMediaDeletion -> {
                val u = userResponseValidation!!.data
                val sm = u.socialMediaInfo
                var socialM : MutableList<SocialMediaInfo> = arrayListOf()
                for (i in sm){
                    var sMInfo = SocialMediaInfo(i.socialMedia,i.sociaMediaName,u.id)
                    socialM.add(sMInfo)
                }
                viewModel?.insertSocialMediaInfo(socialM)
            }
            Actions.SocialMediaDelError -> {
                Toast.makeText(requireActivity(),"Error while attempting to save information to database",Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(requireActivity(),"Unknown error",Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun checkDBProcessEnded() {
        Log.d(TAG, "checkDBProcessEnded called")
        countChecker += 1
        if(countChecker==2){
            val action = NavGraphDirections.callHome(true)
            findNavController().navigate(action)
        }
    }


    //========================================================
    //HTTP REQUEST METHODS
    private fun logUserIn() {
        if(!loginUserEmail.text.toString().isNullOrEmpty()&&!loginPassword.text.toString().isNullOrEmpty()){
            val newUserAuth = UserAuthentication(loginPassword.text.toString(),loginUserEmail.text.toString())
            Log.d("HttpRequestCtrl", "jsonObject is $newUserAuth")
            bag.add(
                HttpRequestCtrl.buildService((Endpoints::class.java), getString(R.string.kungfuServerUrl)).login(newUserAuth)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe({response -> onResponse(response)}, {t -> onFailure(t) })
            )
        }else{
            Log.d(TAG, "loginButton not validated")
            Toast.makeText(requireActivity(),"You must inform your e-mail and your password",Toast.LENGTH_LONG).show()
        }
    }
    private fun recoverPasswordEmail(email:String){
        val userEmail = ForgotPasswordBodyData(email)
        bag.add(
            HttpRequestCtrl.buildService((Endpoints::class.java), getString(R.string.kungfuServerUrl)).forgotPassword(userEmail)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({response -> forgotPasswordResponse(response)}, {t -> forgotPasswordFailure(t) })
        )
    }

    //========================================================
    //DATABASE RETURN CALLBACKS
    private fun handleNullCase() {
        Log.d(TAG, "no data returned")
    }

    private fun handleIt(it: UserAndSocialMedia) {
        Log.d(TAG, "dataReturn was ${it.user.email} and ${it.user.userId}")
        loginUserEmail.setText(it.user.email)
    }
    //========================================================
    //HTTP RETURN CALLBACKS
    private fun forgotPasswordFailure(t: Throwable) {
        Toast.makeText(requireActivity(),"The attempt to recover password failed.",Toast.LENGTH_LONG).show()
    }

    private fun forgotPasswordResponse(response: ForgotPasswordResponseValidation) {
        if(!response.hasErros){
            Toast.makeText(requireActivity(),"Success! ${response.msg}",Toast.LENGTH_LONG).show()
        }else{
            Toast.makeText(requireActivity(),"The attempt to recover your password failed",Toast.LENGTH_LONG).show()
        }
    }
    private fun onResponse(response: UserResponseValidation?) {
        response?.let{
            if(!it.hasErros){
                userResponseValidation = it
                viewModel?.deleteAllUserInfo()
                viewModel?.deleteAllSocialMediaInfo()

                val u = it.data
                val sm = u.socialMediaInfo
                var user = UserDB(u.id, u.email,u.memberSince,u.name,u.phoneNumber,u.token,1)
                var socialM : MutableList<SocialMediaInfo> = arrayListOf()
                for (i in sm){
                    var sMInfo = SocialMediaInfo(i.socialMedia,i.sociaMediaName,u.id)
                    socialM.add(sMInfo)
                }
                viewModel?.insertUserInfo(user)
                viewModel?.insertSocialMediaInfo(socialM)
                viewModel?.getUser()
            }else{
                Toast.makeText(requireActivity(),"$it.msg",Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun onFailure(t: Throwable?) {
        Log.d("HttpRequestCtrl", "failed $t")
    }
    //OTHER UI METHODS
    private fun callResetPasswordAlert(){
        var textField = EditText(activity)
        textField.hint = "johndoe@mail.com"
        textField.setPadding(16,16,8,0)

        textField.setBackgroundResource(android.R.color.transparent)
        var dialogBuilder = AlertDialog.Builder(activity)
        dialogBuilder.setMessage("Please inform you user acount e-mail address and click on Send.")
            .setView(textField)
            .setCancelable(false)
            .setPositiveButton("Send", DialogInterface.OnClickListener{
                dialog, id ->
                print(dialog)
                print(id)
                Log.d(TAG, "ForgotPasswordAlert with $dialog $id ${textField.text.toString()}")
                recoverPasswordEmail(textField.text.toString())
            })
            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> print(dialog) })
        val alert = dialogBuilder.create()
        alert.setTitle("Password recovery")
        alert.show()
    }


}