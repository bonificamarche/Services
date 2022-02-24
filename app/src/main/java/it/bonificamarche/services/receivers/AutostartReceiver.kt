package it.bonificamarche.services.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import it.bonificamarche.services.MainActivity
import it.bonificamarche.services.services.PhotoService

class AutostartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        try {
            Log.i(TAG, "Started")

            val action = intent.action
            if (Intent.ACTION_BOOT_COMPLETED == action ||
                Intent.ACTION_LOCKED_BOOT_COMPLETED == action ||
                Intent.ACTION_REBOOT == action
            ) {

                Log.i(TAG, "Start activity")
//                val service = Intent(context, PhotoService::class.java)
//                context.startService(service)

                context.startActivity(Intent(context, MainActivity::class.java))
            }
        } catch (ex: Exception) {
            Log.e(TAG, ex.message!!)
        }
    }

    companion object {
        private const val TAG = "Autostart Receiver"
    }
}