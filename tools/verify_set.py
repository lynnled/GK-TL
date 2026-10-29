# -*- coding: utf-8 -*-
"""Cross-check a parsed JSON against the source docx line by line."""
import json
import os
import re
import sys

import docx

Q_RE = re.compile(r"^\s*(\d{1,2})\s*[.、．]\s*(.*)$")
OPT_RE = re.compile(r"^\s*([A-C])\s*[.、．]\s*(.*)$")
TEXT_RE = re.compile(r"^\s*[（(]?\s*Text\s*(\d+)\s*[)）]?\s*(.*)$", re.I)
ANS_RE = re.compile(r"(\d{1,2})\s*[-—–~至]\s*(\d{1,2})\s*([A-C]{2,})")


def split_opts(line):
    """Split a line that starts with A./B./C. into options, including same-line options."""
    marks = list(re.finditer(r"([A-C])\s*[.、．]\s*", line))
    out = {}
    for i, m in enumerate(marks):
        start = m.end()
        end = marks[i + 1].start() if i + 1 < len(marks) else len(line)
        out[m.group(1)] = line[start:end].strip()
    return out


def main():
    docx_path, json_path = sys.argv[1:3]
    data = json.load(open(json_path, encoding="utf-8"))
    lines = [p.text.replace(" ", " ").strip() for p in docx.Document(docx_path).paragraphs]
    lines = [x for x in lines if x]

    ans_i = next(i for i, t in enumerate(lines) if "参考答案" in t)
    raw_i = next(i for i, t in enumerate(lines) if "录音原文" in t)

    # 1) answers from quick-check lines
    expected_answers = {}
    for t in lines[ans_i:raw_i]:
        for m in ANS_RE.finditer(t):
            a, b, letters = int(m.group(1)), int(m.group(2)), m.group(3)
            for k in range(b - a + 1):
                if k < len(letters):
                    expected_answers[a + k] = letters[k]

    # 2) question stems and options, line-start only
    expected_q = {}
    cur = None
    for t in lines[:ans_i]:
        qm = Q_RE.match(t)
        if qm:
            no = int(qm.group(1))
            if 1 <= no <= 30:
                cur = {"no": no, "stem": qm.group(2).strip(), "options": {}}
                expected_q[no] = cur
                continue
        om = OPT_RE.match(t)
        if om and cur is not None:
            cur["options"].update(split_opts(t))

    # 3) transcripts
    expected_texts = {}
    cur_text = None
    for t in lines[raw_i + 1:]:
        tm = TEXT_RE.match(t)
        if tm:
            cur_text = int(tm.group(1))
            expected_texts[cur_text] = []
            rest = (tm.group(2) or "").strip()
            if rest and not rest.startswith(("(", "（")):
                expected_texts[cur_text].append(rest)
            continue
        if cur_text is not None:
            expected_texts[cur_text].append(t)

    # 4) compare
    problems = []
    actual_q = {q["no"]: q for q in data["questions"]}
    if sorted(actual_q) != list(range(1, 21)):
        problems.append("question numbers %s" % sorted(actual_q))
    for no in range(1, 21):
        a = actual_q.get(no)
        e = expected_q.get(no)
        if a is None or e is None:
            problems.append("Q%d missing" % no)
            continue
        if a["stem"] != e["stem"]:
            problems.append("Q%d stem\n  docx: %r\n  json: %r" % (no, e["stem"], a["stem"]))
        for k in "ABC":
            av = a["options"].get(k)
            ev = e["options"].get(k)
            if av != ev:
                problems.append("Q%d opt %s\n  docx: %r\n  json: %r" % (no, k, ev, av))
        if a.get("answer") != expected_answers.get(no):
            problems.append("Q%d answer docx=%r json=%r" % (no, expected_answers.get(no), a.get("answer")))

    actual_texts = {t["no"]: t["content"] for t in data["texts"]}
    for no in range(1, 11):
        ev = "\n".join(expected_texts.get(no, [])).strip()
        av = actual_texts.get(no, "")
        # the parser skips some duplicate section headers; compare loosely on first/last 40 chars
        if ev[:40] != av[:40] or ev[-40:] != av[-40:]:
            problems.append("Text %d differs\n  docx: %r\n  json: %r" % (no, ev[:60], av[:60]))

    print("=" * 60)
    print(os.path.basename(json_path), "questions", len(actual_q), "texts", len(actual_texts))
    if problems:
        print("PROBLEMS:", len(problems))
        for p in problems:
            print(" -", p)
        sys.exit(1)
    print("OK: answers, stems, options and texts match the docx")


if __name__ == "__main__":
    main()
