package com.nanxin.hrtrecorder

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/*
 * Android author watermark: Nanxin.
 * Local reminders never use network services. They wake the app locally, ask
 * the user to confirm, and only then write a real dose record.
 */

object ReminderScheduler {
    private const val CHANNEL_ID = "hrt_plan_reminders"
    private const val CHANNEL_NAME = "HRT Recorder reminders"
    const val ACTION_FIRE = "com.nanxin.hrtrecorder.ACTION_PLAN_REMINDER"
    const val ACTION_TAKEN = "com.nanxin.hrtrecorder.ACTION_PLAN_TAKEN"
    const val ACTION_SKIP = "com.nanxin.hrtrecorder.ACTION_PLAN_SKIP"
    const val ACTION_SNOOZE = "com.nanxin.hrtrecorder.ACTION_PLAN_SNOOZE"
    const val EXTRA_PLAN_ID = "planId"
    const val EXTRA_SCHEDULED_TIME_H = "scheduledTimeH"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Local medication plan reminders"
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun scheduleAll(context: Context, snapshot: AppStateSnapshot) {
        createChannel(context)
        snapshot.medicationPlans
            .filter { it.enabled && it.reminderEnabled }
            .forEach { plan ->
                nextOccurrenceTime(plan)?.let { scheduled ->
                    scheduleOccurrence(context, plan, scheduled, scheduled)
                }
            }
    }

    fun cancelPlan(context: Context, planId: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(alarmPendingIntent(context, planId, 0.0))
    }

    fun showReminder(context: Context, planId: String, scheduledTimeH: Double) {
        createChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val store = NativeStore(context.applicationContext)
        val snapshot = store.load()
        val language = store.loadAppLanguage()
        val plan = snapshot.medicationPlans.firstOrNull { it.id == planId && it.enabled } ?: return
        val notificationId = requestCode(planId)
        val openIntent = PendingIntent.getActivity(
            context,
            notificationId + 41,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(language.t("HRT 用药提醒", "HRT dose reminder"))
            .setContentText("${plan.displayName(language)} · ${plan.route.label(language)} · ${planDoseText(plan, language)}")
            .setStyle(
                Notification.BigTextStyle().bigText(
                    "${plan.displayName(language)}\n${plan.route.label(language)} · ${planDoseText(plan, language)}\n${language.t("确认已服用后才会写入正式记录。", "A real dose is logged only after you confirm it was taken.")}",
                ),
            )
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_save,
                language.t("已服用", "Taken"),
                actionPendingIntent(context, ACTION_TAKEN, planId, scheduledTimeH),
            )
            .addAction(
                android.R.drawable.ic_popup_sync,
                language.t("稍后", "Snooze"),
                actionPendingIntent(context, ACTION_SNOOZE, planId, scheduledTimeH),
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                language.t("跳过", "Skip"),
                actionPendingIntent(context, ACTION_SKIP, planId, scheduledTimeH),
            )
            .build()
        context.getSystemService(NotificationManager::class.java).notify(notificationId, notification)
    }

    fun handleAction(context: Context, action: String?, planId: String?, scheduledTimeH: Double) {
        if (planId.isNullOrBlank() || !scheduledTimeH.isFinite()) return
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(requestCode(planId))
        val store = NativeStore(context.applicationContext)
        val snapshot = store.load()
        when (action) {
            ACTION_TAKEN -> {
                val next = markPlanTaken(snapshot, planId, scheduledTimeH).snapshot
                store.save(next)
                scheduleAll(context, next)
            }
            ACTION_SKIP -> {
                val next = markPlanSkipped(snapshot, planId, scheduledTimeH).snapshot
                store.save(next)
                scheduleAll(context, next)
            }
            ACTION_SNOOZE -> {
                snapshot.medicationPlans.firstOrNull { it.id == planId }?.let { plan ->
                    val trigger = nowEpochHours() + (10.0 / 60.0)
                    scheduleOccurrence(context, plan, trigger, scheduledTimeH)
                }
            }
        }
    }

    private fun scheduleOccurrence(
        context: Context,
        plan: MedicationPlan,
        triggerTimeH: Double,
        scheduledTimeH: Double,
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerMillis = (triggerTimeH * 3_600_000.0).toLong()
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            alarmPendingIntent(context, plan.id, scheduledTimeH),
        )
    }

    private fun alarmPendingIntent(context: Context, planId: String, scheduledTimeH: Double): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_PLAN_ID, planId)
            putExtra(EXTRA_SCHEDULED_TIME_H, scheduledTimeH)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(planId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun actionPendingIntent(
        context: Context,
        action: String,
        planId: String,
        scheduledTimeH: Double,
    ): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_PLAN_ID, planId)
            putExtra(EXTRA_SCHEDULED_TIME_H, scheduledTimeH)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode("$planId:$action"),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun requestCode(value: String): Int =
        value.hashCode() and 0x7fffffff
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.showReminder(
            context = context,
            planId = intent.getStringExtra(ReminderScheduler.EXTRA_PLAN_ID).orEmpty(),
            scheduledTimeH = intent.getDoubleExtra(ReminderScheduler.EXTRA_SCHEDULED_TIME_H, Double.NaN),
        )
    }
}

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.handleAction(
            context = context,
            action = intent.action,
            planId = intent.getStringExtra(ReminderScheduler.EXTRA_PLAN_ID),
            scheduledTimeH = intent.getDoubleExtra(ReminderScheduler.EXTRA_SCHEDULED_TIME_H, Double.NaN),
        )
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val snapshot = NativeStore(context.applicationContext).load()
        ReminderScheduler.scheduleAll(context, snapshot)
    }
}
