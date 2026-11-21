package com.alikhwa.thikrni;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.Calendar;
import java.util.GregorianCalendar;

/** إحصائيات الصلاة على النبي (يومي/أسبوعي/شهري) والهدف اليومي. */
public final class Stats {
    private Stats() {}

    public interface Listener {
        void onAdded(boolean goalReached);
    }

    public static volatile Listener listener;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static String key(Calendar c) {
        return "d_" + c.get(Calendar.YEAR)
                + String.format(java.util.Locale.US, "%02d%02d", c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private static String todayKey() {
        return key(new GregorianCalendar());
    }

    public static long today(Prefs p) {
        return p.dayCount(todayKey());
    }

    /** آخر ٧ أيام (يشمل اليوم). */
    public static long week(Prefs p) {
        long s = 0;
        for (long v : last7(p)) s += v;
        return s;
    }

    /** الشهر الميلادي الحالي حتى اليوم. */
    public static long month(Prefs p) {
        Calendar c = new GregorianCalendar();
        int today = c.get(Calendar.DAY_OF_MONTH);
        long s = 0;
        for (int i = 0; i < today; i++) {
            s += p.dayCount(key(c));
            c.add(Calendar.DAY_OF_MONTH, -1);
        }
        return s;
    }

    /** مصفوفة من ٧ قيم: الأقدم أولًا واليوم أخيرًا. */
    public static long[] last7(Prefs p) {
        long[] out = new long[7];
        Calendar c = new GregorianCalendar();
        for (int i = 6; i >= 0; i--) {
            out[i] = p.dayCount(key(c));
            c.add(Calendar.DAY_OF_MONTH, -1);
        }
        return out;
    }

    /** ترتيب أسماء الأيام المقابلة لـ last7. */
    public static String[] last7Labels() {
        String[] out = new String[7];
        Calendar c = new GregorianCalendar();
        for (int i = 6; i >= 0; i--) {
            out[i] = i == 6 ? "اليوم" : HijriUtil.shortDay(c.get(Calendar.DAY_OF_WEEK));
            c.add(Calendar.DAY_OF_MONTH, -1);
        }
        return out;
    }

    /** يضيف صلاة واحدة للعدّاد والإحصائيات. يعيد true إذا بلغ الهدف اليومي للتو. */
    public static boolean add(Context c) {
        Context app = c.getApplicationContext();
        Prefs p = new Prefs(app);
        String tk = todayKey();
        long before = p.dayCount(tk);
        p.incCount();
        p.addDay(tk, 1);
        long after = before + 1;

        // حذف الأيام القديمة (أكثر من ٤٠٠ يوم)
        Calendar old = new GregorianCalendar();
        old.add(Calendar.DAY_OF_MONTH, -400);
        p.removeKey(key(old));

        boolean reached = p.goalEnabled() && before < p.goal() && after >= p.goal()
                && !tk.equals(p.goalAnnouncedDay());
        if (reached) {
            p.setGoalAnnouncedDay(tk);
            // التطبيق مفتوح: تظهر الاحتفالية داخله، وإلا يصل إشعار خاص
            if (listener == null) Notifier.showGoal(app, p.goal());
        }
        SalawatWidget.updateAll(app);
        final Listener l = listener;
        final boolean r = reached;
        if (l != null) MAIN.post(() -> l.onAdded(r));
        return reached;
    }

    public static void resetAll(Context c) {
        Prefs p = new Prefs(c);
        p.setCount(0);
        p.clearDays();
        p.setGoalAnnouncedDay("");
        SalawatWidget.updateAll(c);
    }
}
