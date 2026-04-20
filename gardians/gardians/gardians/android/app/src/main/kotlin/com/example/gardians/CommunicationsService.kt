package com.example.gardians

import android.content.Context
import android.net.Uri
import android.provider.CallLog

object CommunicationsService {

    // ── Call Log: last 7 days, max 100 entries ────────────────────────────
    fun getCallLog(context: Context): List<Map<String, Any>> {
        val result      = mutableListOf<Map<String, Any>>()
        val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000

        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.DATE
                ),
                "${CallLog.Calls.DATE} >= ?",
                arrayOf(sevenDaysAgo.toString()),
                "${CallLog.Calls.DATE} DESC"
            ) ?: return result

            cursor.use { c ->
                val numIdx  = c.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIdx = c.getColumnIndex(CallLog.Calls.TYPE)
                val durIdx  = c.getColumnIndex(CallLog.Calls.DURATION)
                val dateIdx = c.getColumnIndex(CallLog.Calls.DATE)

                var count = 0
                while (c.moveToNext() && count < 100) {
                    val type = when (c.getInt(typeIdx)) {
                        CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                        CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                        CallLog.Calls.MISSED_TYPE   -> "MISSED"
                        else                        -> "UNKNOWN"
                    }
                    result.add(mapOf(
                        "number"          to (c.getString(numIdx)  ?: ""),
                        "name"            to (c.getString(nameIdx) ?: ""),
                        "callType"        to type,
                        "durationSeconds" to c.getInt(durIdx),
                        "timestamp"       to c.getLong(dateIdx),
                    ))
                    count++
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Guardian", "getCallLog error: ${e.message}")
        }

        android.util.Log.d("Guardian", "getCallLog: ${result.size} entries")
        return result
    }

    // ── SMS: last 7 days, max 200 entries ─────────────────────────────────
    fun getSmsLog(context: Context): List<Map<String, Any>> {
        val result       = mutableListOf<Map<String, Any>>()
        val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000

        try {
            val cursor = context.contentResolver.query(
                Uri.parse("content://sms"),
                arrayOf("address", "body", "type", "date"),
                "date >= ?",
                arrayOf(sevenDaysAgo.toString()),
                "date DESC"
            ) ?: return result

            cursor.use { c ->
                val addrIdx = c.getColumnIndex("address")
                val bodyIdx = c.getColumnIndex("body")
                val typeIdx = c.getColumnIndex("type")
                val dateIdx = c.getColumnIndex("date")

                var count = 0
                while (c.moveToNext() && count < 200) {
                    val type = when (c.getInt(typeIdx)) {
                        1    -> "INBOX"
                        2    -> "SENT"
                        else -> "OTHER"
                    }
                    result.add(mapOf(
                        "address"   to (c.getString(addrIdx) ?: ""),
                        "body"      to (c.getString(bodyIdx) ?: ""),
                        "smsType"   to type,
                        "timestamp" to c.getLong(dateIdx),
                    ))
                    count++
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Guardian", "getSmsLog error: ${e.message}")
        }

        android.util.Log.d("Guardian", "getSmsLog: ${result.size} entries")
        return result
    }
}
