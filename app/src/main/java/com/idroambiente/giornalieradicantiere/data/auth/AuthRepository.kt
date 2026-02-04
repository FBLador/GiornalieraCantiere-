package com.idroambiente.giornalieradicantiere.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

data class AuthUser(
    val name: String,
    val pin: String
)

class AuthRepository(
    private val dataStore: DataStore<Preferences>
) {
    private val nameKey = stringPreferencesKey("auth_user_name")
    private val pinKey = stringPreferencesKey("auth_user_pin")

    val user: Flow<AuthUser?> = dataStore.data.map { preferences ->
        val name = preferences[nameKey]
        val pin = preferences[pinKey]
        if (name.isNullOrBlank() || pin.isNullOrBlank()) {
            null
        } else {
            AuthUser(name, pin)
        }
    }

    suspend fun registerUser(name: String, pin: String) {
        dataStore.edit { preferences ->
            preferences[nameKey] = name
            preferences[pinKey] = pin
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val currentPin = dataStore.data.map { it[pinKey] }.map { it.orEmpty() }
        return currentPin.map { it == pin }.firstOrNull() ?: false
    }
}
