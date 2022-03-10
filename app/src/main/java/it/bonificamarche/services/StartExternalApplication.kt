package it.bonificamarche.services

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import it.bonificamarche.services.common.show


class StartExternalApplication : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle: Bundle = intent.extras!!
        val packageApp = bundle.getString("package")

        try {
            val intent: Intent? = packageManager.getLaunchIntentForPackage(packageApp!!)
            this.startActivity(intent)
        } catch (e: PackageManager.NameNotFoundException) {
            show(TAG, "Error: ${e.message}")
        }
    }

    companion object {
        const val TAG = "Start External Aplication"
    }
}