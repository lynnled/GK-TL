package com.gkyytlzt.lynn;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class LibraryActivity extends Activity {
    private static final int REQ_PICK = 1001;

    private final List<Row> rows = new ArrayList<>();
    private BaseAdapter adapter;
    private String downloadingId;
    private AtomicBoolean cancelFlag;

    private static class Row {
        Catalog.Entry cat;
        IndexStore.Entry local;

        Row(Catalog.Entry cat, IndexStore.Entry local) {
            this.cat = cat;
            this.local = local;
        }

        String id() { return cat != null ? cat.id : local.id; }
        String title() { return cat != null ? cat.title : local.title; }
        String meta() {
            if (cat != null) {
                return cat.sizeMb() + (local != null ? " · 已下载" : "");
            }
            return "已下载";
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(Color.WHITE);
        bar.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 8), 0);

        TextView title = Ui.text(this, "高考听力真题", 17, Ui.TEXT, true);
        bar.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView imp = Ui.text(this, "导入", 14, Ui.ACCENT, false);
        imp.setTextColor(getResources().getColor(R.color.topbar_btn_text));
        imp.setBackgroundResource(R.drawable.topbar_btn_bg);
        imp.setGravity(Gravity.CENTER);
        imp.setPadding(Ui.dp(this, 20), Ui.dp(this, 10), Ui.dp(this, 20), Ui.dp(this, 10));
        imp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickPackage(); }
        });
        bar.addView(imp, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 55)));

        View line = new View(this);
        line.setBackgroundColor(getResources().getColor(R.color.line_grey));
        root.addView(line, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 1)));

        ListView list = new ListView(this);
        list.setDivider(new android.graphics.drawable.ColorDrawable(Ui.BG));
        list.setDividerHeight(Ui.dp(this, 8));
        list.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        list.setClipToPadding(false);
        list.setBackgroundColor(Ui.BG);
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setGravity(Gravity.CENTER_HORIZONTAL);
        footer.setBackgroundColor(Ui.BG);
        footer.setPadding(0, Ui.dp(this, 24), 0, Ui.dp(this, 24));
        footer.setLayoutParams(new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView ver = Ui.text(this, "版本号 0.7 · 开发者 lynnled", 11, Ui.MUTED, false);
        ver.setGravity(Gravity.CENTER);
        footer.addView(ver);

        TextView github = new TextView(this);
        github.setTextSize(11);
        github.setTextColor(Ui.MUTED);
        github.setGravity(Gravity.CENTER);
        SpannableString ss = new SpannableString("在GitHub查看");
        int gs = ss.toString().indexOf("GitHub");
        int ge = gs + "GitHub".length();
        ss.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.primary_blue)),
                gs, ge, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new UnderlineSpan(), gs, ge, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        github.setText(ss);
        github.setPadding(0, Ui.dp(this, 6), 0, 0);
        github.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { openGithub(); }
        });
        footer.addView(github);

        list.addFooterView(footer, null, false);
        list.setFooterDividersEnabled(false);

        adapter = new BaseAdapter() {
            public int getCount() { return rows.size(); }
            public Object getItem(int i) { return rows.get(i); }
            public long getItemId(int i) { return i; }

            public View getView(int pos, View cv, ViewGroup parent) {
                final Row r = rows.get(pos);
                LinearLayout row = new LinearLayout(LibraryActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setBackgroundColor(Color.WHITE);
                int p = Ui.dp(LibraryActivity.this, 16);
                row.setPadding(p, p, p, p);
                row.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                LinearLayout texts = new LinearLayout(LibraryActivity.this);
                texts.setOrientation(LinearLayout.VERTICAL);
                TextView t = Ui.text(LibraryActivity.this, r.title(), 17, Ui.TEXT, true);
                texts.addView(t);
                TextView m = Ui.text(LibraryActivity.this, r.meta(), 12, Ui.MUTED, false);
                LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                mlp.topMargin = Ui.dp(LibraryActivity.this, 4);
                texts.addView(m, mlp);
                row.addView(texts, new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                if (r.local != null) {
                    ImageButton del = new ImageButton(LibraryActivity.this);
                    del.setImageResource(R.drawable.ic_delete);
                    del.setBackgroundColor(Color.TRANSPARENT);
                    del.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    int dp8 = Ui.dp(LibraryActivity.this, 8);
                    del.setPadding(dp8, dp8, dp8, dp8);
                    LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                            Ui.dp(LibraryActivity.this, 40), Ui.dp(LibraryActivity.this, 40));
                    dlp.leftMargin = Ui.dp(LibraryActivity.this, 4);
                    del.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) { confirmDelete(r.local); }
                    });
                    row.addView(del, dlp);
                }

                final TextView action = Ui.text(LibraryActivity.this, "", 14,
                        getResources().getColor(R.color.topbar_btn_text), false);
                action.setBackgroundResource(R.drawable.topbar_btn_bg);
                action.setGravity(Gravity.CENTER);
                action.setPadding(Ui.dp(LibraryActivity.this, 18), Ui.dp(LibraryActivity.this, 9),
                        Ui.dp(LibraryActivity.this, 18), Ui.dp(LibraryActivity.this, 9));
                if (downloadingId != null && downloadingId.equals(r.id())) {
                    action.setText("下载中");
                    action.setEnabled(false);
                    action.setAlpha(0.5f);
                } else if (r.local != null) {
                    action.setText("打开");
                } else {
                    action.setText("下载");
                }
                LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                alp.leftMargin = Ui.dp(LibraryActivity.this, 12);
                row.addView(action, alp);

                row.setClickable(true);
                row.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (r.local != null) open(r.local.id);
                        else startDownload(r.cat);
                    }
                });
                action.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (r.local != null) open(r.local.id);
                        else startDownload(r.cat);
                    }
                });
                row.setOnLongClickListener(new View.OnLongClickListener() {
                    public boolean onLongClick(View v) {
                        if (r.local != null) {
                            confirmDelete(r.local);
                            return true;
                        }
                        return false;
                    }
                });
                return row;
            }
        };
        list.setAdapter(adapter);
        setContentView(root);
        refresh();
    }

    private void refresh() {
        rows.clear();
        List<IndexStore.Entry> imported = IndexStore.load(this);
        for (Catalog.Entry ce : Catalog.ENTRIES) {
            rows.add(new Row(ce, find(imported, ce.id)));
        }
        for (IndexStore.Entry ie : imported) {
            if (Catalog.byId(ie.id) == null) rows.add(new Row(null, ie));
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private IndexStore.Entry find(List<IndexStore.Entry> list, String id) {
        for (IndexStore.Entry e : list) {
            if (e.id.equals(id)) return e;
        }
        return null;
    }

    private void open(String id) {
        Intent i = new Intent(this, ExamActivity.class);
        i.putExtra("id", id);
        startActivity(i);
    }

    private void openGithub() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/lynnled/GK-TL")));
        } catch (Exception e) {
            toast("无法打开浏览器");
        }
    }

    private void confirmDelete(final IndexStore.Entry e) {
        new AlertDialog.Builder(this)
                .setTitle("删除题库")
                .setMessage("确定删除“" + e.title + "”？音频副本也会删除。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        TklibImporter.deleteRec(new File(new File(getFilesDir(), "exams"), e.id));
                        IndexStore.remove(LibraryActivity.this, e.id);
                        refresh();
                    }
                }).show();
    }

    private void startDownload(final Catalog.Entry entry) {
        if (entry == null) return;
        if (downloadingId != null) {
            toast("正在下载，请稍候");
            return;
        }
        downloadingId = entry.id;
        adapter.notifyDataSetChanged();

        final AtomicBoolean cancel = new AtomicBoolean(false);
        cancelFlag = cancel;

        final ProgressBar bar = new ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(0);
        ColorStateList primary = ColorStateList.valueOf(getResources().getColor(R.color.primary_blue));
        bar.setProgressTintList(primary);
        bar.setIndeterminateTintList(primary);
        final TextView status = new TextView(this);
        status.setTextSize(13);
        status.setTextColor(getResources().getColor(R.color.text_common));
        status.setText("准备下载…");
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 24);
        box.setPadding(p, p, p, p);
        box.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(this, 12);
        box.addView(status, slp);

        final AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("下载 " + entry.title)
                .setView(box)
                .setNegativeButton("取消", null)
                .setCancelable(false)
                .create();
        dlg.setOnShowListener(new DialogInterface.OnShowListener() {
            public void onShow(DialogInterface d) {
                dlg.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(
                        new View.OnClickListener() {
                            public void onClick(View v) {
                                cancel.set(true);
                                dlg.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
                                status.setText("正在取消…");
                            }
                        });
            }
        });
        dlg.show();

        new Thread(new Runnable() {
            public void run() {
                File tmp = new File(getCacheDir(), entry.id + ".tklib.download");
                if (tmp.exists()) tmp.delete();
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(entry.url());
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(30000);
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestProperty("User-Agent", "GaokaoListening/0.5");
                    int code = conn.getResponseCode();
                    if (code != 200) throw new Exception("服务器返回 HTTP " + code);
                    long total = conn.getContentLengthLong();
                    if (total <= 0) ui(new Runnable() {
                        public void run() { bar.setIndeterminate(true); }
                    });
                    InputStream in = conn.getInputStream();
                    FileOutputStream out = new FileOutputStream(tmp);
                    byte[] buf = new byte[64 * 1024];
                    long done = 0;
                    long lastUi = 0;
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        if (cancel.get()) break;
                        out.write(buf, 0, n);
                        done += n;
                        long now = System.currentTimeMillis();
                        if (now - lastUi > 200) {
                            lastUi = now;
                            final long d = done;
                            final long t = total;
                            ui(new Runnable() {
                                public void run() {
                                    if (t > 0) bar.setProgress((int) (d * 100 / t));
                                    status.setText(mb(d) + (t > 0 ? " / " + mb(t) : ""));
                                }
                            });
                        }
                    }
                    out.flush();
                    out.close();
                    in.close();
                    if (cancel.get()) {
                        tmp.delete();
                        ui(new Runnable() {
                            public void run() { finishDownload(dlg, null, "已取消"); }
                        });
                        return;
                    }
                    if (total > 0 && done != total) throw new Exception("下载不完整");
                    if (entry.bytes > 0 && done != entry.bytes) throw new Exception("文件大小不符");
                    ui(new Runnable() {
                        public void run() {
                            bar.setIndeterminate(true);
                            status.setText("正在导入…");
                        }
                    });
                    final TklibImporter.Result r = TklibImporter.importFile(LibraryActivity.this, tmp);
                    tmp.delete();
                    ui(new Runnable() {
                        public void run() { finishDownload(dlg, r, null); }
                    });
                } catch (final Exception e) {
                    tmp.delete();
                    final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                    ui(new Runnable() {
                        public void run() {
                            if (cancel.get()) finishDownload(dlg, null, "已取消");
                            else finishDownload(dlg, null, msg);
                        }
                    });
                } finally {
                    if (conn != null) conn.disconnect();
                    cancelFlag = null;
                }
            }
        }).start();
    }

    private void finishDownload(AlertDialog dlg, TklibImporter.Result r, String msg) {
        if (dlg != null && dlg.isShowing()) dlg.dismiss();
        downloadingId = null;
        refresh();
        if (r != null) {
            if (r.exam != null) toast("下载完成：" + r.exam.title);
            else showError(r.error);
        } else if (msg != null) {
            if ("已取消".equals(msg)) toast("已取消");
            else showError(msg);
        }
    }

    private void showError(String msg) {
        new AlertDialog.Builder(this)
                .setTitle("下载失败")
                .setMessage(msg == null ? "未知错误" : msg)
                .setPositiveButton("知道了", null)
                .show();
    }

    private void ui(Runnable r) {
        if (isFinishing() || isDestroyed()) return;
        runOnUiThread(new Runnable() {
            public void run() {
                if (!isFinishing() && !isDestroyed()) r.run();
            }
        });
    }

    private String mb(long bytes) {
        return String.format(Locale.US, "%.1f MB", bytes / 1048576.0);
    }

    private void pickPackage() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            startActivityForResult(i, REQ_PICK);
        } catch (Exception e) {
            toast("无法打开文件选择器: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_PICK || res != RESULT_OK || data == null || data.getData() == null) return;
        final Uri uri = data.getData();
        final AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("导入中")
                .setMessage("正在读取题库包，请稍候…")
                .setCancelable(false)
                .create();
        dlg.show();
        new Thread(new Runnable() {
            public void run() {
                final TklibImporter.Result r = TklibImporter.importUri(LibraryActivity.this, uri);
                ui(new Runnable() {
                    public void run() {
                        if (dlg.isShowing()) dlg.dismiss();
                        if (r.exam != null) {
                            toast("导入成功：" + r.exam.title);
                            refresh();
                        } else {
                            showError(r.error);
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        if (cancelFlag != null) cancelFlag.set(true);
        super.onDestroy();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
