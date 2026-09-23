package com.studiolexair.movaphone.core.common.time

/** Fuente de tiempo inyectable: permite tests deterministas. */
interface TimeProvider {
    fun now(): Long
    fun elapsedRealtime(): Long
}

class SystemTimeProvider : TimeProvider {
    override fun now(): Long = System.currentTimeMillis()
    override fun elapsedRealtime(): Long = android.os.SystemClock.elapsedRealtime()
}
