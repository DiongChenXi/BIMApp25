package com.example.mysignmate

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.mysignmate.databinding.ActivityMainBinding

private const val LOG_TAG = "Main Activity"

class MainActivity : AppCompatActivity() {
    private lateinit var activityMainBinding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
    }

    fun onTranslateSignButtonClick(view: View) {
        Log.d(LOG_TAG, "Navigating into Translate Sign Activity.")
        val intent = Intent(this, TranslateSignActivity::class.java)
        startActivity(intent)
    }

    fun onLearnSignButtonClick(view: View) {
        Log.d(LOG_TAG, "Navigating into Learn Sign Activity.")
        val intent = Intent(this, LearnSignActivity::class.java)
        startActivity(intent)
    }

}
