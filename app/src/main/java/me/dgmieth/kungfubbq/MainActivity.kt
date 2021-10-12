package me.dgmieth.kungfubbq

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("MainActivity", "onCreate: starts")

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        Log.d("MainActivity", "onCreate: navHostFragment")
        navController = navHostFragment.findNavController()
        setSupportActionBar(toolbar)
        setupActionBarWithNavController(navController)
        Log.d("MainActivity", "onCreate: end")

    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_bar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return  super.onOptionsItemSelected(item)
    }

}