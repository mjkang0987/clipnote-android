#!/usr/bin/env python3
"""iOS 문자열 카탈로그(`Localizable.xcstrings`) → 안드로이드 `strings.xml` 4벌.

문구의 source of truth 는 웹 `clipnote` 사전이고, iOS 가 그걸 카탈로그로 들고 있다.
안드로이드가 문구를 따로 적으면 세 곳이 조금씩 갈린다 — 그래서 **베끼지 않고 변환한다.**

    python3 scripts/sync-localizations.py ../clipnote-ios/Shared/Localization/Localizable.xcstrings

키 이름은 `home.hero.title` → `home_hero_title` 로 바꾼다(안드로이드 리소스명에 점을 못 쓴다).
포맷 지시자는 `%@`→`%s`, `%lld`→`%d`, `%1$@`→`%1$s`.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from xml.sax.saxutils import escape

# iOS 언어 코드 → 안드로이드 리소스 디렉터리. ko 는 기본값(values/)이다 — 원본 언어라
# 번역이 빠진 키가 있어도 시스템 폴백이 한국어로 떨어진다(iOS `t(_:)` 의 폴백과 같은 결과).
DIRS = {"ko": "values", "en": "values-en", "ja": "values-ja", "zh-Hans": "values-zh-rCN"}

SPECIFIER = re.compile(r"%(\d+\$)?(@|lld|ld|d)")


def res_name(key: str) -> str:
    name = re.sub(r"[^0-9a-zA-Z]+", "_", key)
    return name[0].lower() + name[1:] if name else name


def convert_specifiers(text: str) -> str:
    def repl(m: re.Match[str]) -> str:
        index, kind = m.group(1) or "", m.group(2)
        return f"%{index}{'s' if kind == '@' else 'd'}"

    return SPECIFIER.sub(repl, text)


def xml_value(text: str) -> str:
    body = escape(convert_specifiers(text))
    # 안드로이드 문자열 리터럴에서 살아 있는 문자들 — 이스케이프하지 않으면 조용히 잘리거나
    # 리소스 참조로 해석된다.
    body = body.replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"')
    body = body.replace("\n", "\\n")
    if body.startswith(("@", "?")):
        body = "\\" + body
    return body


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__)
        return 2

    catalog = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
    strings = catalog["strings"]

    names: dict[str, str] = {}
    for key in strings:
        name = res_name(key)
        if name in names:
            print(f"리소스명 충돌: {key} 와 {names[name]} 가 모두 {name}")
            return 1
        names[name] = key

    root = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res"
    for lang, directory in DIRS.items():
        out = root / directory
        out.mkdir(parents=True, exist_ok=True)
        lines = [
            '<?xml version="1.0" encoding="utf-8"?>',
            "<!-- 자동 생성: scripts/sync-localizations.py. 직접 고치지 말 것 -->",
            "<resources>",
        ]
        missing = []
        for key in sorted(strings):
            unit = strings[key]["localizations"].get(lang, {}).get("stringUnit")
            if unit is None:
                missing.append(key)
                continue
            lines.append(f'    <string name="{res_name(key)}">{xml_value(unit["value"])}</string>')
        lines.append("</resources>")
        (out / "strings.xml").write_text("\n".join(lines) + "\n", encoding="utf-8")
        note = f" (번역 누락 {len(missing)}개)" if missing else ""
        print(f"{directory}/strings.xml — {len(strings) - len(missing)}개{note}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
