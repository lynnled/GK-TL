package com.gkyytlzt.lynn;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class Ui {
    public static final int BG = Color.rgb(246, 247, 247);
    public static final int CARD = Color.WHITE;
    public static final int TEXT = Color.rgb(51, 51, 51);
    public static final int MUTED = Color.rgb(153, 153, 153);
    public static final int ACCENT = Color.rgb(72, 132, 187);
    public static final int GREEN = Color.rgb(64, 147, 198);
    public static final int RED = Color.rgb(200, 45, 45);

    public static int dp(Context c, float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics());
    }

    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    public static View card(Context c) {
        android.widget.LinearLayout l = new android.widget.LinearLayout(c);
        l.setOrientation(android.widget.LinearLayout.VERTICAL);
        l.setBackgroundColor(CARD);
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int m = dp(c, 12);
        lp.setMargins(m, m / 2, m, m / 2);
        l.setLayoutParams(lp);
        return l;
    }
}
