#!/usr/bin/env python3
"""Google Play phone screenshots from the update recordings in promo/footage.

  python promo/make_play_screenshots.py

Writes promo/play/01-….png … (1440×2560, 9:16, 24-bit PNG without alpha) and promo/play/alt-text.txt.
Each screenshot is a frame of a real screen recording of the app (demo data) under a one-line caption.
Google's rules, from the Play Console help: tagline no more than 20% of the image, no "New", "Free",
"Best" or calls to action, no other store's badge, no notifications in the status bar, no alpha.

Needs ffmpeg and Pillow.
"""
import subprocess
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent
FOOTAGE, OUT, WORK = ROOT / "footage", ROOT / "play", ROOT / "play" / "work"
FONTS = ROOT.parent / "app/src/main/res/font"

W, H = 1440, 2560
CAPTION_H = 310                      # 12% of the height; Google allows 20%
SCREEN_W = 1000                      # the recording is 1080 wide, scaled to this
BAR = 112                            # height of the recording's status bar, in recording pixels (1080 wide)

# order matters: the first two screenshots do most of the converting
SHOTS = [
    dict(clip="home_light", t=0.5, caption="Secure 2FA codes at a glance",
         alt="Authenticator app home screen listing two-factor codes with live countdown rings for Google, GitHub and Proton"),
    dict(clip="cloud", t=18.5, caption="Encrypted cloud backup",
         alt="Cloud backup settings with auto-backup on, encrypted on the device with the vault password, and a Back Up Now button"),
    dict(clip="themes2", t=3.5, caption="Fingerprint unlock & auto-lock",
         alt="Security settings with biometric unlock for fingerprint or face, a session timeout and local encrypted backup options"),
    dict(clip="cloud", t=3.5, caption="Zero-knowledge encryption",
         alt="Turn on cloud backup dialog: backups are encrypted with your vault password on this device, so only you can restore them"),
    dict(clip="restore", t=0.5, caption="Restore on any new phone",
         alt="Create your vault screen with biometric unlock and options to restore from a backup file or from VaultSpec Cloud"),
    dict(clip="themes2", t=9.5, caption="Hide codes, block screenshots",
         alt="Behavior settings: block screenshots, tap to reveal codes, tap to copy, highlight on tap and a password reminder"),
    dict(clip="themes2", t=17.0, caption="Light, dark & pitch black",
         alt="Authenticator codes in the pitch black theme for AMOLED screens, with Google, GitHub, Proton and Discord accounts"),
]


def font(weight, size):
    return ImageFont.truetype(str(FONTS / f"inter_{weight}.ttf"), size)


def frame(clip, seconds, name):
    WORK.mkdir(parents=True, exist_ok=True)
    out = WORK / f"{name}.png"
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-ss", str(seconds), "-i", str(FOOTAGE / f"{clip}.mp4"),
                    "-frames:v", "1", str(out)], check=True)
    return Image.open(out).convert("RGB")


def clean_status_bar(shot):
    """Replaces the recording's status bar (clock, notification icons) with a plain one: 9:41, full signal,
    Wi-Fi and battery. Google asks for no notifications and full indicators."""
    w = shot.width
    bg = shot.getpixel((12, BAR + 8))
    dark = sum(bg) / 3 < 128
    ink = (245, 245, 247) if dark else (20, 20, 24)
    d = ImageDraw.Draw(shot)
    d.rectangle((0, 0, w, BAR), fill=bg)
    cy = BAR / 2 - 6
    d.text((60, cy), "9:41", font=font("semibold", 40), fill=ink, anchor="lm")
    x = w - 60 - 58
    d.rounded_rectangle((x, cy - 15, x + 52, cy + 15), radius=8, outline=ink, width=3)
    d.rounded_rectangle((x + 5, cy - 10, x + 47, cy + 10), radius=4, fill=ink)
    d.rectangle((x + 53, cy - 6, x + 57, cy + 6), fill=ink)
    bx = x - 76
    for i, h in enumerate((10, 16, 22, 28)):
        d.rounded_rectangle((bx + i * 11, cy + 14 - h, bx + i * 11 + 7, cy + 14), radius=2, fill=ink)
    wx, wy = bx - 34, cy + 12
    for r in (30, 20, 10):
        d.arc((wx - r, wy - r, wx + r, wy + r), 225, 315, fill=ink, width=5)
    return shot


def background():
    img = Image.new("RGB", (W, H), (7, 12, 32))
    glow = Image.new("RGB", (W, H), (7, 12, 32))
    d = ImageDraw.Draw(glow)
    d.ellipse((-300, 500, W + 300, 2300), fill=(14, 52, 150))
    d.ellipse((300, -300, W + 400, 700), fill=(30, 40, 110))
    return Image.blend(img, glow.filter(ImageFilter.GaussianBlur(260)), 0.9)


def fit_caption(d, text, max_width=1260):
    size = 104
    while size > 64 and d.textlength(text, font=font("bold", size)) > max_width:
        size -= 2
    return font("bold", size)


def compose(shot, caption):
    img = background()
    d = ImageDraw.Draw(img)

    fnt = fit_caption(d, caption)
    tw = d.textlength(caption, font=fnt)
    d.text(((W - tw) / 2, CAPTION_H / 2 + 6), caption, font=fnt, fill=(255, 255, 255), anchor="lm")

    # The recording's screen, scaled and clipped to rounded corners, with a thin black bezel and soft shadow
    sh = round(shot.height * SCREEN_W / shot.width)
    screen = shot.resize((SCREEN_W, sh), Image.LANCZOS)
    x, y = (W - SCREEN_W) // 2, CAPTION_H
    radius, bezel = 74, 12
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle((x - bezel, y - bezel + 24, x + SCREEN_W + bezel, y + sh + bezel + 24),
                                             radius=radius + bezel, fill=(0, 0, 0, 190))
    blurred = shadow.filter(ImageFilter.GaussianBlur(30))
    img.paste(blurred, (0, 0), blurred)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((x - bezel, y - bezel, x + SCREEN_W + bezel, y + sh + bezel), radius=radius + bezel, fill=(4, 5, 8))
    d.rounded_rectangle((x - bezel, y - bezel, x + SCREEN_W + bezel, y + sh + bezel), radius=radius + bezel,
                        outline=(70, 78, 98), width=2)
    mask = Image.new("L", (SCREEN_W * 2, sh * 2), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, SCREEN_W * 2 - 1, sh * 2 - 1), radius=radius * 2, fill=255)
    img.paste(screen, (x, y), mask.resize((SCREEN_W, sh), Image.LANCZOS))
    return img


def main():
    OUT.mkdir(exist_ok=True)
    notes = []
    for i, spec in enumerate(SHOTS, start=1):
        shot = clean_status_bar(frame(spec["clip"], spec["t"], f"{spec['clip']}_{spec['t']}"))
        img = compose(shot, spec["caption"])
        slug = "".join(c.lower() if c.isalnum() else "-" for c in spec["caption"]).strip("-")
        slug = "-".join(part for part in slug.split("-") if part)
        path = OUT / f"{i:02d}-{slug}.png"
        img.convert("RGB").save(path, optimize=True)
        assert len(spec["alt"]) <= 140, f"alt text too long ({len(spec['alt'])}): {spec['alt']}"
        notes.append(f"{path.name}\n  caption: {spec['caption']}\n  alt ({len(spec['alt'])}): {spec['alt']}\n")
        print(path.name, img.size)
    (OUT / "alt-text.txt").write_text("\n".join(notes))


if __name__ == "__main__":
    main()
