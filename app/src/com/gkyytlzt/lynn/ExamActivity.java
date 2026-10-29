package com.gkyytlzt.lynn;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExamActivity extends Activity {
    private Exam exam;
    private MediaPlayer player;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SeekBar seekBar;
    private TextView timeStart;
    private TextView timeEnd;
    private TextView speedTv;
    private TextView submitBtn;
    private ImageButton playBtn;
    private ListView list;
    private QuestionAdapter adapter;
    private String[] selected;
    private boolean[] transcriptOpen;
    private final List<Holder> holders = new ArrayList<>();
    private boolean submitted;
    private boolean dragging;
    private boolean prepared;
    private float speed = 1.0f;
    private int speedIndex = 1;
    private final float[] speeds = new float[]{0.75f, 1.0f, 1.25f, 1.5f};

    private static class Holder {
        View item;
        Exam.Question q;
        TextView stem;
        RadioGroup group;
        final List<RadioButton> buttons = new ArrayList<>();
        View resultRow;
        TextView answerValue;
        TextView link;
        TextView box;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String id = getIntent().getStringExtra("id");
        File dir = new File(new File(getFilesDir(), "exams"), id == null ? "" : id);
        try {
            File mf = new File(dir, "manifest.json");
            byte[] bytes = IndexStore.readAll(new FileInputStream(mf));
            exam = Exam.fromJson(new JSONObject(new String(bytes, StandardCharsets.UTF_8)));
        } catch (Exception e) {
            Toast.makeText(this, "题库读取失败", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        File audio = new File(dir, exam.audio);
        if (!audio.exists()) {
            File[] fs = dir.listFiles();
            if (fs != null) {
                for (File f : fs) {
                    if (f.getName().toLowerCase(Locale.ROOT).endsWith(".mp3")) {
                        audio = f;
                        break;
                    }
                }
            }
        }

        selected = new String[exam.questions.size()];
        transcriptOpen = new boolean[exam.questions.size()];
        setContentView(R.layout.activity_local_listen);
        bindViews();
        setupPlayer(audio);
        handler.post(tick);
    }

    private void bindViews() {
        ImageView back = findViewById(R.id.local_back);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        TextView title = findViewById(R.id.local_title);
        title.setText(exam.title);

        submitBtn = findViewById(R.id.local_submit);
        submitBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (submitted) recreate();
                else submit();
            }
        });

        seekBar = findViewById(R.id.local_seekbar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) timeStart.setText(fmt(progress));
            }
            public void onStartTrackingTouch(SeekBar sb) { dragging = true; }
            public void onStopTrackingTouch(SeekBar sb) {
                dragging = false;
                if (player != null && prepared) player.seekTo(sb.getProgress());
            }
        });

        timeStart = findViewById(R.id.local_time_start);
        timeEnd = findViewById(R.id.local_time_end);
        speedTv = findViewById(R.id.local_speed);
        speedTv.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { cycleSpeed(); }
        });

        playBtn = findViewById(R.id.local_play);
        playBtn.setImageResource(R.drawable.ic_play);
        playBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { togglePlay(); }
        });

        ImageButton minus = findViewById(R.id.local_minus5);
        minus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { seekBy(-5000); }
        });

        ImageButton plus = findViewById(R.id.local_plus5);
        plus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { seekBy(5000); }
        });

        TextView minusLabel = findViewById(R.id.local_minus5_label);
        minusLabel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { seekBy(-5000); }
        });
        TextView plusLabel = findViewById(R.id.local_plus5_label);
        plusLabel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { seekBy(5000); }
        });

        ImageButton allText = findViewById(R.id.local_all_text);
        allText.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showAllTexts(); }
        });

        list = findViewById(R.id.local_question_list);
        adapter = new QuestionAdapter();
        list.setAdapter(adapter);
    }

    private void setupPlayer(File audio) {
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            player.setDataSource(audio.getAbsolutePath());
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    prepared = true;
                    seekBar.setMax(mp.getDuration());
                    timeStart.setText("00:00");
                    timeEnd.setText(fmt(mp.getDuration()));
                }
            });
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    playBtn.setImageResource(R.drawable.ic_play);
                    seekBar.setProgress(mp.getDuration());
                }
            });
            player.prepareAsync();
        } catch (Exception e) {
            Toast.makeText(this, "音频加载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void togglePlay() {
        if (player == null || !prepared) {
            Toast.makeText(this, "音频还没准备好", Toast.LENGTH_SHORT).show();
            return;
        }
        if (player.isPlaying()) {
            player.pause();
            playBtn.setImageResource(R.drawable.ic_play);
        } else {
            player.start();
            applySpeed(false);
            playBtn.setImageResource(R.drawable.ic_pause);
        }
    }

    private void seekBy(int delta) {
        if (player == null || !prepared) return;
        int pos = player.getCurrentPosition() + delta;
        pos = Math.max(0, Math.min(player.getDuration(), pos));
        player.seekTo(pos);
        seekBar.setProgress(pos);
        timeStart.setText(fmt(pos));
    }

    private void cycleSpeed() {
        speedIndex = (speedIndex + 1) % speeds.length;
        speed = speeds[speedIndex];
        speedTv.setText(speedText(speed));
        applySpeed(true);
    }

    private String speedText(float s) {
        if (s == 1.0f) return "1.0倍";
        if (s == 1.5f) return "1.5倍";
        if (s == 1.25f) return "1.25倍";
        return "0.75倍";
    }

    private void applySpeed(boolean restorePause) {
        if (player == null || !prepared) return;
        boolean wasPlaying = player.isPlaying();
        try {
            player.setPlaybackParams(new PlaybackParams().setSpeed(speed));
        } catch (Exception ignored) {
        }
        if (restorePause && !wasPlaying) {
            try { player.pause(); } catch (Exception ignored) {}
            playBtn.setImageResource(R.drawable.ic_play);
        }
    }

    private final Runnable tick = new Runnable() {
        public void run() {
            if (player != null && prepared && !dragging) {
                try {
                    int pos = player.getCurrentPosition();
                    seekBar.setProgress(pos);
                    timeStart.setText(fmt(pos));
                } catch (Exception ignored) {
                }
            }
            handler.postDelayed(this, 500);
        }
    };

    private class QuestionAdapter extends BaseAdapter {
        public int getCount() { return exam.questions.size(); }
        public Object getItem(int i) { return exam.questions.get(i); }
        public long getItemId(int i) { return i; }

        public View getView(final int pos, View convertView, ViewGroup parent) {
            View v = LayoutInflater.from(ExamActivity.this)
                    .inflate(R.layout.item_local_question, parent, false);
            final Holder h = new Holder();
            h.item = v;
            h.q = exam.questions.get(pos);
            h.stem = v.findViewById(R.id.local_question_text);
            h.group = v.findViewById(R.id.local_options);
            h.buttons.add((RadioButton) v.findViewById(R.id.local_opt_a));
            h.buttons.add((RadioButton) v.findViewById(R.id.local_opt_b));
            h.buttons.add((RadioButton) v.findViewById(R.id.local_opt_c));
            h.resultRow = v.findViewById(R.id.local_result_row);
            h.answerValue = v.findViewById(R.id.local_answer_value);
            h.link = v.findViewById(R.id.local_transcript_link);
            h.box = v.findViewById(R.id.local_transcript_box);

            h.stem.setText(h.q.no + ". " + h.q.stem);
            String[] letters = new String[]{"A", "B", "C"};
            for (int i = 0; i < h.buttons.size(); i++) {
                final String letter = letters[i];
                final RadioButton rb = h.buttons.get(i);
                String opt = h.q.options.get(letter);
                rb.setOnCheckedChangeListener(null);
                if (opt == null) {
                    rb.setVisibility(View.GONE);
                    continue;
                }
                rb.setVisibility(View.VISIBLE);
                rb.setText(opt);
                rb.setTextColor(getResources().getColor(R.color.text_common));
                rb.setChecked(letter.equals(selected[pos]));
                rb.setClickable(!submitted);
                rb.setFocusable(!submitted);
                rb.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(android.widget.CompoundButton b2, boolean checked) {
                        if (checked && !submitted) selected[pos] = letter;
                    }
                });
            }

            h.link.setText("原文");
            h.link.setPaintFlags(h.link.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            h.link.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v2) {
                    transcriptOpen[pos] = !transcriptOpen[pos];
                    h.box.setVisibility(transcriptOpen[pos] ? View.VISIBLE : View.GONE);
                    h.link.setText(transcriptOpen[pos] ? "收起" : "原文");
                    adapter.notifyDataSetChanged();
                }
            });

            if (submitted) applyResult(h, pos);
            else {
                h.resultRow.setVisibility(View.GONE);
                h.box.setVisibility(View.GONE);
            }
            return v;
        }
    }

    private void applyResult(Holder h, int pos) {
        Exam.Question q = h.q;
        String pick = selected[pos];
        String correct = q.answer;
        String[] letters = new String[]{"A", "B", "C"};
        for (int i = 0; i < h.buttons.size(); i++) {
            String letter = letters[i];
            RadioButton rb = h.buttons.get(i);
            String opt = q.options.get(letter);
            if (opt == null) continue;
            rb.setClickable(false);
            rb.setFocusable(false);
            if (letter.equals(correct)) {
                rb.setText(opt + "  √");
                rb.setTextColor(getResources().getColor(R.color.result_blue));
                rb.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            } else if (letter.equals(pick)) {
                rb.setText(opt + "  ×");
                rb.setTextColor(getResources().getColor(R.color.wrong_red));
            } else {
                rb.setText(opt);
                rb.setTextColor(getResources().getColor(R.color.text_grey));
            }
        }
        h.answerValue.setText(correct == null ? "" : correct);
        h.resultRow.setVisibility(View.VISIBLE);
        String text = exam.texts.get(q.textNo);
        h.box.setText(text == null || text.length() == 0 ? "这一套题没有本段原文。" : text);
        h.box.setVisibility(transcriptOpen[pos] ? View.VISIBLE : View.GONE);
        h.link.setText(transcriptOpen[pos] ? "收起" : "原文");
    }

    private void submit() {
        if (submitted) return;
        int missing = 0;
        for (String s : selected) if (s == null) missing++;
        if (missing > 0) {
            final int m = missing;
            new AlertDialog.Builder(this)
                    .setTitle("还有题目没答")
                    .setMessage("还有 " + m + " 题未作答，确定交卷吗？")
                    .setNegativeButton("继续作答", null)
                    .setPositiveButton("交卷", new android.content.DialogInterface.OnClickListener() {
                        public void onClick(android.content.DialogInterface d, int w) {
                            doSubmit();
                        }
                    }).show();
            return;
        }
        doSubmit();
    }

    private void doSubmit() {
        submitted = true;
        submitBtn.setText("重做");
        int right = 0;
        for (int i = 0; i < exam.questions.size(); i++) {
            if (exam.questions.get(i).answer.equals(selected[i])) right++;
        }
        adapter.notifyDataSetChanged();
        int total = exam.questions.size();
        int percent = total == 0 ? 0 : (int) Math.round(right * 100.0 / total);
        new AlertDialog.Builder(this)
                .setTitle("交卷结果")
                .setMessage("共 " + total + " 题，答对 " + right + " 题\n正确率 " + percent + "%")
                .setPositiveButton("知道了", null)
                .show();
    }

    private void showAllTexts() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 20; i++) {
            String s = exam.texts.get(i);
            if (s == null) continue;
            sb.append("Text ").append(i).append("\n").append(s).append("\n\n");
        }
        TextView tv = new TextView(this);
        tv.setText(sb.length() == 0 ? "没有原文" : sb.toString());
        tv.setTextSize(14);
        tv.setTextColor(getResources().getColor(R.color.text_common));
        tv.setLineSpacing(0, 1.4f);
        tv.setPadding(Ui.dp(this, 20), Ui.dp(this, 16), Ui.dp(this, 20), Ui.dp(this, 16));
        tv.setTextIsSelectable(true);
        ScrollView sv = new ScrollView(this);
        sv.addView(tv);
        new AlertDialog.Builder(this).setTitle("全部录音原文").setView(sv)
                .setPositiveButton("关闭", null).show();
    }

    private String fmt(int ms) {
        int total = Math.max(0, ms) / 1000;
        return String.format(Locale.US, "%02d:%02d", total / 60, total % 60);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(tick);
        if (player != null) {
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
        super.onDestroy();
    }
}
