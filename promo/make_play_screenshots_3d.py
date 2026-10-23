#!/usr/bin/env python3
"""Google Play screenshots in the 3D "enterprise" style of the App Store set: the real screen on a tilted,
extruded phone in a dark studio, under an eyebrow, a one-line headline and a row of badges.

  python promo/make_play_screenshots_3d.py

Reuses the backdrop and 3D device code of the App Store renderer (auth-ios marketing/appstore/source/
render_store.py) with an Android front: punch-hole camera instead of the Dynamic Island, flatter corners.
Frames and status-bar cleanup come from make_play_screenshots.py. Writes promo/play/3d/*.png (1440×2560).
Text stays in the top 17% of the image (Google allows 20%). Needs ffmpeg, Pillow and NumPy.
"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
OUT = ROOT / "play" / "3d"
RENDERER = Path("/Volumes/Projects/auth-ios/marketing/appstore/source/render_store.py")

sys.path.insert(0, str(ROOT))
from make_play_screenshots import clean_status_bar, frame  # noqa: E402

# Load the shared part of the App Store renderer (everything above its layouts) with an Android device face
source = RENDERER.read_text()
shared = source[: source.index("# ─── iPhone")]
island = '''    if kind == "phone":
        iw, ih, top = sw * 0.286, sw * 0.084, sw * 0.025
        cx = W / 2
        d.rounded_rectangle([cx - iw / 2, ring + bezel + top, cx + iw / 2, ring + bezel + top + ih], ih / 2, fill=(0, 0, 0, 255))
'''
punch_hole = '''    if kind == "phone":
        r, cx, cy = sw * 0.021, W / 2, ring + bezel + sw * 0.047
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(0, 0, 0, 255))
'''
assert island in shared, "renderer changed: Dynamic Island block not found"
shared = shared.replace(island, punch_hole).replace("int(sw * 0.125)", "int(sw * 0.085)")
ns = {"__name__": "renderer"}
sys.argv = [str(RENDERER), "", str(OUT)]
exec(compile(shared, str(RENDERER), "exec"), ns)
backdrop, place, badges, font, SS = ns["backdrop"], ns["place"], ns["badges"], ns["font"], ns["SS"]
Image, ImageDraw = ns["Image"], ns["ImageDraw"]

W, H = 1440, 2560
SKY, WHITE = (90, 170, 255), (255, 255, 255)
GREEN, BLUE, AMBER, VIOLET = (52, 211, 153), (10, 132, 255), (255, 176, 32), (167, 139, 250)

# Order matters: the first two do most of the converting. Yaw alternates so the set reads as a sequence.
SHOTS = [
    dict(clip="home_light", t=0.5, name="01-secure-2fa-codes", eyebrow="VaultSpec Authenticator",
         title="Secure 2FA codes at a glance", badges=[("Live countdown", BLUE), ("Tap to copy", GREEN), ("Works offline", VIOLET)], yaw=-18),
    dict(clip="cloud", t=18.5, name="02-encrypted-cloud-backup", eyebrow="Cloud backup",
         title="Encrypted cloud backup", badges=[("AES-256-GCM", GREEN), ("Automatic", BLUE), ("Restore anywhere", VIOLET)], yaw=18),
    dict(clip="themes2", t=3.5, name="03-fingerprint-unlock", eyebrow="Security",
         title="Fingerprint unlock & auto-lock", badges=[("Fingerprint or face", BLUE), ("Session timeout", AMBER)], yaw=-16),
    dict(clip="cloud", t=3.5, name="04-zero-knowledge", eyebrow="Privacy",
         title="Zero-knowledge encryption", badges=[("Your vault password", GREEN), ("We can't read your codes", AMBER)], yaw=16),
    dict(clip="restore", t=0.5, name="05-restore-any-phone", eyebrow="Switch phones",
         title="Restore on any phone", badges=[("Backup file", BLUE), ("VaultSpec Cloud", GREEN)], yaw=-16),
    dict(clip="themes2", t=9.5, name="06-hide-codes", eyebrow="Security controls",
         title="Hide codes, block screenshots", badges=[("Block screenshots", GREEN), ("Tap to reveal", AMBER)], yaw=16),
    dict(clip="themes2", t=17.0, name="07-themes", eyebrow="Appearance",
         title="Light, dark & pitch black", badges=[("System theme", BLUE), ("AMOLED black", VIOLET)], yaw=-18),
]


def header(canvas, eyebrow, title, items):
    """Eyebrow, a one-line headline sized to fit and a badge row, all within the top ~17% of the image."""
    d = ImageDraw.Draw(canvas)
    w = canvas.size[0]
    small = font("semibold", 30)
    text, spacing = eyebrow.upper(), 5 * SS
    widths = [d.textlength(ch, font=small) for ch in text]
    x = (w - sum(widths) - spacing * (len(text) - 1)) / 2
    for ch, cw in zip(text, widths):
        d.text((x, 120 * SS), ch, font=small, fill=SKY)
        x += cw + spacing
    size = 100
    while size > 64 and d.textlength(title, font=font("bold", size)) > 1320 * SS:
        size -= 2
    big = font("bold", size)
    d.text(((w - d.textlength(title, font=big)) / 2, 172 * SS), title, font=big, fill=WHITE)
    badges(canvas, items, 318, scale=1.0)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for spec in SHOTS:
        shot = clean_status_bar(frame(spec["clip"], spec["t"], f"{spec['clip']}_{spec['t']}"))
        canvas = backdrop(W * SS, H * SS, glow=(0.5, 0.66))
        header(canvas, spec["eyebrow"], spec["title"], spec["badges"])
        tilt = 1 if spec["yaw"] > 0 else -1
        place(canvas, shot, "phone", 720, 1560, 1980, yaw=spec["yaw"], pitch=7, roll=3 * tilt)
        img = canvas.convert("RGB").resize((W, H), Image.LANCZOS)
        img.save(OUT / f"{spec['name']}.png", optimize=True)
        print(spec["name"], img.size)


if __name__ == "__main__":
    main()
