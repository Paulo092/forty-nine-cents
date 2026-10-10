package com.fortyninecents.notification

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.fortyninecents.FortyNineCentsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Recebe todas as notificações do sistema (após o usuário conceder o acesso) e repassa
 * apenas as de apps de bancos para o [NotificationProcessor].
 */
class BankNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val container
        get() = (application as FortyNineCentsApp).container

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val source = sbn.packageName
        val isSms = BankApps.isSms(source)
        if (source == packageName || !(BankApps.isBank(source) || isSms)) return
        if (isSms && !container.settings.smsCaptureEnabled.value) return

        val notification = sbn.notification ?: return
        // resumos de grupo repetem o conteúdo das notificações filhas
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = (
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
            )?.toString()

        if (title.isNullOrBlank() && text.isNullOrBlank()) return
        // em apps de SMS o título é o remetente: só aceitamos códigos curtos/nomes de banco
        if (isSms && !SmsSenderFilter.isTrusted(title)) return

        scope.launch {
            runCatching {
                container.notificationProcessor.process(
                    packageName = source,
                    sourceName = BankApps.displayName(source),
                    title = title,
                    text = text,
                    postedAt = sbn.postTime,
                )
            }.onSuccess { result ->
                Log.d(TAG, "Notificação de ${sbn.packageName}: ${result::class.simpleName}")
            }.onFailure { error ->
                Log.e(TAG, "Falha ao processar notificação de ${sbn.packageName}", error)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "BankNotificationListener"

        /** Se o usuário já liberou o acesso às notificações para o app. */
        fun isEnabled(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

        fun componentName(context: Context) = ComponentName(context, BankNotificationListenerService::class.java)
    }
}
