package it.bonificamarche.services.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import it.bonificamarche.services.aidl.AidlServerServiceImpl

class AidlServerService : Service() {

    private lateinit var aidlServerService: AidlServerServiceImpl

    override fun onBind(intent: Intent): IBinder {
        return aidlServerService
    }

    override fun onCreate() {
        super.onCreate()
        show("Service created")
        aidlServerService = AidlServerServiceImpl()
    }

    companion object {
        private const val TAG = "AidlServerService"
        val show = { log: String ->
            Log.e(TAG, log)
        }
    }
}