package it.bonificamarche.services

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import it.bonificamarche.services.common.show


class StartExternalApplication : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle: Bundle = intent.extras!!
        val packageApp = bundle.getString("package")

        Log.e(TAG, packageApp.toString())
        try {
            val intent: Intent? = packageManager.getLaunchIntentForPackage(packageApp!!)
            startActivity(intent)
        } catch (e: Exception) {
            show(TAG, "Error: ${e.message}")
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.error))
                .setMessage(e.message)
                .setPositiveButton(getString(R.string.ok)) { _, _ -> }
                .show()
        }
    }

    companion object {
        const val TAG = "Start External Application"
    }
}