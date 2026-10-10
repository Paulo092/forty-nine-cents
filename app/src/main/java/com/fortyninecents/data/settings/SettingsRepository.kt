package com.fortyninecents.data.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferências do usuário. SharedPreferences basta para poucos booleanos; se crescer,
 * trocar por DataStore mantendo a mesma interface.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val _smsCaptureEnabled = MutableStateFlow(prefs.getBoolean(KEY_SMS_CAPTURE, false))

    /** Ler SMS de bancos. Desligado por padrão: SMS pode ser enviado por qualquer pessoa. */
    val smsCaptureEnabled: StateFlow<Boolean> = _smsCaptureEnabled.asStateFlow()

    fun setSmsCaptureEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SMS_CAPTURE, enabled) }
        _smsCaptureEnabled.value = enabled
    }

    private companion object {
        const val FILE = "settings"
        const val KEY_SMS_CAPTURE = "sms_capture_enabled"
    }
}
