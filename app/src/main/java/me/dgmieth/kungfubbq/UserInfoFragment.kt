package me.dgmieth.kungfubbq

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.fragment_register.*
import kotlinx.android.synthetic.main.fragment_userinfo.*
import me.dgmieth.kungfubbq.datatabase.room.KungfuBBQRoomDatabase
import me.dgmieth.kungfubbq.datatabase.room.RoomViewModel
import me.dgmieth.kungfubbq.datatabase.roomEntities.CookingDateAndCookingDateDishesWithOrder
import me.dgmieth.kungfubbq.datatabase.roomEntities.UserAndSocialMedia

class UserInfoFragment : Fragment(R.layout.fragment_userinfo) {

    private val TAG = "UserInfoFragment"

    private val MAPS_API_KEY = "google_maps_api_key"
    private var viewModel: RoomViewModel? = null
    private var userInfo : UserAndSocialMedia? = null
    private var editItemBtn : MenuItem? = null

    private var bag = CompositeDisposable()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProviders.of(this).get(RoomViewModel::class.java)
        var db = KungfuBBQRoomDatabase.getInstance(requireActivity())
        viewModel?.getDbInstance(db)
        //Subscribing to user
        viewModel?.user?.observe(viewLifecycleOwner, Observer {
            Log.d(TAG, "user returned $it")
            viewModel?.getCookingDates()
            if(!it.user.email.isNullOrEmpty()){
                Log.d(TAG,"userUpdateOrder called -> value $it")
                userInfo = it
                userInfo?.let {
                    val user = it
                    userInfoUserName.text = user.user.email
                    userInfoMemberSince.text = user.user.memberSince
                    userInfoName.setText(user.user.name)
                    userInfoPhone.setText(user.user.phoneNumber)
                    userInfoFacebookName.setText(user.socialMedias[0].socialMediaName)
                    userInfoInstagramName.setText(user.socialMedias[1].socialMediaName)
                }
                Log.d(TAG, "user is $it")

            }else{
                Handler(Looper.getMainLooper()).post{
                    Toast.makeText(requireActivity(),"It was not possible to retrieve information from this app's database. Please restart the app.",
                        Toast.LENGTH_LONG).show()
                    var action = CalendarFragmentDirections.callCalendarFragmentGlobal()
                    findNavController().navigate(action)
                }
            }
        })
        viewModel?.getUser()
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        userInfoCancelBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
        userInfoUpdatePasswordBtn.setOnClickListener {
            val action = UserInfoFragmentDirections.callUpdatePassword()
            findNavController().navigate(action)
        }
        userInfoCancelBtn.setOnClickListener {
            showSaveBtn(false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.getItem(0).isVisible = false
        menu.getItem(1).isVisible = true
        menu.getItem(2).isVisible = true
        menu.getItem(3).isVisible = false
        editItemBtn = menu.getItem(2)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.logOutMenuBtn -> {
                val action = HomeFragmentDirections.callHome(false)
                findNavController().navigate(action)
                true
            }
            R.id.editMenuBtn -> {
                showSaveBtn(true)
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }
    private fun showSaveBtn(value:Boolean){
        editItemBtn!!.isVisible = !value
        userInfoBtnGroup.isVisible = value
        userInfoUpdatePasswordBtn.isVisible = !value
    }
}