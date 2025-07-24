package com.example.mysignmate

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.mysignmate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var activityMainBinding: ActivityMainBinding
    private val poseViewModel : PoseViewModel by viewModels()
    private val handViewModel : HandViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
//
//        val navHostFragment =
//            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
//        val navController = navHostFragment.navController
//        activityMainBinding.navigation.setupWithNavController(navController)
//        activityMainBinding.navigation.setOnNavigationItemReselectedListener {
//            // ignore the reselection
//        }
    }

    public fun onTranslateSignButtonClick(view: View) {
        Toast.makeText(this, "Translate Sign Button Clicked!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, TranslateSignActivity::class.java)
        startActivity(intent)
    }

    public fun onLearnSignButtonClick(view: View) {
        Toast.makeText(this, "Learn Sign Button Clicked!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LearnSignActivity::class.java)
        startActivity(intent)
    }

}
