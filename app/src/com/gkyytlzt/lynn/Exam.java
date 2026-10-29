package com.gkyytlzt.lynn;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Exam {
    public String id;
    public String title;
    public int year;
    public String series;
    public String audio;
    public final List<Question> questions = new ArrayList<>();
    public final Map<Integer, String> texts = new HashMap<>();

    public static class Question {
        public int no;
        public String stem;
        public final Map<String, String> options = new HashMap<>();
        public String answer;
        public int textNo;
    }

    public static Exam fromJson(JSONObject o) {
        Exam e = new Exam();
        e.id = o.optString("id");
        e.title = o.optString("title");
        e.year = o.optInt("year");
        e.series = o.optString("series");
        e.audio = o.optString("audio", "audio.mp3");
        JSONArray qs = o.optJSONArray("questions");
        if (qs != null) {
            for (int i = 0; i < qs.length(); i++) {
                JSONObject qo = qs.optJSONObject(i);
                if (qo == null) continue;
                Question q = new Question();
                q.no = qo.optInt("no");
                q.stem = qo.optString("stem");
                q.answer = qo.optString("answer");
                q.textNo = qo.optInt("textNo");
                JSONObject ops = qo.optJSONObject("options");
                if (ops != null) {
                    for (String k : new String[]{"A", "B", "C"}) {
                        if (ops.has(k)) q.options.put(k, ops.optString(k));
                    }
                }
                e.questions.add(q);
            }
        }
        JSONArray ts = o.optJSONArray("texts");
        if (ts != null) {
            for (int i = 0; i < ts.length(); i++) {
                JSONObject to = ts.optJSONObject(i);
                if (to != null) e.texts.put(to.optInt("no"), to.optString("content"));
            }
        }
        return e;
    }

    public String meta() {
        return year + "  " + series + "  ·  " + questions.size() + " 题";
    }
}
