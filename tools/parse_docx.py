# -*- coding: utf-8 -*-
"""Parse one 高考听力 docx into the tklib manifest JSON.

Usage:
  python parse_docx.py <docx> <mp3> <out.json> <id> <title> <year> <series>
"""
import json
import os
import re
import sys

import docx


def paras(path):
    d = docx.Document(path)
    out = []
    for p in d.paragraphs:
        t = (p.text or "").replace(" ", " ").strip()
        if t:
            out.append(t)
    return out


def opt_re():
    return re.compile(r"([A-C])\s*[.、．]\s*")


def split_options(line):
    """Return {A:..,B:..,C:..} from a line, supporting same-line options."""
    m = list(opt_re().finditer(line))
    if not m:
        return {}
    res = {}
    for i, mm in enumerate(m):
        letter = mm.group(1)
        start = mm.end()
        end = m[i + 1].start() if i + 1 < len(m) else len(line)
        res[letter] = line[start:end].strip()
    return res


ANSWER_RE = re.compile(r"(\d{1,2})\s*[-—–~至]\s*(\d{1,2})\s*([A-C]{2,})")
Q_RE = re.compile(r"^\s*(\d{1,2})\s*[.、．]\s*(.+)$")
TEXT_RE = re.compile(r"^\s*[（(]?\s*Text\s*(\d+)\s*[)）]?\s*(.*)$", re.I)
LISTEN_RE = re.compile(r"听第\s*(\d+)\s*段材料[，,]\s*回答第?\s*([0-9、,，至\-—~]+)\s*题")


def parse_answers(lines, start, end):
    answers = {}
    for line in lines[start:end]:
        for m in ANSWER_RE.finditer(line):
            a, b, letters = int(m.group(1)), int(m.group(2)), m.group(3)
            n = b - a + 1
            if len(letters) < n:
                continue
            for i in range(n):
                answers[a + i] = letters[i]
    return answers


def expand_numbers(s):
    out = []
    for part in re.split(r"[、,，\s]+", s):
        if not part:
            continue
        m = re.match(r"^(\d+)\s*[-—–~至]\s*(\d+)$", part)
        if m:
            a, b = int(m.group(1)), int(m.group(2))
            out.extend(range(a, b + 1))
        elif part.isdigit():
            out.append(int(part))
    return out


def main():
    docx_path, mp3_path, out_path, eid, title, year, series = sys.argv[1:8]
    lines = paras(docx_path)

    raw_i = next((i for i, t in enumerate(lines) if "录音原文" in t), None)
    ans_i = next((i for i, t in enumerate(lines) if "参考答案" in t or "答案速查" in t), None)
    if raw_i is None or ans_i is None:
        raise SystemExit("missing 参考答案/录音原文 marker")

    answers = parse_answers(lines, ans_i, raw_i if raw_i > ans_i else len(lines))

    questions = {}
    cur = None
    pending_text_no = None
    listen_map = {}
    for i in range(0, raw_i):
        t = lines[i]
        lm = LISTEN_RE.search(t)
        if lm:
            text_no = int(lm.group(1))
            for qn in expand_numbers(lm.group(2)):
                listen_map[qn] = text_no
            continue
        # 进入新的部分/章节或新的一段材料说明时，停止把后续文字算到上一题
        if re.match(r"^\s*(第[一二三四]部分|第[一二三四五]节|听下面|Text\s*\d+)", t, re.I):
            cur = None
            continue
        qm = Q_RE.match(t)
        if qm and not t.lower().startswith(("text",)):
            no = int(qm.group(1))
            if 1 <= no <= 30:
                cur = {"no": no, "stem": qm.group(2).strip(), "options": {}, "answer": answers.get(no, ""), "textNo": listen_map.get(no)}
                questions[no] = cur
                continue
        if cur is not None and re.match(r"^\s*[A-C]\s*[.、．]", t):
            opts = split_options(t)
            if opts:
                cur["options"].update(opts)
    # default text mapping: Q1-5 -> Text1-5, Q6-8 -> Text6, 9-11 -> 7, 12-14 -> 8, 15-17 -> 9, 18-20 -> 10
    default_text = {1: 1, 2: 2, 3: 3, 4: 4, 5: 5}
    for q in (6, 7, 8):
        default_text[q] = 6
    for q in (9, 10, 11):
        default_text[q] = 7
    for q in (12, 13, 14):
        default_text[q] = 8
    for q in (15, 16, 17):
        default_text[q] = 9
    for q in (18, 19, 20):
        default_text[q] = 10
    for q in questions.values():
        if not q["textNo"]:
            q["textNo"] = default_text.get(q["no"])

    texts = {}
    cur_text = None
    for t in lines[raw_i + 1:]:
        tm = TEXT_RE.match(t)
        if tm:
            cur_text = int(tm.group(1))
            texts[cur_text] = []
            rest = (tm.group(2) or "").strip()
            if rest and not rest.startswith(("(", "（")):
                texts[cur_text].append(rest)
            continue
        if cur_text is not None and not TEXT_RE.match(t):
            # skip repeated section headers that appear in some files
            if re.match(r"^第[一二]部分|^第[一二]节|^参考答案|^答案速查", t):
                continue
            texts[cur_text].append(t)
    texts_out = [{"no": k, "content": "\n".join(v).strip()} for k, v in sorted(texts.items())]

    qs = [questions[k] for k in sorted(questions)]
    by_no = {q["no"]: q for q in qs}
    manifest = {
        "schema": 1,
        "id": eid,
        "title": title,
        "year": int(year),
        "series": series,
        "audio": "audio.mp3",
        "questions": qs,
        "texts": texts_out,
    }
    problems = []
    if len(qs) != 20 or [q["no"] for q in qs] != list(range(1, 21)):
        problems.append("question numbers: %s" % [q["no"] for q in qs][:30])
    bad_words = ("选项中选出", "听每段", "听下面", "每小题", "读两遍", "作答时间")
    for q in qs:
        if sorted(q["options"]) != ["A", "B", "C"]:
            problems.append("Q%s options %s" % (q["no"], sorted(q["options"])))
        for k, v in q["options"].items():
            if not v.strip():
                problems.append("Q%s option %s empty" % (q["no"], k))
            elif any(w in v for w in bad_words):
                problems.append("Q%s option %s looks like instruction: %r" % (q["no"], k, v[:40]))
        if q["answer"] not in ("A", "B", "C"):
            problems.append("Q%s answer %r" % (q["no"], q["answer"]))
    if len(texts_out) != 10:
        problems.append("texts: %d" % len(texts_out))
    manifest["_problems"] = problems
    if os.path.exists(mp3_path):
        manifest["audioBytes"] = os.path.getsize(mp3_path)
    else:
        problems.append("mp3 missing")

    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=1)
    print("wrote %s questions=%d texts=%d problems=%d" % (out_path, len(qs), len(texts_out), len(problems)))
    for p in problems[:20]:
        print("  !", p)


if __name__ == "__main__":
    main()
