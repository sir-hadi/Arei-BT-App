package com.example.android.araibtv03

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_get_started.*

class GetStartedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_started)

        get_started_button.setOnClickListener(){
            val intent= Intent(this,BTConnection::class.java)
            startActivity(intent)
        }
    }
}