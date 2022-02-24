package it.bonificamarche.services.ipc

import android.os.Process
import it.bonificamarche.services.IRemoteService


private val binder = object : IRemoteService.Stub() {

    override fun getPid(): Int =
        Process.myPid()

    override fun basicTypes(
        anInt: Int,
        aLong: Long,
        aBoolean: Boolean,
        aFloat: Float,
        aDouble: Double,
        aString: String
    ) {
    }
}