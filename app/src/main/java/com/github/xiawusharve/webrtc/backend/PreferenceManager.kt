package com.github.xiawusharve.webrtc.backend

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.xiawusharve.webrtc.Config
import com.github.xiawusharve.webrtc.MessagePreview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PreferenceManager(private val context: Context) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name="myPreference")
    private val localIdKey = stringPreferencesKey("localId")
    private val remoteIdKey = stringPreferencesKey("remoteId")
    private val displayNameKey = stringPreferencesKey("displayName")
    private val messagesKey = stringSetPreferencesKey("messages")
    suspend fun loadConfig(): Config {
        val localId = context.dataStore.data.map { it[localIdKey]?:"" }.first()
        val remoteId = context.dataStore.data.map { it[remoteIdKey]?:"" }.first()
        val displayName = context.dataStore.data.map { it[displayNameKey]?:"" }.first()
        return Config(localId, remoteId, displayName)
    }

    suspend fun loadMessageList(): List<MessagePreview> {
        val set = context.dataStore.data.map { it[messagesKey] ?: emptySet() }.first()
        return set.map { MessagePreview.parseFrom(it) }
    }

    suspend fun saveConfig(config: Config) {
        context.dataStore.edit {
            it[localIdKey] = config.localId
            it[remoteIdKey] = config.remoteId
            it[displayNameKey] = config.displayName
        }
    }

    suspend fun saveMessageList(messages: List<MessagePreview>) {
        context.dataStore.edit {
            it[messagesKey] = messages.map { it.toString() }.toSet()
        }
    }
}