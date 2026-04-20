package com.example.gardians

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import java.util.Calendar

object AppUsageService {

    // ── Packages we never want to report (device utilities, not apps) ────
    private val EXCLUDED_PACKAGES = setOf(
        "com.android.settings",
        "com.android.systemui",
        "com.android.phone",
        "com.android.launcher",
        "com.android.launcher2",
        "com.android.launcher3",
        "com.sec.android.app.launcher",          // Samsung One UI Home
        "com.samsung.android.app.cocktailbarservice",
        "com.samsung.android.devicecare",
        "com.samsung.android.app.settings.bixby",
        "com.samsung.android.bixby.agent",
        "com.samsung.android.bixby.wakeup",
        "com.samsung.android.app.smartcapture",
        "com.samsung.android.app.spage",         // Samsung Free
        "com.samsung.android.incallui",
        "com.samsung.android.dialer",
    )

    private val EXCLUDED_PREFIXES = listOf(
        "com.android.inputmethod",
        "com.google.android.inputmethod",
        "com.samsung.android.ime",
    )

    private fun isExcluded(pkg: String): Boolean {
        if (pkg in EXCLUDED_PACKAGES) return true
        return EXCLUDED_PREFIXES.any { pkg.startsWith(it) }
    }

    // ── getInstalledApps ─────────────────────────────────────────────────
    // Returns every app that has a launcher icon — user-installed AND
    // pre-installed apps the parent might want to block (YouTube, Chrome…).
    // Pure background/service apps without a launcher are excluded.
    fun getInstalledApps(context: Context): List<Map<String, Any>> {
        val pm     = context.packageManager
        val result = mutableListOf<Map<String, Any>>()

        // GET_META_DATA is not needed and makes the query heavier — use 0
        pm.getInstalledApplications(0).forEach { appInfo ->
            try {
                val pkg = appInfo.packageName
                if (pkg == context.packageName) return@forEach
                if (isExcluded(pkg))             return@forEach

                // Must have a launcher icon visible to the user
                pm.getLaunchIntentForPackage(pkg) ?: return@forEach

                val appName = pm.getApplicationLabel(appInfo).toString()
                result.add(mapOf("packageName" to pkg, "appName" to appName))
            } catch (_: Exception) { /* package removed mid-query */ }
        }

        android.util.Log.d("Guardian", "getInstalledApps: ${result.size} apps")
        return result.sortedBy { it["appName"] as String }
    }

    // ── getUsedApps ──────────────────────────────────────────────────────
    // Returns apps used today (since midnight) with usage > 0.
    // Tries INTERVAL_DAILY first; falls back to computing from raw events
    // (needed on some Samsung One UI versions where INTERVAL_DAILY returns 0).
    fun getUsedApps(context: Context): List<Map<String, Any>> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE)
                as UsageStatsManager
        val pm  = context.packageManager

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE,      0)
            set(Calendar.SECOND,      0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val now        = System.currentTimeMillis()

        // ── Primary: INTERVAL_DAILY from midnight ───────────────────────
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now)

        val usageMap = mutableMapOf<String, Long>() // pkg → ms in foreground

        if (!stats.isNullOrEmpty()) {
            stats.forEach { s ->
                if (s.totalTimeInForeground > 0) {
                    usageMap[s.packageName] =
                        (usageMap[s.packageName] ?: 0L) + s.totalTimeInForeground
                }
            }
        }

        // ── Fallback: calculate from raw ACTIVITY events ────────────────
        // Used when INTERVAL_DAILY returns nothing (common on Samsung/Xiaomi)
        if (usageMap.isEmpty()) {
            android.util.Log.w("Guardian", "INTERVAL_DAILY empty — using event fallback")
            val events   = usm.queryEvents(startOfDay, now)
            val event    = UsageEvents.Event()
            val resumeAt = mutableMapOf<String, Long>()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED ->
                        resumeAt[event.packageName] = event.timeStamp
                    UsageEvents.Event.ACTIVITY_PAUSED  -> {
                        val start = resumeAt.remove(event.packageName) ?: continue
                        val ms    = event.timeStamp - start
                        if (ms > 0) usageMap[event.packageName] =
                            (usageMap[event.packageName] ?: 0L) + ms
                    }
                }
            }
            // Add still-running apps (resumed but never paused yet)
            resumeAt.forEach { (pkg, start) ->
                val ms = now - start
                if (ms > 0) usageMap[pkg] = (usageMap[pkg] ?: 0L) + ms
            }
        }

        // ── Build result list ───────────────────────────────────────────
        val result = mutableListOf<Map<String, Any>>()
        usageMap.entries
            .filter { it.value >= 60_000L }          // at least 1 minute
            .sortedByDescending { it.value }
            .take(40)
            .forEach { (pkg, ms) ->
                try {
                    if (pkg == context.packageName) return@forEach
                    if (isExcluded(pkg))             return@forEach

                    pm.getLaunchIntentForPackage(pkg) ?: return@forEach

                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val minutes = (ms / 60_000L).toInt()

                    result.add(mapOf(
                        "packageName"      to pkg,
                        "appName"          to appName,
                        "usageTimeMinutes" to minutes,
                    ))
                } catch (_: Exception) { /* package removed */ }
            }

        android.util.Log.d("Guardian", "getUsedApps: ${result.size} apps, map size=${usageMap.size}")
        return result
    }

    // ── getCurrentApp ────────────────────────────────────────────────────
    // Returns the package currently in the foreground, or null.
    //
    // Algorithm: take the LAST ACTIVITY_RESUMED event in the past 10 minutes.
    // Events are returned in chronological order, so the last RESUMED is the
    // most-recently-opened app.
    //
    // Why NOT use RESUMED-without-PAUSED filtering?
    // Many apps (YouTube, TikTok, Instagram) use SplashActivity → MainActivity
    // transitions where the lifecycle order is:
    //   MainActivity  RESUMED (t1)
    //   SplashActivity PAUSED  (t2)   ← t2 > t1 for the SAME package
    // This makes the package look "paused" even though it is in the foreground.
    //
    // Instead, after finding the last RESUMED package we check TWO things:
    //  1. If it is the HOME launcher → user pressed HOME → return null.
    //  2. If it has no launcher icon  → it is a system service → return null.
    fun getCurrentApp(context: Context): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE)
                as UsageStatsManager
        val pm  = context.packageManager

        val now       = System.currentTimeMillis()
        val startTime = now - 10L * 60 * 1000   // 10-minute lookback

        val events = usm.queryEvents(startTime, now)
        val event  = UsageEvents.Event()

        var lastPkg: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType != UsageEvents.Event.ACTIVITY_RESUMED) continue
            val pkg = event.packageName
            if (pkg == context.packageName) continue
            lastPkg = pkg   // keep updating — last value is the most recent
        }

        if (lastPkg == null) return null

        // Exclude the home-screen launcher (user pressed HOME button)
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val homePkg = pm.resolveActivity(homeIntent, 0)?.activityInfo?.packageName
        if (lastPkg == homePkg) return null

        // Exclude system services/background apps that have no launcher icon
        if (pm.getLaunchIntentForPackage(lastPkg) == null) return null

        return lastPkg
    }
}
