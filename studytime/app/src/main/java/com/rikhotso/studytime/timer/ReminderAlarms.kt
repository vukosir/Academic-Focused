package com.rikhotso.studytime.timer

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rikhotso.studytime.R
import com.rikhotso.studytime.data.AppDataJson
import com.rikhotso.studytime.data.FileStorage
import com.rikhotso.studytime.data.MAX_REMINDERS
import com.rikhotso.studytime.data.Reminder
import com.rikhotso.studytime.data.planReminders
import java.io.File

/** The name of the save file inside the app's private storage. */
const val SAVE_FILE_NAME = "studytime.json"

/** Lets the ViewModel hand over the list of reminders without knowing about Android. */
interface ReminderScheduler {
    fun sync(reminders: List<Reminder>)
}

/**
 * Books reminders with the system AlarmManager. There are [MAX_REMINDERS] numbered slots: each
 * sync clears every slot and fills them again from the list, so a reminder for something that
 * was deleted or finished can never be left behind.
 */
class AndroidReminderScheduler(context: Context) : ReminderScheduler {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun sync(reminders: List<Reminder>) {
        for (slot in 0 until MAX_REMINDERS) {
            alarmManager.cancel(pendingIntent(slot, "", ""))
        }
        reminders.take(MAX_REMINDERS).forEachIndexed { slot, reminder ->
            setAlarm(alarmManager, reminder.atMillis, pendingIntent(slot, reminder.title, reminder.text))
        }
    }

    private fun pendingIntent(slot: Int, title: String, text: String): PendingIntent {
        val intent = Intent(appContext, ReminderReceiver::class.java)
            .putExtra(EXTRA_TITLE, title)
            .putExtra(EXTRA_TEXT, text)
            .putExtra(EXTRA_SLOT, slot)
        return PendingIntent.getBroadcast(
            appContext,
            FIRST_REQUEST_CODE + slot,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        const val EXTRA_SLOT = "slot"
        const val CHANNEL_ID = "study_reminders"
        const val FIRST_REQUEST_CODE = 2000
        const val FIRST_NOTIFICATION_ID = 100
    }
}

/**
 * Reads the save file and books the reminders for the next two weeks again, plus (when
 * [includeTimer] is set) any timer that is still counting down. Called when the phone restarts,
 * when the app is updated and each time a reminder fires, so the two week window keeps moving.
 */
fun rebookEverything(context: Context, includeTimer: Boolean) {
    try {
        val appContext = context.applicationContext
        val text = FileStorage(File(appContext.filesDir, SAVE_FILE_NAME)).read()
        val data = AppDataJson.decodeOrNull(text) ?: return
        AndroidReminderScheduler(appContext).sync(planReminders(data, System.currentTimeMillis()))
        if (includeTimer) bookTimerAlarms(AndroidAlarmScheduler(appContext), data.timer, data.settings)
    } catch (e: Exception) {
        // Reminders are rebuilt the next time studytime is opened.
    }
}

/** Shows one reminder. Tapping it opens studytime. */
class ReminderReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission") // canNotify() is checked just before notify() below.
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(AndroidReminderScheduler.EXTRA_TITLE).orEmpty()
        val text = intent.getStringExtra(AndroidReminderScheduler.EXTRA_TEXT).orEmpty()
        val slot = intent.getIntExtra(AndroidReminderScheduler.EXTRA_SLOT, 0)
        if (title.isNotBlank()) {
            val manager = NotificationManagerCompat.from(context)
            manager.createNotificationChannel(
                NotificationChannelCompat.Builder(AndroidReminderScheduler.CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName(context.getString(R.string.reminder_channel_name))
                    .setDescription(context.getString(R.string.reminder_channel_description))
                    .build(),
            )
            val notification = NotificationCompat.Builder(context, AndroidReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(openAppIntent(context))
                .build()
            if (canNotify(context)) {
                manager.notify(AndroidReminderScheduler.FIRST_NOTIFICATION_ID + slot, notification)
            }
        }
        rebookEverything(context, includeTimer = false)
    }
}

/** Alarms are wiped when the phone restarts or the app is updated, so they are booked again here. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.MY_PACKAGE_REPLACED") {
            rebookEverything(context, includeTimer = true)
        }
    }
}
