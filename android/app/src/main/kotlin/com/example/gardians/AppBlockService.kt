package com.example.gardians

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class AppBlockService : Service() {

    private val handler   = Handler(Looper.getMainLooper())
    private var tickCount = 0

    // Screen time state
    private var screenTimeLimitMinutes  = 0
    private var cachedTotalUsageMinutes = 0

    companion object {
        // CopyOnWriteArraySet: thread-safe reads on main thread +
        // writes from background network threads without ConcurrentModificationException.
        val blockedPackages: MutableSet<String> =
            java.util.concurrent.CopyOnWriteArraySet()
    }

    private val checkRunnable = object : Runnable {
        override fun run() {
            checkAndBlock()
            tickCount++

            // ⚠️ Use separate `if` blocks — NOT `when`.
            // `when` only fires the FIRST matching branch, so every multiple
            // of 30 (also a multiple of 10) would only ever call pingBackend().

            // Every 5 s — ping + fetch blocked list (fast block response)
            if (tickCount % 5 == 0) {
                pingBackend()
            }

            // Every 10 s — refresh usage cache for screen-time check
            if (tickCount % 10 == 0) {
                updateCachedTotalUsage()
            }

            // Every 30 s — persist usage + send to backend
            if (tickCount % 30 == 0) {
                saveUsageToPrefs()
                sendUsageToBackend()
            }

            // Every 5 min — send installed apps list
            if (tickCount % 300 == 0) {
                saveInstalledAppsToPrefs()
                sendInstalledAppsToBackend()
            }

            // Every 10 min — fetch screen time limit + send comms
            if (tickCount % 600 == 0) {
                fetchScreenTimeLimitFromBackend()
                sendCommsToBackend()
            }

            handler.postDelayed(this, 1000)
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("FlutterSharedPreferences", MODE_PRIVATE)

        // Restore blocked apps
        val appsFromIntent = intent?.getStringArrayExtra("blockedApps")
        if (appsFromIntent != null) {
            blockedPackages.addAll(appsFromIntent)
        } else {
            val saved = prefs.getString("flutter.blocked_apps", "") ?: ""
            if (saved.isNotEmpty()) blockedPackages.addAll(saved.split(","))
        }

        // Restore screen time limit
        screenTimeLimitMinutes = prefs.getInt("flutter.screen_time_limit", 0)

        startForeground(2, buildNotification())
        handler.post(checkRunnable)

        // Initial data burst 10 s after start
        handler.postDelayed({
            pingBackend()
            sendInstalledAppsToBackend()
            sendUsageToBackend()
            fetchScreenTimeLimitFromBackend()
            sendCommsToBackend()
            updateCachedTotalUsage()
        }, 10_000)

        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Block check (runs every second) ───────────────────────────────────

    private fun checkAndBlock() {
        val currentApp = AppUsageService.getCurrentApp(this) ?: return
        if (currentApp == packageName)    return
        if (BlockedAppActivity.isShowing) return

        // Log every 5 s (every 5th call) to avoid flooding logcat
        if (tickCount % 5 == 0) {
            android.util.Log.d("Guardian",
                "checkAndBlock: app=$currentApp blocked=${blockedPackages.contains(currentApp)} list=$blockedPackages")
        }

        // Screen time limit takes priority over individual blocks
        if (screenTimeLimitMinutes > 0 && cachedTotalUsageMinutes >= screenTimeLimitMinutes) {
            startActivity(Intent(this, BlockedAppActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("isScreenTimeLimit", true)
                putExtra("usedMinutes",  cachedTotalUsageMinutes)
                putExtra("limitMinutes", screenTimeLimitMinutes)
            })
            return
        }

        if (blockedPackages.contains(currentApp)) {
            val appName = try {
                val ai = packageManager.getApplicationInfo(currentApp, 0)
                packageManager.getApplicationLabel(ai).toString()
            } catch (_: Exception) { currentApp.substringAfterLast('.') }

            startActivity(Intent(this, BlockedAppActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                putExtra("isScreenTimeLimit", false)
                putExtra("blockedApp", currentApp)
                putExtra("appName",    appName)
            })
            android.util.Log.d("Guardian", "Blocked: $appName")
        }
    }

    // ── Usage cache (called every 30 s off main thread) ───────────────────

    private fun updateCachedTotalUsage() {
        Thread {
            val apps = AppUsageService.getUsedApps(this)
            cachedTotalUsageMinutes = apps.sumOf { (it["usageTimeMinutes"] as? Int) ?: 0 }
            android.util.Log.d("Guardian",
                "Usage: ${cachedTotalUsageMinutes}min / limit: ${screenTimeLimitMinutes}min")
        }.start()
    }

    // ── Screen time limit ─────────────────────────────────────────────────

    private fun fetchScreenTimeLimitFromBackend() {
        Thread {
            try {
                val prefs    = getSharedPreferences("FlutterSharedPreferences", MODE_PRIVATE)
                val deviceId = prefs.getString("flutter.my_device_id", "") ?: ""
                if (deviceId.isEmpty()) return@Thread
                val token    = prefs.getString("flutter.jwt_token",    "") ?: ""

                val url  = java.net.URL("http://192.168.100.7:8080/devices/$deviceId/settings")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.connectTimeout = 5000
                conn.readTimeout    = 5000

                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()
                    val json  = org.json.JSONObject(body)
                    val limit = json.optInt("screenTimeLimitMinutes", 0)
                    screenTimeLimitMinutes = limit
                    prefs.edit().putInt("flutter.screen_time_limit", limit).apply()
                    android.util.Log.d("Guardian", "Screen time limit fetched: ${limit}min")
                } else {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                android.util.Log.e("Guardian", "fetchSettings error: ${e.message}")
            }
        }.start()
    }

    // ── Comms (calls + SMS) ───────────────────────────────────────────────

    private fun sendCommsToBackend() {
        Thread {
            try {
                val prefs    = getSharedPreferences("FlutterSharedPreferences", MODE_PRIVATE)
                val deviceId = prefs.getString("flutter.my_device_id", "") ?: ""
                if (deviceId.isEmpty()) return@Thread
                val token    = prefs.getString("flutter.jwt_token",    "") ?: ""

                // Call log
                if (checkSelfPermission(android.Manifest.permission.READ_CALL_LOG)
                    == PackageManager.PERMISSION_GRANTED) {
                    val calls = CommunicationsService.getCallLog(this)
                    if (calls.isNotEmpty()) {
                        val arr = org.json.JSONArray()
                        calls.forEach { c ->
                            arr.put(org.json.JSONObject().apply {
                                put("number",          c["number"] ?: "")
                                put("name",            c["name"]   ?: "")
                                put("callType",        c["callType"] ?: "")
                                put("durationSeconds", c["durationSeconds"] ?: 0)
                                put("timestamp",       c["timestamp"] ?: 0L)
                            })
                        }
                        postJson("http://192.168.100.7:8080/comms/$deviceId/calls",
                            org.json.JSONObject().put("calls", arr).toString(), token)
                        android.util.Log.d("Guardian", "Sent ${calls.size} calls")
                    }
                }

                // SMS
                if (checkSelfPermission(android.Manifest.permission.READ_SMS)
                    == PackageManager.PERMISSION_GRANTED) {
                    val msgs = CommunicationsService.getSmsLog(this)
                    if (msgs.isNotEmpty()) {
                        val arr = org.json.JSONArray()
                        msgs.forEach { m ->
                            arr.put(org.json.JSONObject().apply {
                                put("address",   m["address"]   ?: "")
                                put("body",      m["body"]      ?: "")
                                put("smsType",   m["smsType"]   ?: "")
                                put("timestamp", m["timestamp"] ?: 0L)
                            })
                        }
                        postJson("http://192.168.100.7:8080/comms/$deviceId/sms",
                            org.json.JSONObject().put("messages", arr).toString(), token)
                        android.util.Log.d("Guardian", "Sent ${msgs.size} SMS")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Guardian", "sendComms error: ${e.message}")
            }
        }.start()
    }

    // ── Usage ─────────────────────────────────────────────────────────────

    private fun saveUsageToPrefs() {
        try {
            val apps   = AppUsageService.getUsedApps(this)
            val prefs  = getSharedPreferences("FlutterSharedPreferences", MODE_PRIVATE)
            val jArray = org.json.JSONArray()
            apps.forEach { app ->
                jArray.put(org.json.JSONObject().apply {
                    put("packageName",      app["packageName"] ?: "")
                    put("appName",          app["appName"]     ?: "")
                    put("usageTimeMinutes", app["usageTimeMinutes"] ?: 0)
                })
            }
            prefs.edit()
                .putString("flutter.used_apps_json", jArray.toString())
                .putLong("flutter.used_apps_timestamp", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            android.util.Log.e("Guardian", "saveUsage error: ${e.message}")
        }
    }

    private fun saveInstalledAppsToPrefs() {
        try {
            val apps   = AppUsageService.getInstalledApps(this)
            val prefs  = getSharedPreferences("FlutterSharedPreferences", MODE_PRIVATE)
            val jArray = org.json.JSONArray()
            apps.forEach { app ->
                jArray.put(org.json.JSONObject().apply {
                    put("packageName", app["packageName"] ?: "")
                    put("appName",     app["appName"]     ?: "")
                })
            }
            prefs.edit().putString("flutter.installed_apps_json", jArray.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.e("Guardian", "saveInstalled error: ${e.message}")
        }
    }

    private fun sendUsageToBackend() {
        Thread {
            try {
                val prefs    = getSharedPreferences("FlutterSharedPreferences", MODE_PRIVATE)
                val deviceId = prefs.getString("flutter.my_device_id", "") ?: ""
                if (deviceId.isEmpty()) return@Thread
                val token    = prefs.getString("flutter.jwt_token",    "") ?: ""

                val apps = AppUsageService.getUsedApps(this)
                if (apps.isEmpty()) return@Thread

                val arr = org.json.JSONArray()
                apps.forEach { app ->
                    arr.put(org.json.JSONObject().apply {
                        put("packageName",      app["packageName"] ?: "")
                        put("appName",          app["appName"]     ?: "")
                        put("usageTimeMinutes", app["usageTimeMinutes"] ?: 0)
                    })
                }
                postJson("http://192.168.100.7:8080/apps/$deviceId/used",
                    org.json.JSONObject().put("apps", arr).toString(), token)
                android.util.Log.d("Guardian", "Sent ${apps.size} usage records")
            } catch (e: Exception) {
                android.util.Log.e("Guardian", "sendUsage error: ${e.message}")
            }
        }.start()
    }

    private fun sendInstalledAppsToBackend() {
        Thread {
            try {
                val prefs    = getSharedPreferences("FlutterSharedPreferences", MODE_PRIVATE)
                val deviceId = prefs.getString("flutter.my_device_id", "") ?: ""
                if (deviceId.isEmpty()) return@Thread
                val token    = prefs.getString("flutter.jwt_token",    "") ?: ""

                val apps = AppUsageService.getInstalledApps(this)
                if (apps.isEmpty()) return@Thread

                val arr = org.json.JSONArray()
                apps.forEach { app ->
                    arr.put(org.json.JSONObject().apply {
                        put("packageName", app["packageName"] ?: "")
                        put("appName",     app["appName"]     ?: "")
                    })
                }
                postJson("http://192.168.100.7:8080/apps/$deviceId/installed",
                    org.json.JSONObject().put("apps", arr).toString(), token)
                android.util.Log.d("Guardian", "Sent ${apps.size} installed apps")
            } catch (e: Exception) {
                android.util.Log.e("Guardian", "sendInstalled error: ${e.message}")
            }
        }.start()
    }

    // ── Ping + blocked list ───────────────────────────────────────────────

    private fun pingBackend() {
        Thread {
            try {
                val prefs    = getSharedPreferences("FlutterSharedPreferences", MODE_PRIVATE)
                val deviceId = prefs.getString("flutter.my_device_id", "") ?: ""
                val token    = prefs.getString("flutter.jwt_token",    "") ?: ""
                if (deviceId.isEmpty()) {
                    android.util.Log.w("Guardian", "Ping: no deviceId in prefs")
                    return@Thread
                }

                // ── Heartbeat: read the response BODY, not just the status code.
                // The backend can embed blockedPackages + screenTimeLimitMinutes
                // directly in the heartbeat response so we don't need a second
                // network call just to get the blocked list.
                var heartbeatOk = false
                if (token.isNotEmpty()) {
                    for (endpoint in listOf("heartbeat", "ping")) {
                        try {
                            val method = if (endpoint == "heartbeat") "PATCH" else "POST"
                            val url  = java.net.URL("http://192.168.100.7:8080/devices/$deviceId/$endpoint")
                            val conn = url.openConnection() as java.net.HttpURLConnection
                            conn.requestMethod = method
                            conn.setRequestProperty("Authorization", "Bearer $token")
                            conn.connectTimeout = 5000; conn.readTimeout = 5000

                            val code = conn.responseCode
                            if (code in 200..299) {
                                // Read body — backend may return blockedPackages / settings here
                                val body = try { conn.inputStream.bufferedReader().readText() } catch (_: Exception) { null }
                                conn.disconnect()
                                applyServerConfig(body, prefs)
                                heartbeatOk = true
                                break
                            }
                            conn.disconnect()
                        } catch (_: Exception) { }
                    }
                }

                // ── Always fetch blocked apps via dedicated endpoint (belt + braces).
                // This covers backends that don't embed the list in the heartbeat.
                fetchAndApplyBlockedApps(deviceId, token)

                android.util.Log.d("Guardian",
                    "Ping done — heartbeat=$heartbeatOk blocked=${blockedPackages.size}")
            } catch (e: Exception) {
                android.util.Log.e("Guardian", "Ping error: ${e.message}")
            }
        }.start()
    }

    /**
     * Parse any JSON the backend returns (heartbeat body, settings, etc.)
     * and apply whatever fields are present.
     * Accepts multiple naming conventions so we're not fragile to backend style.
     */
    private fun applyServerConfig(body: String?, prefs: android.content.SharedPreferences) {
        if (body.isNullOrBlank()) return
        try {
            val json = org.json.JSONObject(body)

            // ── Blocked packages (backend can include in heartbeat response) ──
            val pkgArr = json.optJSONArray("blockedPackages")
                ?: json.optJSONArray("blocked_packages")
                ?: json.optJSONArray("packages")
            if (pkgArr != null) {
                val packages = List(pkgArr.length()) { pkgArr.getString(it) }
                blockedPackages.clear()
                blockedPackages.addAll(packages)
                prefs.edit()
                    .putString("flutter.blocked_apps",
                        if (packages.isEmpty()) "" else packages.joinToString(","))
                    .apply()
                android.util.Log.d("Guardian",
                    "applyServerConfig: ${packages.size} blocked → $packages")
            }

            // ── Screen time limit ────────────────────────────────────────────
            val limit = when {
                json.has("screenTimeLimitMinutes")  -> json.getInt("screenTimeLimitMinutes")
                json.has("screen_time_limit")        -> json.getInt("screen_time_limit")
                json.has("screenTimeLimit")          -> json.getInt("screenTimeLimit")
                else                                 -> -1
            }
            if (limit >= 0) {
                screenTimeLimitMinutes = limit
                prefs.edit().putInt("flutter.screen_time_limit", limit).apply()
            }
        } catch (_: Exception) { /* body might not be JSON, ignore */ }
    }

    private fun fetchAndApplyBlockedApps(deviceId: String, token: String) {
        try {
            val url  = java.net.URL("http://192.168.100.7:8080/apps/$deviceId/blocked")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connectTimeout = 5000; conn.readTimeout = 5000

            if (conn.responseCode !in 200..299) { conn.disconnect(); return }
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            // Parse robustly: accept {"packages":[...]} OR a plain array [...]
            // Never use `?: return` — an absent/null field means "empty list",
            // so we still clear blockedPackages (handles "Unblock All" correctly).
            val packages: List<String> = try {
                val arr = org.json.JSONObject(body).optJSONArray("packages")
                if (arr != null) List(arr.length()) { arr.getString(it) } else emptyList()
            } catch (_: Exception) {
                // Body is a plain JSON array, not an object
                try {
                    val arr = org.json.JSONArray(body)
                    List(arr.length()) { arr.getString(it) }
                } catch (_: Exception) { emptyList() }
            }

            blockedPackages.clear()
            blockedPackages.addAll(packages)

            val prefs = getSharedPreferences("FlutterSharedPreferences", MODE_PRIVATE)
            prefs.edit()
                .putString("flutter.blocked_apps",
                    if (packages.isEmpty()) "" else packages.joinToString(","))
                .apply()

            android.util.Log.d("Guardian",
                "fetchBlocked: applied ${packages.size} packages → $packages")
        } catch (e: Exception) {
            android.util.Log.e("Guardian", "fetchBlocked error: ${e.message}")
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun postJson(urlStr: String, body: String, token: String) {
        val conn = (java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
            doOutput      = true
            connectTimeout = 8000; readTimeout = 8000
        }
        conn.outputStream.use { it.write(body.toByteArray()) }
        conn.responseCode   // trigger
        conn.disconnect()
    }

    private fun buildNotification(): Notification {
        val channelId = "app_block_channel"
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(channelId, "App Control", NotificationManager.IMPORTANCE_LOW)
            )
        return Notification.Builder(this, channelId)
            .setContentTitle("Guardians")
            .setContentText("App monitoring active")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
    }
}
