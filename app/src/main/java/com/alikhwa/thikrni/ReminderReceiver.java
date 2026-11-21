package com.alikhwa.thikrni;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String ACTION_ALARM = "com.alikhwa.thikrni.ALARM";
    public static final String ACTION_COUNT = "com.alikhwa.thikrni.COUNT";
    public static final String ACTION_DHIKR = "com.alikhwa.thikrni.DHIKR";
    public static final String ACTION_OCCASION = "com.alikhwa.thikrni.OCCASION";

    @Override
    public void onReceive(Context context, Intent intent) {
        String a = intent.getAction();
        if (a == null) return;
        Prefs p = new Prefs(context);
        switch (a) {
            case ACTION_ALARM:
                if (p.enabled()) {
                    ReminderScheduler.schedule(context);
                    SalawatWidget.updateAll(context);
                    final PendingResult pr = goAsync();
                    SoundPlayer.play(context, "alarm", pr::finish);
                }
                break;
            case ACTION_DHIKR:
                if (p.enabled() && p.adhkar() && !Quiet.isQuiet(p)) {
                    Notifier.showDhikr(context, Adhkar.next());
                }
                break;
            case ACTION_OCCASION:
                ReminderScheduler.scheduleOccasion(context);
                SalawatWidget.updateAll(context);
                if (p.occasions() && !Quiet.isQuiet(p)) {
                    Occasions.Occ today = Occasions.onDay(p, 0);
                    if (today != null) {
                        Notifier.showOccasion(context, "اليوم: " + today.name, today.msg);
                    } else {
                        Occasions.Occ tomorrow = Occasions.onDay(p, 1);
                        if (tomorrow != null) {
                            Notifier.showOccasion(context, "غدًا: " + tomorrow.name, tomorrow.msg);
                        }
                    }
                }
                break;
            case ACTION_COUNT:
                Stats.add(context);
                break;
            default:
                break;
        }
    }
}
