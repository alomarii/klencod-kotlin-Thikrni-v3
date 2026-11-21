package com.alikhwa.thikrni;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** الإعدادات: التذكير، أوقات الصمت، الجمعة، الهدف، الأذكار والمناسبات، المظهر، المشاركة. */
final class SettingsPage extends Page {
    private static final int[] CHIP_MINS = {5, 10, 15, 30, 60, 120, 180, 0};
    private static final String[] CHIP_LABELS = {"٥ دقائق", "١٠ دقائق", "١٥ دقيقة", "٣٠ دقيقة",
            "ساعة", "ساعتان", "٣ ساعات", "مخصص"};
    private static final int[] FRI_MINS = {5, 10, 15, 30};
    private static final String[] FRI_LABELS = {"٥ د", "١٠ د", "١٥ د", "٣٠ د"};
    private static final int[] GOAL_VALUES = {50, 100, 300, 500, 1000, 0};
    private static final String[] GOAL_LABELS = {"٥٠", "١٠٠", "٣٠٠", "٥٠٠", "١٠٠٠", "مخصص"};

    private Ui.Chips intervalChips, goalChips, fridayChips;
    private TextView btnFrom, btnTo, tvQuiet;
    private LinearLayout quietRow, goalBox, fridayBox;

    SettingsPage(MainActivity a, Ui ui) {
        super(a, ui);
        build();
    }

    @Override
    void build() {
        content.addView(ui.pageTitle("الإعدادات", null));
        buildReminderCard();
        buildQuietCard();
        buildFridayCard();
        buildGoalCard();
        buildExtrasCard();
        buildThemeCard();
        buildAppCard();
        buildFooter();
    }

    // ---------------------------------------------------------------- التذكير

    private void buildReminderCard() {
        LinearLayout c = ui.card();
        c.addView(ui.sectionTitle("التذكير"));
        c.addView(ui.switchRow("تفعيل التذكير", p.enabled(), b -> {
            p.setEnabled(b);
            ReminderScheduler.syncAll(act);
        }));
        c.addView(ui.divider());

        TextView every = ui.text("ذكّرني كل:", 15, Color.WHITE, true);
        every.setGravity(Gravity.START);
        every.setPadding(0, ui.dp(4), 0, ui.dp(6));
        c.addView(every);
        intervalChips = ui.chips(CHIP_LABELS, 4, -1, this::onIntervalChip);
        c.addView(intervalChips);
        refreshIntervalChips();

        c.addView(ui.divider());
        c.addView(ui.switchRow("عند توصيل الشاحن", p.onCharge(), b -> {
            p.setOnCharge(b);
            ReminderScheduler.syncAll(act);
        }));
        c.addView(ui.switchRow("عند فتح قفل الهاتف", p.onUnlock(), b -> {
            p.setOnUnlock(b);
            ReminderScheduler.syncAll(act);
        }));
        c.addView(ui.switchRow("الاهتزاز", p.vibrate(), b -> p.setVibrate(b)));
        c.addView(ui.switchRow("عدّ تلقائي لكل تذكير يُسمَع", p.autoCount(), b -> p.setAutoCount(b)));
        c.addView(ui.hint("عند التفعيل يُضاف كل تذكير يُشغَّل إلى عدّاد صلواتك وإحصائياتك تلقائيًا."));
        content.addView(c);
    }

    private void refreshIntervalChips() {
        int cur = p.intervalMin();
        int sel = 7;
        for (int i = 0; i < 7; i++) if (CHIP_MINS[i] == cur) sel = i;
        intervalChips.select(sel);
        intervalChips.setLabel(7, sel == 7 ? "كل " + HijriUtil.digits(cur) + " د" : "مخصص");
    }

    private void onIntervalChip(int idx) {
        if (CHIP_MINS[idx] > 0) {
            p.setIntervalMin(CHIP_MINS[idx]);
            ReminderScheduler.syncAll(act);
            refreshIntervalChips();
            return;
        }
        askNumber("ذكّرني كل كم دقيقة؟", "عدد الدقائق", 1, 1440, v -> {
            p.setIntervalMin(v);
            ReminderScheduler.syncAll(act);
            refreshIntervalChips();
        });
    }

    private interface OnNumber {
        void got(int v);
    }

