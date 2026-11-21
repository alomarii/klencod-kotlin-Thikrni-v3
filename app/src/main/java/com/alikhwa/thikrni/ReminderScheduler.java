package com.alikhwa.thikrni;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.GregorianCalendar;

/** جدولة التذكير الدوري والأذكار والمناسبات وتشغيل خدمة الشاحن/فتح القفل. */
public final class ReminderScheduler {
    private ReminderScheduler() {}

    private static PendingIntent pending(Context c, String action, int code) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.setAction(action);
        return PendingIntent.getBroadcast(c, code, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** الفاصل الفعلي: يقصر في ليلة الجمعة ويومها إن فُعّل التكثيف. */
    public static int effectiveInterval(Prefs p) {
        int i = p.intervalMin();
        if (p.fridayBoost() && HijriUtil.isFridayWindow()) i = Math.min(i, p.fridayInterval());
        return Math.max(1, i);
    }

    public static void schedule(Context c) {
        Context app = c.getApplicationContext();
        Prefs p = new Prefs(app);
        AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pending(app, ReminderReceiver.ACTION_ALARM, 100));
        am.cancel(pending(app, ReminderReceiver.ACTION_DHIKR, 102));
        if (!p.enabled()) return;
        long now = System.currentTimeMillis();
        long every = effectiveInterval(p) * 60_000L;
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, now + every,
                pending(app, ReminderReceiver.ACTION_ALARM, 100));
        if (p.adhkar()) {
            // الذكر القصير في منتصف المدة بين تذكيرين
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, now + every / 2,
                    pending(app, ReminderReceiver.ACTION_DHIKR, 102));
        }
    }

    /** تنبيه يومي ٨ صباحًا للتحقق من المناسبات الهجرية. */
    public static void scheduleOccasion(Context c) {
        Context app = c.getApplicationContext();
        Prefs p = new Prefs(app);
        AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pending(app, ReminderReceiver.ACTION_OCCASION, 101));
        if (!p.occasions()) return;
        Calendar t = new GregorianCalendar();
        t.set(Calendar.HOUR_OF_DAY, 8);
        t.set(Calendar.MINUTE, 0);
        t.set(Calendar.SECOND, 0);
        t.set(Calendar.MILLISECOND, 0);
        if (t.getTimeInMillis() <= System.currentTimeMillis() + 60_000L) t.add(Calendar.DAY_OF_MONTH, 1);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.getTimeInMillis(),
                pending(app, ReminderReceiver.ACTION_OCCASION, 101));
    }

    public static void cancel(Context c) {
        Context app = c.getApplicationContext();
        AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pending(app, ReminderReceiver.ACTION_ALARM, 100));
        am.cancel(pending(app, ReminderReceiver.ACTION_DHIKR, 102));
    }

    /** يطبّق الإعدادات الحالية: المنبّهات + الخدمة. */
    public static void syncAll(Context c) {
        Context app = c.getApplicationContext();
        Prefs p = new Prefs(app);
        schedule(app);
        scheduleOccasion(app);
        Intent svc = new Intent(app, ReminderService.class);
        if (p.enabled()) {
            try {
                if (Build.VERSION.SDK_INT >= 26) app.startForegroundService(svc);
                else app.startService(svc);
            } catch (Exception ignored) {
            }
        } else {
            app.stopService(svc);
        }
    }
}
