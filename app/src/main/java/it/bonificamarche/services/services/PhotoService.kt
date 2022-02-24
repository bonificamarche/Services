package it.bonificamarche.services.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.util.*

class PhotoService : Service() {

    private lateinit var timerTask : TimerTask

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "OnCreate")

        timerTask = object : TimerTask() {
            override fun run() {
                Log.i(TAG, "OnStart")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.i(TAG, "OnStartCommand")

        val timer = Timer()
        timer.schedule(timerTask, 0, 1500)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.i(TAG, "OnDestroy")
        super.onDestroy()
    }

    companion object {
        private const val TAG = "Photo Service"
    }
}