package com.gkyytlzt.lynn;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class IndexStore {
    public static class Entry {
        public String id;
        public String title;
        public int year;
        public String series;
        public String audio;
        public int questionCount;
    }

    private static File file(Context c) {
        return new File(c.getFilesDir(), "index.json");
    }

    public static List<Entry> load(Context c) {
        List<Entry> out = new ArrayList<>();
        File f = file(c);
        if (!f.exists()) return out;
        try {
            byte[] b = readAll(new FileInputStream(f));
            JSONObject root = new JSONObject(new String(b, StandardCharsets.UTF_8));
            JSONArray arr = root.optJSONArray("exams");
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Entry e = new Entry();
                e.id = o.optString("id");
                e.title = o.optString("title");
                e.year = o.optInt("year");
                e.series = o.optString("series");
                e.audio = o.optString("audio");
                e.questionCount = o.optInt("questionCount");
                out.add(e);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static void save(Context c, List<Entry> list) {
        try {
            JSONArray arr = new JSONArray();
            for (Entry e : list) {
                JSONObject o = new JSONObject();
                o.put("id", e.id);
                o.put("title", e.title);
                o.put("year", e.year);
                o.put("series", e.series);
                o.put("audio", e.audio);
                o.put("questionCount", e.questionCount);
                arr.put(o);
            }
            JSONObject root = new JSONObject();
            root.put("exams", arr);
            FileOutputStream fos = new FileOutputStream(file(c));
            fos.write(root.toString().getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (Exception ignored) {
        }
    }

    public static void upsert(Context c, Exam exam) {
        List<Entry> list = load(c);
        Entry found = null;
        for (Entry e : list) {
            if (e.id.equals(exam.id)) {
                found = e;
                break;
            }
        }
        if (found == null) {
            found = new Entry();
            list.add(found);
        }
        found.id = exam.id;
        found.title = exam.title;
        found.year = exam.year;
        found.series = exam.series;
        found.audio = exam.audio;
        found.questionCount = exam.questions.size();
        save(c, list);
    }

    public static void remove(Context c, String id) {
        List<Entry> list = load(c);
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).id.equals(id)) list.remove(i);
        }
        save(c, list);
    }

    static byte[] readAll(FileInputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        in.close();
        return bos.toByteArray();
    }
}
