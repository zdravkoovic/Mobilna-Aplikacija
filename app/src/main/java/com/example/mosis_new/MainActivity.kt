package com.example.mosis_new

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.example.mosis_new.databinding.ActivityMainBinding
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.mosis_new.model.User
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity(){

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var auth: FirebaseAuth
    private lateinit var drawer: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        auth = Firebase.auth

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        drawer = binding.drawerLayout
        val navView: NavigationView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.mapFragment
            ), drawer
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        binding.toolbar.setupWithNavController(navController, AppBarConfiguration(
            setOf(R.id.mapFragment), drawer
        ))

        binding.mainContent.mainFab.setOnClickListener{
            navController.navigate(R.id.action_MapFragment_To_AddObjectFragment)
        }

        navController.addOnDestinationChangedListener{controller, destination, arguments->
            if(R.id.loginFragment != destination.id && R.id.registerFragment != destination.id
                && R.id.addObjectFragment != destination.id && R.id.filterFragment != destination.id){
                binding.mainContent.mainFab.show()
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                binding.appBar.isVisible = true
            }else if(R.id.addObjectFragment == destination.id){
                binding.mainContent.mainFab.hide()
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                binding.appBar.isVisible = true
            }else if(R.id.filterFragment == destination.id){
                binding.appBar.isVisible = true
                binding.mainContent.mainFab.hide()
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }else{
                binding.appBar.isVisible = false
                binding.mainContent.mainFab.hide()
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        navController.navigate(R.id.action_MapFragment_To_FilterFragment)
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        return super.onPrepareOptionsMenu(menu)
    }

}
