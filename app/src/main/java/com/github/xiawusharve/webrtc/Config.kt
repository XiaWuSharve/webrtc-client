package com.github.xiawusharve.webrtc

import androidx.compose.foundation.text.input.TextFieldState

data class Config(
    var localId: String,
    var remoteId: String,
    var displayName: String,
)