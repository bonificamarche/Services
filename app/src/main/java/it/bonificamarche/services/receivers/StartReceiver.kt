package it.bonificamarche.services.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import it.bonificamarche.services.MainActivity

class StartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {

            val intentActivity = Intent(context, MainActivity::class.java)
            intentActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intentActivity)
        }
    }
}