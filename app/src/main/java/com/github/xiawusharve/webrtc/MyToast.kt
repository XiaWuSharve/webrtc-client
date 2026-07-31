package com.github.xiawusharve.webrtc

import android.content.Context
import android.widget.Toast

class MyToast(private val context: Context) {
    private val _permissionDenyList = fun (deniedList: List<String>): Toast {
        return Toast.makeText(
            context,
            "以下权限被拒绝: $deniedList",
            Toast.LENGTH_LONG
        )
    }

    fun permissionDenyList(deniedList: List<String>) {
        _permissionDenyList(deniedList).show()
    }
}