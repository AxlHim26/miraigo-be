#!/usr/bin/env python3
"""Export the fields used by the mobile Kanji map from the FE catalog."""
import argparse
import json
import re
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("catalog", type=Path)
args = parser.parse_args()
entries = {}
for file in sorted(args.catalog.glob("*.json")):
    if len(file.stem) != 1 or not re.fullmatch(r"[\u3400-\u9fff\U00020000-\U000323af]", file.stem):
        continue
    raw = json.loads(file.read_text(encoding="utf-8"))
    jisho = raw.get("jishoData") or {}
    radical = (jisho.get("radical") or {}).get("symbol")
    parts = [p for p in (jisho.get("parts") or []) if isinstance(p, str)]
    animation = jisho.get("strokeOrderGifUri") or ""
    if not re.fullmatch(r"https://raw\.githubusercontent\.com/mistval/kanji_images/master/gifs/[a-fA-F0-9]+\.gif", animation):
        animation = None
    entries[file.stem] = {"radical": radical, "parts": parts, "strokeOrderGif": animation}
print(json.dumps(entries, ensure_ascii=False, separators=(",", ":")))
