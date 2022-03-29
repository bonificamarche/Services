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
import it.bonificamarche.services.services.AidlServerService


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
        } else startAidlService()
    }

    /**
     * Start service when the permission is granted.
     */
    private fun startAidlService() {
        startService(Intent(this, AidlServerService::class.java))
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
                startAidlService()
            } else {
                show(TAG, "Permission denied!")

                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.attention)
                    .setMessage("Devi accettare i permessi per permettere all'app di accedere alla memoria.")
                    .setPositiveButton(R.string.ok) { _, _ ->
                        finish()
                    }
                    .show()
            }
        }

    companion object {
        const val TAG = "Main Activity"
    }
}