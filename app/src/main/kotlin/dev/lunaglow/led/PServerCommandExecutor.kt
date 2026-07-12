package dev.lunaglow.led

import android.os.IBinder
import android.os.Parcel

fun interface ShellCommandExecutor {
    fun execute(command: String): String
}

class PServerCommandExecutor : ShellCommandExecutor {
    override fun execute(command: String): String {
        val binder = resolveBinder() ?: error("PServerBinder is unavailable")
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeStringArray(arrayOf(command, "1"))
            check(binder.transact(TRANSACTION_EXECUTE, data, reply, 0)) {
                "PServerBinder rejected the command"
            }
            reply.createByteArray()?.toString(Charsets.UTF_8).orEmpty()
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    private fun resolveBinder(): IBinder? {
        val serviceManager = Class.forName("android.os.ServiceManager")
        val getService = serviceManager.getDeclaredMethod("getService", String::class.java)
        return getService.invoke(null, SERVICE_NAME) as? IBinder
    }

    private companion object {
        const val SERVICE_NAME = "PServerBinder"
        const val TRANSACTION_EXECUTE = 0
    }
}
