package it.bonificamarche.services

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import it.bonificamarche.services.services.AidlServerService


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startService(Intent(this, AidlServerService::class.java))
        //finish()
    }
}