    private void askNumber(String title, String hint, final int min, final int max, final OnNumber cb) {
        final EditText et = new EditText(act);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setHint(hint);
        et.setGravity(Gravity.CENTER);
        new AlertDialog.Builder(act)
                .setTitle(title)
                .setView(et)
                .setPositiveButton("حفظ", (d, w) -> {
                    try {
                        int v = Integer.parseInt(et.getText().toString().trim());
                        cb.got(Math.max(min, Math.min(max, v)));
                    } catch (Exception ignored) {
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    // ------------------------------------------------------------ أوقات الصمت

    private void buildQuietCard() {
        LinearLayout c = ui.card();
        c.addView(ui.sectionTitle("أوقات الصمت"));
        c.addView(ui.switchRow("لا تزعجني في هذه الساعات", p.quietEnabled(), b -> {
            p.setQuietEnabled(b);
            quietRow.animate().alpha(b ? 1f : 0.4f).setDuration(250).start();
            refreshQuiet();
        }));

        quietRow = new LinearLayout(act);
        quietRow.setOrientation(LinearLayout.HORIZONTAL);
        quietRow.setPadding(0, ui.dp(8), 0, 0);
        btnFrom = ui.outlineButton("");
        btnTo = ui.outlineButton("");
        btnFrom.setOnClickListener(v -> pickTime(true));
        btnTo.setOnClickListener(v -> pickTime(false));
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, ui.dp(46), 1f);
        l1.setMargins(0, 0, ui.dp(5), 0);
        LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(0, ui.dp(46), 1f);
        l2.setMargins(ui.dp(5), 0, 0, 0);
        quietRow.addView(btnFrom, l1);
        quietRow.addView(btnTo, l2);
        quietRow.setAlpha(p.quietEnabled() ? 1f : 0.4f);
        c.addView(quietRow, new LinearLayout.LayoutParams(-1, -2));

        tvQuiet = ui.hint("");
        c.addView(tvQuiet);
        content.addView(c);
        refreshQuiet();
    }

    private void pickTime(final boolean from) {
        int cur = from ? p.quietStart() : p.quietEnd();
        new TimePickerDialog(act, (view, h, m) -> {
            if (from) p.setQuietStart(h * 60 + m);
            else p.setQuietEnd(h * 60 + m);
            refreshQuiet();
        }, cur / 60, cur % 60, false).show();
    }

    private void refreshQuiet() {
        btnFrom.setText("من: " + HijriUtil.clock(p.quietStart()));
        btnTo.setText("إلى: " + HijriUtil.clock(p.quietEnd()));
        if (!p.quietEnabled()) {
            tvQuiet.setText("فعّل الخيار لإيقاف الصوت والتنبيهات في ساعات النوم أو العمل مثلًا.");
        } else if (Quiet.isQuiet(p)) {
            tvQuiet.setText("نحن الآن في وقت الصمت، لن يُشغَّل أي تذكير حتى الساعة " + HijriUtil.clock(p.quietEnd()) + ".");
        } else {
            tvQuiet.setText("لن يُشغَّل أي صوت أو تنبيه بين هذين الوقتين. العدّ اليدوي يعمل دائمًا.");
        }
    }

    // ------------------------------------------------------------------ الجمعة

    private void buildFridayCard() {
        LinearLayout c = ui.card();
        c.addView(ui.sectionTitle("ليلة الجمعة ويومها"));
        c.addView(ui.switchRow("تكثيف التذكير", p.fridayBoost(), b -> {
            p.setFridayBoost(b);
            fridayBox.animate().alpha(b ? 1f : 0.4f).setDuration(250).start();
            ReminderScheduler.syncAll(act);
        }));
        fridayBox = new LinearLayout(act);
        fridayBox.setOrientation(LinearLayout.VERTICAL);
        TextView lbl = ui.text("ذكّرني كل:", 14, Color.WHITE, true);
        lbl.setGravity(Gravity.START);
        lbl.setPadding(0, ui.dp(6), 0, ui.dp(4));
        fridayBox.addView(lbl);
        int sel = -1;
        for (int i = 0; i < FRI_MINS.length; i++) if (FRI_MINS[i] == p.fridayInterval()) sel = i;
        fridayChips = ui.chips(FRI_LABELS, 4, sel, i -> {
            p.setFridayInterval(FRI_MINS[i]);
            fridayChips.select(i);
            ReminderScheduler.syncAll(act);
        });
        fridayBox.addView(fridayChips);
        fridayBox.setAlpha(p.fridayBoost() ? 1f : 0.4f);
        c.addView(fridayBox, new LinearLayout.LayoutParams(-1, -2));
        c.addView(ui.hint("يسرّع التذكير من مساء الخميس (٦ م) حتى مساء الجمعة (٦ م) تقريبًا، تذكيرًا بفضل الصلاة على النبي في هذا الوقت."));
        content.addView(c);
    }

    // ------------------------------------------------------------ الهدف اليومي

    private void buildGoalCard() {
        LinearLayout c = ui.card();
        c.addView(ui.sectionTitle("الهدف اليومي"));
        c.addView(ui.switchRow("تفعيل الهدف", p.goalEnabled(), b -> {
            p.setGoalEnabled(b);
            goalBox.animate().alpha(b ? 1f : 0.4f).setDuration(250).start();
            SalawatWidget.updateAll(act);
        }));
        goalBox = new LinearLayout(act);
        goalBox.setOrientation(LinearLayout.VERTICAL);
        goalChips = ui.chips(GOAL_LABELS, 3, -1, this::onGoalChip);
        goalBox.addView(goalChips);
        goalBox.setAlpha(p.goalEnabled() ? 1f : 0.4f);
        c.addView(goalBox, new LinearLayout.LayoutParams(-1, -2));
        c.addView(ui.hint("يظهر شريط تقدّم متحرك في الرئيسية، وعند إتمام الهدف تُعرض احتفالية وتصلك إشعار خاص."));
        content.addView(c);
        refreshGoalChips();
    }

    private void refreshGoalChips() {
        int cur = p.goal();
        int sel = 5;
        for (int i = 0; i < 5; i++) if (GOAL_VALUES[i] == cur) sel = i;
        goalChips.select(sel);
        goalChips.setLabel(5, sel == 5 ? HijriUtil.num(cur) : "مخصص");
    }

    private void onGoalChip(int idx) {
        if (GOAL_VALUES[idx] > 0) {
            p.setGoal(GOAL_VALUES[idx]);
            refreshGoalChips();
            SalawatWidget.updateAll(act);
            return;
        }
        askNumber("كم صلاة تريد كل يوم؟", "عدد الصلوات", 1, 100000, v -> {
            p.setGoal(v);
            refreshGoalChips();
            SalawatWidget.updateAll(act);
        });
    }

    // ------------------------------------------------------- الأذكار والمناسبات

    private void buildExtrasCard() {
        LinearLayout c = ui.card();
        c.addView(ui.sectionTitle("أذكار ومناسبات"));
        c.addView(ui.switchRow("أذكار قصيرة بين التذكيرات", p.adhkar(), b -> {
            p.setAdhkar(b);
            ReminderScheduler.syncAll(act);
        }));
        c.addView(ui.hint("إشعار هادئ بدون صوت بذكر قصير (استغفار، تسبيح...) في منتصف المدة بين تذكيرين."));
        c.addView(ui.divider());
        c.addView(ui.switchRow("تنبيه المناسبات الهجرية", p.occasions(), b -> {
            p.setOccasions(b);
            ReminderScheduler.scheduleOccasion(act);
        }));
        c.addView(ui.hint("المولد النبوي ورمضان والعيدان وغيرها، تُحسب تلقائيًا من التاريخ الهجري وتصلك صباحًا. يمكنك تعديل الهجري ±٣ أيام من الرئيسية."));
        content.addView(c);
    }

    // ------------------------------------------------------------------ المظهر

    private void buildThemeCard() {
        LinearLayout c = ui.card();
        c.addView(ui.sectionTitle("المظهر"));
        c.addView(ui.hint("لكل مظهر شكل أزرار وحركة وخلفية وانتقالات خاصة به."));
        final int perRow = 4;
        LinearLayout row = null;
        for (int i = 0; i < Theme.NAMES.length; i++) {
            if (i % perRow == 0) {
                row = new LinearLayout(act);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
                rp.topMargin = ui.dp(8);
                c.addView(row, rp);
            }
            final int idx = i;
            Theme th = Theme.of(i);
            LinearLayout item = new LinearLayout(act);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            View sw = new View(act);
            boolean sel = p.theme() == i;
            // معاينة بشكل أزرار هذا الثيم نفسه
            sw.setBackground(ui.shapeFor(th.st, Ui.K_SMALL).gradient(true, th.bgTop, th.bgMid, th.accent)
                    .stroke(sel ? Color.WHITE : Theme.a(th.accent, 0x88), ui.dp(sel ? 3 : 1)));
            if (sel) {
                sw.setScaleX(1.1f);
                sw.setScaleY(1.1f);
            }
            item.addView(sw, new LinearLayout.LayoutParams(ui.dp(50), ui.dp(50)));
            TextView nm = ui.text(Theme.NAMES[i], 12, sel ? ui.t.accent : ui.t.sub, sel);
            nm.setPadding(0, ui.dp(6), 0, 0);
            nm.setSingleLine(true);
            item.addView(nm);
            TextView tag = ui.text(Style.TAGS[i], 10, ui.t.sub, false);
            tag.setAlpha(0.8f);
            item.addView(tag);
            ui.pressEffect(item);
            item.setOnClickListener(v -> {
                if (p.theme() != idx) {
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
                    p.setTheme(idx);
                    act.applyTheme(v);
                }
            });
            row.addView(item, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        // إكمال الصف الأخير بفراغات
        int rem = Theme.NAMES.length % perRow;
        if (rem != 0 && row != null) {
            for (int k = rem; k < perRow; k++) row.addView(new View(act), new LinearLayout.LayoutParams(0, 1, 1f));
        }
        content.addView(c);
    }

    // ------------------------------------------------------------------ التطبيق

    private void buildAppCard() {
        LinearLayout c = ui.card();
        c.addView(ui.sectionTitle("التطبيق"));

        VenomButton widget = ui.venom("إضافة الويدجت للشاشة الرئيسية", true);
        widget.setOnClickListener(v -> WidgetPin.request(act));
        c.addView(widget, new LinearLayout.LayoutParams(-1, ui.dp(48) + 2 * ui.venomPad()));
        c.addView(ui.hint("ويدجت يعرض التاريخ الهجري والميلادي وعدّاد اليوم، وبه زر للصلاة على النبي. تتبع ألوانه المظهر المختار."));
        c.addView(ui.divider());

        TextView bat = ui.outlineButton("السماح بالعمل في الخلفية (مهم)");
        bat.setOnClickListener(v -> {
            try {
                Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + act.getPackageName()));
                act.startActivity(i);
            } catch (Exception e) {
                try {
                    act.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                } catch (Exception ignored) {
                }
            }
        });
        c.addView(bat, new LinearLayout.LayoutParams(-1, ui.dp(46)));

        VenomButton share = ui.venom("مشاركة التطبيق", false);
        share.setOnClickListener(v -> {
            Intent s = new Intent(Intent.ACTION_SEND);
            s.setType("text/plain");
            s.putExtra(Intent.EXTRA_SUBJECT, "تطبيق ذكرني");
            s.putExtra(Intent.EXTRA_TEXT, "اللهم صلِّ وسلِّم على نبينا محمد\n"
                    + "جرّب تطبيق «ذكرني» لتذكيرك بالصلاة على النبي صلى الله عليه وسلم، "
                    + "ومن دلّ على خير فله مثل أجر فاعله:\n" + AppConfig.SHARE_URL);
            act.startActivity(Intent.createChooser(s, "مشاركة التطبيق"));
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, ui.dp(50) + 2 * ui.venomPad());
        lp.topMargin = ui.dp(4);
        c.addView(share, lp);
        c.addView(ui.hint("زر المشاركة يرسل رابط التطبيق مع دعاء، وهو ثواب جارٍ للجميع بإذن الله."));
        content.addView(c);
    }

    private void buildFooter() {
        LinearLayout c = ui.card();
        TextView hadith = ui.text("«من صلى عليَّ صلاةً صلى الله عليه بها عشرًا»", 15, Color.WHITE, false);
        hadith.setLineSpacing(0, 1.25f);
        c.addView(hadith);
        c.addView(ui.divider());
        c.addView(ui.text("برمجة وتطوير", 13, ui.t.sub, false));
        c.addView(ui.text("محمد العمري", 24, ui.t.accent, true));
        TextView phone = ui.text("730215825", 18, Color.WHITE, true);
        phone.setPadding(ui.dp(10), ui.dp(6), ui.dp(10), ui.dp(6));
        phone.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        phone.setOnClickListener(v -> {
            try {
                act.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:730215825")));
            } catch (Exception ignored) {
            }
        });
        c.addView(phone);
        c.addView(ui.text(AppConfig.VERSION_LABEL, 11, ui.t.sub, false));
        content.addView(c);
    }

    @Override
    void onShow() {
        refreshQuiet();
    }
}
