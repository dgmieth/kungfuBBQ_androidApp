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

class LoginFragment : Fragment(R.layout.fragment_login) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
    }
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
            })
            .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> print(dialog) })
        val alert = dialogBuilder.create()
        alert.setTitle("Password recovery")
        alert.show()
    }
}