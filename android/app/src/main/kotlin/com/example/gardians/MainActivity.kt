package com.example.gardians

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import org.json.JSONArray

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.gardians/device_admin"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        
        super.configureFlutterEngine(flutterEngine)

        // شغّلي الـ AppBlockService تلقائياً عشان الـ usage reporting يشتغل في الـ background
        try {
            val serviceIntent = Intent(this, AppBlockService::class.java)
            startForegroundService(serviceIntent)
            android.util.Log.d("Guardian", "AppBlockService started!")
        } catch (e: Exception) {
            android.util.Log.e("Guardian", "Failed to start service: ${e.message}")
        }

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {

                    "isAdminActive" -> {
                        result.success(DeviceLockService.isAdminActive(this))
                    }

                    "requestAdminPermission" -> {
                        DeviceLockService.requestAdminPermission(this)
                        result.success(null)
                    }

                    "lockScreen" -> {
                        DeviceLockService.lockScreen(this)
                        result.success(null)
                    }

                    "startLockService" -> {
                        val intent = Intent(this, LockService::class.java)
                        startForegroundService(intent)
                        result.success(null)
                    }

                    "stopLockService" -> {
                        stopService(Intent(this, LockService::class.java))
                        result.success(null)
                    }

                    // ✅ الجديد بقى — حطيهم هنا 👇

                    "getUsedApps" -> {
                        try {
                            val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
                            val cachedJson = prefs.getString("flutter.used_apps_json", null)
                            val cacheAge = System.currentTimeMillis() - prefs.getLong("flutter.used_apps_timestamp", 0)
                            val cacheValid = cachedJson != null && cacheAge < 10 * 60 * 1000

                            android.util.Log.d("Guardian", "Getting used apps, cached: ${cachedJson?.take(100)}")

                            if (cacheValid) {
                                val jsonArray = JSONArray(cachedJson!!)
                                val apps = mutableListOf<Map<String, Any>>()
                                for (i in 0 until jsonArray.length()) {
                                    val obj = jsonArray.getJSONObject(i)
                                    apps.add(mapOf(
                                        "packageName" to obj.getString("packageName"),
                                        "appName" to obj.getString("appName"),
                                        "usageTimeMinutes" to obj.getInt("usageTimeMinutes")
                                    ))
                                }
                                android.util.Log.d("Guardian", "Returning ${apps.size} cached apps")
                                result.success(apps)
                            } else {
                                android.util.Log.d("Guardian", "No cache, fetching directly")
                                val apps = AppUsageService.getUsedApps(this)
                                val jsonArray = JSONArray()
                                for (app in apps) {
                                    val obj = org.json.JSONObject()
                                    obj.put("packageName", app["packageName"] ?: "")
                                    obj.put("appName", app["appName"] ?: "")
                                    obj.put("usageTimeMinutes", app["usageTimeMinutes"] ?: 0)
                                    jsonArray.put(obj)
                                }
                                prefs.edit()
                                    .putString("flutter.used_apps_json", jsonArray.toString())
                                    .putLong("flutter.used_apps_timestamp", System.currentTimeMillis())
                                    .apply()
                                result.success(apps)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("Guardian", "getUsedApps error: ${e.message}")
                            result.success(emptyList<Map<String, Any>>())
                        }
                    }

                    "getCurrentApp" -> {
                        result.success(AppUsageService.getCurrentApp(this))
                    }

                    "getInstalledApps" -> {
                        try {
                            val apps = AppUsageService.getInstalledApps(this)
                            android.util.Log.d("Guardian", "Returning ${apps.size} installed apps")
                            result.success(apps)
                        } catch (e: Exception) {
                            android.util.Log.e("Guardian", "getInstalledApps error: ${e.message}")
                            result.success(emptyList<Map<String, Any>>())
                        }
                    }

                    "blockApps" -> {
                        val apps = call.argument<List<String>>("apps") ?: emptyList()

                        AppBlockService.blockedPackages.clear()
                        AppBlockService.blockedPackages.addAll(apps)

                        // احفظ الـ blocked apps عشان الـ service يقراها لو restart
                        val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
                        prefs.edit().putString("flutter.blocked_apps", apps.joinToString(",")).apply()

                        val intent = Intent(this, AppBlockService::class.java).apply {
                            putExtra("blockedApps", apps.toTypedArray())
                        }

                        startForegroundService(intent)
                        result.success(null)
                    }

                    "unblockAllApps" -> {
                        AppBlockService.blockedPackages.clear()
                        stopService(Intent(this, AppBlockService::class.java))
                        result.success(null)
                    }

                    "checkUsagePermission" -> {
                        val granted = checkUsageStatsPermission()
                        result.success(granted)
                    }

                    "requestUsagePermission" -> {
                        val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        startActivity(intent)
                        result.success(null)
                    }

                    "checkCommsPermission" -> {
                        val callLogGranted = checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) ==
                                PackageManager.PERMISSION_GRANTED
                        val smsGranted = checkSelfPermission(android.Manifest.permission.READ_SMS) ==
                                PackageManager.PERMISSION_GRANTED
                        result.success(callLogGranted && smsGranted)
                    }

                    "requestCommsPermission" -> {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(
                                android.Manifest.permission.READ_CALL_LOG,
                                android.Manifest.permission.READ_SMS
                            ),
                            101
                        )
                        result.success(null)
                    }

                    else -> result.notImplemented()
                }
            }
    }

    // ✅ تحطي الـ helper function هنا (برا configureFlutterEngine)
    private fun checkUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }
}