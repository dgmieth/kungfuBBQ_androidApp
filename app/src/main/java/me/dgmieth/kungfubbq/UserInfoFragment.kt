package me.dgmieth.kungfubbq

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.fragment_register.*
import kotlinx.android.synthetic.main.fragment_userinfo.*

class UserInfoFragment : Fragment(R.layout.fragment_userinfo) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userInfoCancelBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
        userInfoUpdatePasswordBtn.setOnClickListener {
            val action = UserInfoFragmentDirections.callUpdatePassword()
            findNavController().navigate(action)
        }
    }
}