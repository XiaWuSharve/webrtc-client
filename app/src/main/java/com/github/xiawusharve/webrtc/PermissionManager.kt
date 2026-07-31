package com.github.xiawusharve.webrtc

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.permissionx.guolindev.PermissionX

class PermissionManager(private val activity: FragmentActivity) {
    private val permissionMediator = PermissionX.init(activity)

    companion object {
        const val TAG = "PermissionManager"
    }

    private fun requestPermissions(vararg permissionStrings: String, callback: (deniedList: List<String>) -> Unit = {}) {
        Log.i(TAG, "正在获取权限${permissionStrings.contentToString()}")
            permissionMediator
            .permissions(
                *permissionStrings
            )
            .request { allGranted, _, deniedList ->
                if (allGranted) {
                    Log.i(TAG, "所有权限已授予")
                } else {
                    Log.e(TAG, "acquire permission failed: $deniedList")
                    callback(deniedList)
                }
            }
    }
}