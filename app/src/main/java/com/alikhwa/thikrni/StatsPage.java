package com.alikhwa.thikrni;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** الإحصائيات: اليوم/الأسبوع/الشهر/الإجمالي، رسم ٧ أيام، المناسبات، عدّاد الأجهزة. */
final class StatsPage extends Page {

    private final class Tile {
        final LinearLayout view;
        final TextView num;
        long shown = 0;

        Tile(String label) {
            view = new LinearLayout(act);
            view.setOrientation(LinearLayout.VERTICAL);
            view.setGravity(Gravity.CENTER);
            view.setPadding(ui.dp(8), ui.dp(16), ui.dp(8), ui.dp(16));
            view.setBackground(ui.shape(Ui.K_BANNER).gradient(false, 0x30FFFFFF, 0x14FFFFFF)
                    .stroke(Theme.a(ui.t.accent, 0x55), ui.dp(1)));
            num = ui.text("٠", 28, ui.t.accent, true);
            view.addView(num);
            view.addView(ui.text(label, 13, ui.t.sub, false));
        }

        void set(long v, boolean animate) {
            ui.countUp(num, animate ? 0 : shown, v, "", "");
            shown = v;
        }
    }

    private Tile tToday, tWeek, tMonth, tTotal;
    private BarChartView chart;
    private LinearLayout occBox;
    private TextView devBig, devSub, devNote;
    private long shownDev = 0;

    StatsPage(MainActivity a, Ui ui) {
        super(a, ui);
        build();
    }

    @Override
    void build() {
        content.addView(ui.pageTitle("إحصائياتي", null));

        tToday = new Tile("اليوم");
        tWeek = new Tile("آخر ٧ أيام");
        tMonth = new Tile("هذا الشهر");
        tTotal = new Tile("الإجمالي");
        content.addView(tilesRow(tToday, tWeek));
        content.addView(tilesRow(tMonth, tTotal));

        LinearLayout cc = ui.card();
        cc.addView(ui.sectionTitle("آخر ٧ أيام"));
        chart = new BarChartView(act, ui.t);
        cc.addView(chart, new LinearLayout.LayoutParams(-1, ui.dp(190)));
        content.addView(cc);

        LinearLayout oc = ui.card();
        oc.addView(ui.sectionTitle("المناسبات الهجرية"));
        occBox = new LinearLayout(act);
        occBox.setOrientation(LinearLayout.VERTICAL);
        oc.addView(occBox, new LinearLayout.LayoutParams(-1, -2));
        content.addView(oc);

        LinearLayout dc = ui.card();
        dc.addView(ui.sectionTitle("أجهزة ذكرني"));
        devBig = ui.text("—", 40, ui.t.accent, true);
        devSub = ui.text("جهاز يعمل عليه التطبيق", 14, Color.WHITE, false);
        devNote = ui.hint("");
        devNote.setGravity(Gravity.CENTER);
        dc.addView(devBig);
        dc.addView(devSub);
        dc.addView(devNote);
        dc.setOnClickListener(v -> refreshDevices(true));
        content.addView(dc);

        TextView reset = ui.text("تصفير الإحصائيات", 12, ui.t.sub, false);
        reset.setPadding(ui.dp(10), ui.dp(12), ui.dp(10), ui.dp(4));
        reset.setOnClickListener(v -> new AlertDialog.Builder(act)
                .setTitle("تصفير الإحصائيات؟")
                .setMessage("سيُصفَّر العدّاد الإجمالي وإحصائيات كل الأيام.")
                .setPositiveButton("تصفير", (d, w) -> {
                    Stats.resetAll(act);
                    onShow();
                })
                .setNegativeButton("إلغاء", null)
                .show());
        content.addView(reset);
    }

    private LinearLayout tilesRow(Tile a, Tile b) {
        LinearLayout r = new LinearLayout(act);
        r.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(ui.dp(12), ui.dp(4), ui.dp(12), ui.dp(4));
        r.setLayoutParams(lp);
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, -2, 1f);
        l1.setMargins(ui.dp(4), 0, ui.dp(4), 0);
        LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(0, -2, 1f);
        l2.setMargins(ui.dp(4), 0, ui.dp(4), 0);
        r.addView(a.view, l1);
        r.addView(b.view, l2);
        return r;
    }

    void refreshValues(boolean fromZero) {
        if (tToday == null) return;
        tToday.set(Stats.today(p), fromZero);
        tWeek.set(Stats.week(p), fromZero);
        tMonth.set(Stats.month(p), fromZero);
        tTotal.set(p.count(), fromZero);
        chart.setData(Stats.last7(p), Stats.last7Labels(), fromZero);
    }

    private void refreshOccasions() {
        occBox.removeAllViews();
        List<Occasions.Up> ups = Occasions.upcoming(p, 3);
        for (int i = 0; i < ups.size(); i++) {
            Occasions.Up u = ups.get(i);
            LinearLayout row = new LinearLayout(act);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, ui.dp(8), 0, ui.dp(8));
            LinearLayout left = new LinearLayout(act);
            left.setOrientation(LinearLayout.VERTICAL);
            TextView name = ui.text(u.occ.name, 16, Color.WHITE, true);
            name.setGravity(Gravity.START);
            TextView date = ui.text(u.hijriText(), 12, ui.t.sub, false);
            date.setGravity(Gravity.START);
            left.addView(name);
            left.addView(date);
            row.addView(left, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView when = ui.text(HijriUtil.daysPhrase(u.inDays), 13, ui.t.onAccent, true);
            when.setBackground(ui.shape(Ui.K_CHIP).gradient(true, ui.t.accentDark, ui.t.accentLight));
            when.setPadding(ui.dp(12), ui.dp(6), ui.dp(12), ui.dp(6));
            row.addView(when);
            occBox.addView(row);
            if (i < ups.size() - 1) occBox.addView(ui.divider());
        }
    }

    private void refreshDevices(boolean force) {
        if (!DeviceCounter.configured()) {
            devBig.setText("—");
            devSub.setText("عدّاد الأجهزة غير مفعّل بعد");
            devNote.setText("اضبط AppConfig.DEVICES_DB_URL (راجع README) ليظهر عدد الأجهزة هنا.");
            return;
        }
        devNote.setText("يُحسب بمعرّف عشوائي مجهول لكل تثبيت دون أي بيانات شخصية. اضغط للتحديث.");
        long cached = p.devTotal();
        if (cached >= 0) {
            ui.countUp(devBig, shownDev, cached, "", "");
            shownDev = cached;
            showActive(p.devActive());
        } else {
            devBig.setText("...");
        }
        DeviceCounter.fetch(act, force, (total, active) -> {
            if (total >= 0) {
                ui.countUp(devBig, shownDev, total, "", "");
                shownDev = total;
                showActive(active);
            } else if (shownDev == 0) {
                devBig.setText("—");
                devSub.setText("تعذّر الاتصال، حاول لاحقًا");
            }
        });
    }

    private void showActive(long active) {
        devSub.setText(active >= 0
                ? "جهاز يعمل عليه التطبيق • نشط هذا الأسبوع: " + HijriUtil.num(active)
                : "جهاز يعمل عليه التطبيق");
    }

    @Override
    void onShow() {
        refreshValues(true);
        refreshOccasions();
        refreshDevices(false);
    }
}
