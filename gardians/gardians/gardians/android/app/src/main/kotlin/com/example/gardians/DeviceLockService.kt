package com.example.gardians

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object DeviceLockService {
    
    fun isAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) 
            as DevicePolicyManager
        val adminComponent = ComponentName(context, AdminReceiver::class.java)
        return dpm.isAdminActive(adminComponent)
    }

    fun requestAdminPermission(context: Context) {
        val adminComponent = ComponentName(context, AdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Guardians needs device admin to lock the screen for parental control.")
        }
        context.startActivity(intent)
    }

    fun lockScreen(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) 
            as DevicePolicyManager
        val adminComponent = ComponentName(context, AdminReceiver::class.java)
        if (dpm.isAdminActive(adminComponent)) {
            dpm.lockNow()
        }
    }
}