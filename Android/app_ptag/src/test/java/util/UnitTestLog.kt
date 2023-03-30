package util

import com.faceunity.toolbox.log.FuLogInterface

/**
 *
 */
class UnitTestLog(var enable: Boolean = true) : FuLogInterface {
    companion object {
        const val TAG = "PTALog"
    }

    override fun debug(message: String) {
        if (!enable) return
        println("debug: $message")
    }

    override fun error(message: String, exception: Exception?) {
        if (!enable) return
        println("error: $message ${exception ?: ""}")
    }

    override fun info(message: String) {
        if (!enable) return
        println("info: $message")
    }

    override fun warn(message: String) {
        if (!enable) return
        println("warn: $message")
    }

}