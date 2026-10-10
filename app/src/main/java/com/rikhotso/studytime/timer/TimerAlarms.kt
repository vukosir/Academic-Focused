package com.rikhotso.studytime.timer

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rikhotso.studytime.MainActivity
import com.rikhotso.studytime.R
import com.rikhotso.studytime.data.Settings
import com.rikhotso.studytime.data.TimerKind
import com.rikhotso.studytime.data.TimerMode
import com.rikhotso.studytime.data.TimerSound
import com.rikhotso.studytime.data.TimerState

/**
 * Lets the ViewModel book the "time's up" alarms without knowing about Android.
 * Pass null for a phase that should not ring.
 */
interface AlarmScheduler {
    fun schedule(focusEndsAt: Long?, breakEndsAt: Long?, breakMinutes: Int, sound: TimerSound, notify: Boolean)
    fun cancelAll()
}

/**
 * Books the alarms that match [timer], or clears them when nothing is counting down.
 * Used after every timer change and again when the phone restarts.
 */
fun bookTimerAlarms(alarms: AlarmScheduler, timer: TimerState, settings: Settings) {
    val countingUp = timer.kind == TimerKind.Stopwatch && timer.mode == TimerMode.Focus
    if (!timer.running || timer.mode == TimerMode.Idle || countingUp) {
        alarms.cancelAll()
        return
    }
    if (timer.mode == TimerMode.Focus) {
        val breakRings = timer.breakMinutes > 0 && settings.autoStartBreak
        alarms.schedule(
            focusEndsAt = timer.endsAt,
            breakEndsAt = if (breakRings) timer.endsAt + timer.breakMinutes * 60_000L else null,
            breakMinutes = timer.breakMinutes,
            sound = settings.timerSound,
            notify = settings.notifyTimer,
        )
    } else {
        alarms.schedule(focusEndsAt = null, breakEndsAt = timer.endsAt, breakMinutes = timer.breakMinutes, sound = settings.timerSound, notify = settings.notifyTimer)
    }
}

/** Whether the app may post notifications. From Android 13 the user has to allow it. */
fun canNotify(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED

/** A PendingIntent that opens studytime when a notification is tapped. */
fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
    context,
    0,
    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

/** Sets an alarm that is exact when the system allows it, and close to exact otherwise. */
fun setAlarm(alarmManager: AlarmManager, atMillis: Long, intent: PendingIntent) {
    val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    if (exactAllowed) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, intent)
    } else {
        // Without the exact alarm permission the system may deliver this a little late.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, intent)
    }
}

/**
 * Books exact alarms with the system AlarmManager, so the end of a round is announced on time
 * even when the screen is off or studytime has been swiped away.
 */
class AndroidAlarmScheduler(context: Context) : AlarmScheduler {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(focusEndsAt: Long?, breakEndsAt: Long?, breakMinutes: Int, sound: TimerSound, notify: Boolean) {
        cancelAll()
        if (focusEndsAt != null) {
            val text = if (breakMinutes > 0) "Time for a $breakMinutes minute break." else "Nice work. Start another round when you are ready."
            setAlarm(alarmManager, focusEndsAt, pendingIntent(REQUEST_FOCUS, "Focus session finished", text, sound, notify))
        }
        if (breakEndsAt != null) {
            setAlarm(alarmManager, breakEndsAt, pendingIntent(REQUEST_BREAK, "Break's over", "Ready for another round?", sound, notify))
        }
    }

    override fun cancelAll() {
        alarmManager.cancel(pendingIntent(REQUEST_FOCUS, "", "", TimerSound.Off, false))
        alarmManager.cancel(pendingIntent(REQUEST_BREAK, "", "", TimerSound.Off, false))
        NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID)
    }

    private fun pendingIntent(requestCode: Int, title: String, text: String, sound: TimerSound, notify: Boolean): PendingIntent {
        val intent = Intent(appContext, TimerAlarmReceiver::class.java)
            .putExtra(EXTRA_TITLE, title)
            .putExtra(EXTRA_TEXT, text)
            .putExtra(EXTRA_SOUND, sound.key)
            .putExtra(EXTRA_NOTIFY, notify)
        return PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        const val EXTRA_SOUND = "sound"
        const val EXTRA_NOTIFY = "notify"

        /** The first version used "focus_timer", which played the phone's own sound as well. */
        const val OLD_CHANNEL_ID = "focus_timer"
        const val CHANNEL_ID = "focus_timer_quiet"
        const val NOTIFICATION_ID = 1
        private const val REQUEST_FOCUS = 1001
        private const val REQUEST_BREAK = 1002
    }
}

/** Plays the short tone the user picked in Settings. It follows the phone's notification volume. */
object TimerTones {
    fun play(sound: TimerSound, onDone: () -> Unit = {}) {
        if (sound == TimerSound.Off) {
            onDone()
            return
        }
        try {
            val generator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            val tone = when (sound) {
                TimerSound.Bell -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
                TimerSound.Beep -> ToneGenerator.TONE_PROP_PROMPT
                TimerSound.DoubleBeep -> ToneGenerator.TONE_PROP_BEEP2
                else -> ToneGenerator.TONE_PROP_ACK
            }
            generator.startTone(tone, 900)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    generator.release()
                } catch (e: Exception) {
                    // Already released.
                }
                onDone()
            }, 1100)
        } catch (e: Exception) {
            // Some phones refuse a tone generator while another app holds the audio. Stay silent.
            onDone()
        }
    }
}

/**
 * Runs when a timer alarm fires. It plays the chosen tone and shows a silent notification, so
 * the sound is the one picked in Settings and never doubles up. Tapping the notification opens
 * studytime.
 */
class TimerAlarmReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission") // canNotify() is checked just before notify() below.
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_TITLE) ?: return
        val text = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_TEXT).orEmpty()
        if (title.isBlank()) return

        // Keep the receiver alive until the tone has finished.
        val pending = goAsync()
        TimerTones.play(TimerSound.fromKey(intent.getStringExtra(AndroidAlarmScheduler.EXTRA_SOUND))) {
            try {
                pending?.finish()
            } catch (e: Exception) {
                // The system already closed the broadcast.
            }
        }

        if (!intent.getBooleanExtra(AndroidAlarmScheduler.EXTRA_NOTIFY, true)) return
        val manager = NotificationManagerCompat.from(context)
        manager.deleteNotificationChannel(AndroidAlarmScheduler.OLD_CHANNEL_ID)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(AndroidAlarmScheduler.CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.timer_channel_name))
                .setDescription(context.getString(R.string.timer_channel_description))
                .setSound(null, null)
                .build(),
        )
        val notification = NotificationCompat.Builder(context, AndroidAlarmScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSilent(true)
            .setContentIntent(openAppIntent(context))
            .build()
        if (canNotify(context)) {
            manager.notify(AndroidAlarmScheduler.NOTIFICATION_ID, notification)
        }
    }
}
