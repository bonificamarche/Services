package it.bonificamarche.services

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import it.bonificamarche.services.common.show
import it.bonificamarche.services.services.TimerService
import it.bonificamarche.services.services.RemoteService


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val permission = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            storageLauncher.launch(permission)
        } else startServices()
    }

    private fun startServices() {
        startService(Intent(this, RemoteService::class.java))
        startService(Intent(this, TimerService::class.java))
        finish()
    }

    /**
     * Launcher for storage permission.
     */
    private val storageLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all {
                it.value == true
            }

            if (granted) {
                show(TAG, "Permission granted!")
                startServices()
            } else {
                show(TAG, "Permission denied!")

                MaterialAlertDialogBuilder(this)
                    .setTitle("Attenzione")
                    .setMessage("Devi accettare i permessi per permettere all'app di accedere alla memoria.")
                    .setPositiveButton("Ok") { _, _ ->
                        finish()
                    }
                    .show()
            }
        }

    companion object {
        const val TAG = "Main Activity"
    }
}