package com.example.gardians

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class BlockedAppActivity : Activity() {

    companion object {
        var isShowing = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isShowing = true

        // Keep screen on and show above lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val isScreenTimeLimit = intent.getBooleanExtra("isScreenTimeLimit", false)

        val root = if (isScreenTimeLimit) {
            val usedMinutes  = intent.getIntExtra("usedMinutes",  0)
            val limitMinutes = intent.getIntExtra("limitMinutes", 0)
            buildScreenTimeLimitUI(usedMinutes, limitMinutes)
        } else {
            val blockedApp = intent.getStringExtra("blockedApp") ?: "This app"
            val appName    = intent.getStringExtra("appName") ?: blockedApp.substringAfterLast('.')
            buildUI(appName)
        }
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        isShowing = true
    }

    override fun onStop() {
        super.onStop()
        isShowing = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isShowing = false
    }

    // Back button → go HOME, never return to the blocked app
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(home)
    }

    // ── Build the full-screen block UI programmatically ───────────────────
    private fun buildUI(appName: String): FrameLayout {
        val ctx = this

        // Root: dark navy background
        val root = FrameLayout(ctx).apply {
            setBackgroundColor(Color.parseColor("#042459"))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Centre column
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity     = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Lock icon (built-in Android drawable — no asset needed)
        val icon = ImageView(ctx).apply {
            setImageResource(android.R.drawable.ic_lock_lock)
            setColorFilter(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(160, 160).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 48
            }
        }

        val title = TextView(ctx).apply {
            text     = "App Blocked"
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity  = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity      = Gravity.CENTER_HORIZONTAL
                bottomMargin = 16
            }
        }

        val subtitle = TextView(ctx).apply {
            text     = "\"$appName\" has been blocked\nby your parent."
            textSize = 16f
            setTextColor(Color.parseColor("#99BBDDFF"))
            gravity  = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity      = Gravity.CENTER_HORIZONTAL
                bottomMargin = 60
            }
        }

        val badge = TextView(ctx).apply {
            text     = "🛡️ Guardians Parental Control"
            textSize = 12f
            setTextColor(Color.parseColor("#559ED7EB"))
            gravity  = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER_HORIZONTAL }
        }

        col.addView(icon)
        col.addView(title)
        col.addView(subtitle)
        col.addView(badge)
        root.addView(col)
        return root
    }

    private fun buildScreenTimeLimitUI(usedMinutes: Int, limitMinutes: Int): FrameLayout {
        val ctx = this

        val root = FrameLayout(ctx).apply {
            setBackgroundColor(Color.parseColor("#042459"))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity     = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val icon = ImageView(ctx).apply {
            setImageResource(android.R.drawable.ic_lock_lock)
            setColorFilter(Color.parseColor("#FF9800"))
            layoutParams = LinearLayout.LayoutParams(160, 160).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 48
            }
        }

        val title = TextView(ctx).apply {
            text     = "Screen Time Limit Reached"
            textSize = 26f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity  = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity      = Gravity.CENTER_HORIZONTAL
                bottomMargin = 24
            }
        }

        fun formatMinutes(mins: Int): String {
            val h = mins / 60
            val m = mins % 60
            return if (h > 0) "${h}h ${m}m" else "${m}m"
        }

        val subtitle = TextView(ctx).apply {
            text     = "You've used ${formatMinutes(usedMinutes)} today\nDaily limit: ${formatMinutes(limitMinutes)}"
            textSize = 16f
            setTextColor(Color.parseColor("#99BBDDFF"))
            gravity  = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity      = Gravity.CENTER_HORIZONTAL
                bottomMargin = 60
            }
        }

        val badge = TextView(ctx).apply {
            text     = "🛡️ Guardians Parental Control"
            textSize = 12f
            setTextColor(Color.parseColor("#559ED7EB"))
            gravity  = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER_HORIZONTAL }
        }

        col.addView(icon)
        col.addView(title)
        col.addView(subtitle)
        col.addView(badge)
        root.addView(col)
        return root
    }
}
