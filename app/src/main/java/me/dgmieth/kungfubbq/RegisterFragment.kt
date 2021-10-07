package me.dgmieth.kungfubbq

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.fragment_register.*

class RegisterFragment : Fragment(R.layout.fragment_register) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var dialogBuilder = AlertDialog.Builder(activity)
        dialogBuilder.setMessage("In order to register with Kungfu BBQ you need to have an INVITATION CODE. If you don't have one, please message Kungfu BBQ requesting one. IMPORTANT: on the message, you MUST send the e-mail you want to create the account with.")
            .setCancelable(false)
            .setPositiveButton("ok", DialogInterface.OnClickListener{
                    dialog, id ->
            })
        val alert = dialogBuilder.create()
        alert.setTitle("Invitation code needed")
        alert.show()

        registerCancelBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }
}