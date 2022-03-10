package it.bonificamarche.services.common

import android.util.Log

/**
 * Used for the logging.
 */
val show = { TAG: String, log: Any ->
    Log.e(TAG, "[$TAG] $log")
}