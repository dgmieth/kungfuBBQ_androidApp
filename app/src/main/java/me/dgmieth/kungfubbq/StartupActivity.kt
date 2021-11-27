package me.dgmieth.kungfubbq

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import me.dgmieth.kungfubbq.databinding.ActivityStartupBinding

class StartupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartupBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.startupIcon.alpha = 0f
        binding.startupIcon.animate().setDuration(1200).alpha(1f).withEndAction{
            val i = Intent(this,MainActivity::class.java)
            startActivity(i)
            overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out)
            finish()
        }

    }

}