package me.dgmieth.kungfubbq

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.Menu
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.onesignal.OneSignal
import me.dgmieth.kungfubbq.databinding.ActivityMainBinding
import java.util.concurrent.Executors

const val ONESIGNAL_APP_ID = "oneSignalAppID"
const val DEVELOPER_MODE = true

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    private lateinit var binding:ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity","onCreate called")
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.findNavController()
        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController)
    }
    override fun onStart() {
        super.onStart()
        Log.d("MainActivity","onStart called")
        oneSignalConfiguration()
    }
    private fun oneSignalConfiguration(){
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE)
        OneSignal.initWithContext(this)
        OneSignal.setAppId(getString(R.string.oneSignalAppID))
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_bar, menu)
        return true
    }
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
    override fun onDestroy() {
        Log.d("MainActivity","onStop called")
        OneSignal.removeExternalUserId()
        super.onDestroy()
    }
}