package it.bonificamarche.services.services

import android.app.Service
import android.content.*
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import it.bonificamarche.services.R
import it.bonificamarche.services.aidl.AidlServerServiceImpl
import it.bonificamarche.services.common.show

class AidlServerService : Service() {

    // Aidl Server Service Property
    private lateinit var aidlServerService: AidlServerServiceImpl

    override fun onBind(intent: Intent): IBinder {
        return aidlServerService
    }

    /**
     * On Create
     */
    override fun onCreate() {
        super.onCreate()
        show(TAG, "Started!")

        aidlServerService = AidlServerServiceImpl(this)
        aidlServerService.initialization()
    }

    /**
     * On Destroy
     */
    override fun onDestroy() {
        super.onDestroy()
        aidlServerService.destroyResources()
    }

    companion object {
        private const val TAG = "Aidl Server Service"
    }
}