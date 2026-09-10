import re
import shutil
import sys
import tempfile
import zipfile
import xml.sax.saxutils as xml_utils
from pathlib import Path


DOCX_PATH = Path(r"D:\FPTK8\SBA301\BE-LastUpdate.docx")
OUTPUT_PATH = Path(r"D:\FPTK8\SBA301\BE-Phase0-Complete.docx")
BACKUP_PATH = DOCX_PATH.with_name("BE-LastUpdate.before-phase0-complete.docx")

REPLACEMENTS = {
    "- Security config co ban.": "- Security config cơ bản.",
    "- Request logging.": "- Không còn hạng mục Phase 0 bắt buộc.",
    "- Correlation id.": "",
    "- Security config gắn JWT filter thật.": "",
    "- Test riêng cho exception/validation.": "",
}

INSERT_AFTER = {
    "- `PasswordEncoder`.": [
        "- `JwtProperties`.",
        "- `JwtService`.",
        "- `JwtAuthenticationFilter`.",
        "- Security config gắn JWT filter thật.",
        "- Request logging.",
        "- Correlation id.",
        "- Test riêng cho exception/validation.",
        "- Foundation integration tests cho correlation id và protected endpoint.",
    ],
}

PARAGRAPH_RE = re.compile(r"<w:p\b.*?</w:p>", re.DOTALL)
TEXT_RE = re.compile(r"(<w:t\b[^>]*>)(.*?)(</w:t>)", re.DOTALL)


def paragraph_text(paragraph_xml: str) -> str:
    return "".join(xml_utils.unescape(match.group(2)) for match in TEXT_RE.finditer(paragraph_xml)).strip()


def set_paragraph_text(paragraph_xml: str, value: str) -> str:
    escaped = xml_utils.escape(value)
    first = True

    def repl(match: re.Match[str]) -> str:
        nonlocal first
        if first:
            first = False
            return f"{match.group(1)}{escaped}{match.group(3)}"
        return f"{match.group(1)}{match.group(3)}"

    return TEXT_RE.sub(repl, paragraph_xml)


def patch_xml(xml_text: str) -> tuple[str, int, int]:
    changed = 0
    inserted = 0
    seen_replacements = set()
    seen_inserts = set()

    def patch_paragraph(match: re.Match[str]) -> str:
        nonlocal changed, inserted
        paragraph_xml = match.group(0)
        text = paragraph_text(paragraph_xml)
        new_text = REPLACEMENTS.get(text)
        if new_text is not None:
            paragraph_xml = set_paragraph_text(paragraph_xml, new_text)
            seen_replacements.add(text)
            changed += 1
            text = new_text

        additions = INSERT_AFTER.get(text)
        if additions is None:
            return paragraph_xml
        seen_inserts.add(text)
        inserted += len(additions)
        return paragraph_xml + "".join(set_paragraph_text(paragraph_xml, item) for item in additions)

    patched = PARAGRAPH_RE.sub(patch_paragraph, xml_text)
    missing_replacements = set(REPLACEMENTS) - seen_replacements
    missing_inserts = set(INSERT_AFTER) - seen_inserts
    if missing_replacements or missing_inserts:
        for text in sorted(missing_replacements):
            print(f"Replacement target not found: {text}", file=sys.stderr)
        for text in sorted(missing_inserts):
            print(f"Insert target not found: {text}", file=sys.stderr)
        raise SystemExit(2)
    return patched, changed, inserted


def main() -> int:
    if not DOCX_PATH.exists():
        print(f"Missing file: {DOCX_PATH}", file=sys.stderr)
        return 1
    if not BACKUP_PATH.exists():
        shutil.copy2(DOCX_PATH, BACKUP_PATH)

    with tempfile.TemporaryDirectory() as tmp:
        out_path = Path(tmp) / DOCX_PATH.name
        changed = 0
        inserted = 0
        with zipfile.ZipFile(DOCX_PATH, "r") as zin, zipfile.ZipFile(out_path, "w", zipfile.ZIP_DEFLATED) as zout:
            for item in zin.infolist():
                data = zin.read(item.filename)
                if item.filename == "word/document.xml":
                    text, changed, inserted = patch_xml(data.decode("utf-8"))
                    data = text.encode("utf-8")
                zout.writestr(item, data)
        try:
            shutil.copy2(out_path, DOCX_PATH)
            written_path = DOCX_PATH
        except PermissionError:
            shutil.copy2(out_path, OUTPUT_PATH)
            written_path = OUTPUT_PATH

    print(f"Updated {written_path}")
    print(f"Backup: {BACKUP_PATH}")
    print(f"Replacements: {changed}")
    print(f"Inserted paragraphs: {inserted}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
