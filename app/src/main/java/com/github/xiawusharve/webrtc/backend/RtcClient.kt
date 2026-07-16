package com.github.xiawusharve.webrtc.backend

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class RtcClient: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("RtcClient", "trigger onReceive")
    }
}