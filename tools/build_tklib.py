# -*- coding: utf-8 -*-
"""Build a .tklib package (zip: manifest.json + audio.mp3) from a parsed JSON."""
import hashlib
import json
import os
import sys
import zipfile


def main():
    json_path, mp3_path, out_path = sys.argv[1:4]
    with open(json_path, encoding="utf-8") as f:
        m = json.load(f)
    m.pop("_problems", None)
    with open(mp3_path, "rb") as f:
        blob = f.read()
    m["audioBytes"] = len(blob)
    m["audioSha256"] = hashlib.sha256(blob).hexdigest()
    m["packageVersion"] = 1

    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with zipfile.ZipFile(out_path, "w") as z:
        z.writestr("manifest.json", json.dumps(m, ensure_ascii=False, indent=1).encode("utf-8"), zipfile.ZIP_DEFLATED)
        z.writestr("audio.mp3", blob, zipfile.ZIP_STORED)
    print("wrote %s (%d bytes, %d questions)" % (out_path, os.path.getsize(out_path), len(m["questions"])))


if __name__ == "__main__":
    main()
