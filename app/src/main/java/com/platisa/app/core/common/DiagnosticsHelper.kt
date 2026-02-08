package com.platisa.app.core.common

import android.content.Context
import android.content.pm.PackageManager
import java.security.MessageDigest

object DiagnosticsHelper {
    fun logAppSignature(context: Context) {
        try {
            val packageName = context.packageName
            android.util.Log.d("Diagnostics", "📦 App Package Name: $packageName")
            
            val info = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }

            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners
            } else {
                info.signatures
            }

            signatures?.forEach { signature ->
                val md = MessageDigest.getInstance("SHA-1")
                md.update(signature.toByteArray())
                val digest = md.digest()
                val hexString = StringBuilder()
                for (b in digest) {
                    hexString.append(String.format("%02X:", b))
                }
                if (hexString.isNotEmpty()) hexString.setLength(hexString.length - 1)
                
                android.util.Log.d("Diagnostics", "🔑 App SHA-1: $hexString")
                android.util.Log.d("Diagnostics", "ℹ️ Ensure this SHA-1 is added to Firebase Console -> Project Settings -> General.")
            }
        } catch (e: Exception) {
            android.util.Log.e("Diagnostics", "Failed to get signature", e)
        }
    }
}
