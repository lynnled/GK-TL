package com.gkyytlzt.lynn;

import android.content.Context;
import android.net.Uri;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TklibImporter {

    public static class Result {
        public Exam exam;
        public String error;
    }

    public static Result importUri(Context ctx, Uri uri) {
        Result r = new Result();
        try {
            InputStream is = ctx.getContentResolver().openInputStream(uri);
            if (is == null) {
                r.error = "无法打开文件";
                return r;
            }
            return importStream(ctx, is);
        } catch (Exception e) {
            r.error = e.getMessage() == null ? e.toString() : e.getMessage();
            return r;
        }
    }

    public static Result importFile(Context ctx, File file) {
        Result r = new Result();
        try {
            return importStream(ctx, new FileInputStream(file));
        } catch (Exception e) {
            r.error = e.getMessage() == null ? e.toString() : e.getMessage();
            return r;
        }
    }

    private static Result importStream(Context ctx, InputStream is) {
        Result r = new Result();
        ZipInputStream zin = null;
        try {
            zin = new ZipInputStream(is);
            byte[] manifestBytes = null;
            String audioEntry = null;
            File audioTmp = null;
            ZipEntry ze;
            byte[] buf = new byte[64 * 1024];
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.isDirectory()) continue;
                String name = ze.getName();
                if (name.endsWith("manifest.json")) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    int n;
                    while ((n = zin.read(buf)) > 0) {
                        bos.write(buf, 0, n);
                        if (bos.size() > 4 * 1024 * 1024) throw new Exception("manifest 过大");
                    }
                    manifestBytes = bos.toByteArray();
                } else if (name.endsWith(".mp3")) {
                    audioEntry = new File(name).getName();
                    audioTmp = new File(ctx.getCacheDir(), "import.mp3");
                    FileOutputStream fos = new FileOutputStream(audioTmp);
                    int n;
                    while ((n = zin.read(buf)) > 0) fos.write(buf, 0, n);
                    fos.close();
                }
                zin.closeEntry();
            }
            if (manifestBytes == null) throw new Exception("包内缺少 manifest.json");
            if (audioTmp == null || !audioTmp.exists()) throw new Exception("包内缺少 mp3");
            JSONObject mo = new JSONObject(new String(manifestBytes, StandardCharsets.UTF_8));
            if (mo.optInt("schema") != 1) throw new Exception("不支持的包版本");
            String id = mo.optString("id");
            if (!id.matches("[A-Za-z0-9_-]{1,64}")) throw new Exception("非法题库 ID");
            String expect = mo.optString("audioSha256");
            if (expect != null && expect.length() == 64) {
                String got = sha256(audioTmp);
                if (!expect.equalsIgnoreCase(got)) throw new Exception("音频校验失败");
            }
            Exam exam = Exam.fromJson(mo);
            if (exam.questions.isEmpty()) throw new Exception("题量为 0");
            exam.audio = audioEntry;

            File dir = new File(new File(ctx.getFilesDir(), "exams"), id);
            deleteRec(dir);
            if (!dir.mkdirs()) throw new Exception("无法创建目录");
            FileOutputStream mf = new FileOutputStream(new File(dir, "manifest.json"));
            mf.write(manifestBytes);
            mf.close();
            File audioDst = new File(dir, exam.audio);
            if (!audioTmp.renameTo(audioDst)) {
                FileInputStream in = new FileInputStream(audioTmp);
                FileOutputStream out = new FileOutputStream(audioDst);
                byte[] b2 = new byte[64 * 1024];
                int n;
                while ((n = in.read(b2)) > 0) out.write(b2, 0, n);
                in.close();
                out.close();
                audioTmp.delete();
                if (!audioDst.exists()) throw new Exception("保存音频失败");
            }
            IndexStore.upsert(ctx, exam);
            r.exam = exam;
            return r;
        } catch (Exception e) {
            r.error = e.getMessage() == null ? e.toString() : e.getMessage();
            return r;
        } finally {
            if (zin != null) {
                try { zin.close(); } catch (Exception ignored) {}
            }
        }
    }

    private static String sha256(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        FileInputStream in = new FileInputStream(f);
        byte[] buf = new byte[64 * 1024];
        int n;
        while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
        in.close();
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    static void deleteRec(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) deleteRec(k);
        }
        f.delete();
    }
}
