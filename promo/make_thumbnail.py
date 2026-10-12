#!/usr/bin/env python3
"""Renders thumbnails for the VaultSpec update video from frames of the screen recordings.

  python promo/make_thumbnail.py

Writes promo/thumbnail.png (1280×720, YouTube) and promo/thumbnail-vertical.png (1080×1920,
Shorts / Reels cover). Needs ffmpeg and Pillow.
"""
import subprocess
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

from make_video import ACCENT, BODY_COLOR, BODY_EDGE, BUILD, FOOTAGE, GRAY, ROOT, WHITE, font, logo

AMBER = (255, 184, 77)


def frame_at(clip, seconds):
    """One frame of a recording as an image."""
    BUILD.mkdir(exist_ok=True)
    out = BUILD / f"thumb_{clip}.png"
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-ss", str(seconds), "-i", str(FOOTAGE / f"{clip}.mp4"),
                    "-frames:v", "1", str(out)], check=True)
    return Image.open(out).convert("RGB")


def phone(screen, height):
    """The screenshot inside a Galaxy S25-style body, [height] pixels tall, on a transparent canvas."""
    k = 2
    sh = height * k
    sw = round(sh * screen.width / screen.height)
    radius, bezel, rail = round(sh * 0.034), round(sh * 0.0066), round(sh * 0.0044)
    body = bezel + rail
    w, h = sw + 2 * body + 8 * k, sh + 2 * body
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for top, length in ((0.22, 0.11), (0.365, 0.063)):   # volume and power keys
        d.rounded_rectangle((sw + 2 * body - 3 * k, sh * top, sw + 2 * body + 4 * k, sh * (top + length)),
                            radius=4 * k, fill=BODY_EDGE)
    d.rounded_rectangle((0, 0, sw + 2 * body, h), radius=radius + body, fill=BODY_EDGE)
    d.rounded_rectangle((2, 2, sw + 2 * body - 2, h - 2), radius=radius + body - 2, fill=BODY_COLOR)
    d.rounded_rectangle((rail, rail, sw + 2 * body - rail, h - rail), radius=radius + bezel, fill=(4, 4, 6))
    mask = Image.new("L", (sw, sh), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, sw - 1, sh - 1), radius=radius, fill=255)
    img.paste(screen.resize((sw, sh), Image.LANCZOS), (body, body), mask)
    cx, cy, r = body + sw / 2, body + sh * 0.022, sh * 0.0075
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(0, 0, 0))
    return img.resize((w // k, h // k), Image.LANCZOS)


def place(canvas, layer, x, y, angle=0):
    """Pastes [layer] with a soft drop shadow, optionally rotated."""
    if angle:
        layer = layer.rotate(angle, resample=Image.BICUBIC, expand=True)
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    silhouette = Image.new("RGBA", layer.size, (0, 0, 0, 150))
    shadow.paste(silhouette, (x + 10, y + 26), layer.getchannel("A"))
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(26)))
    canvas.alpha_composite(layer, (x, y))


def backdrop(w, h):
    base = Image.new("RGB", (w, h), (8, 9, 13))
    glow = Image.new("RGB", (w, h), (8, 9, 13))
    d = ImageDraw.Draw(glow)
    d.ellipse((w * 0.35, -h * 0.3, w * 1.25, h * 1.1), fill=(14, 52, 120))
    d.ellipse((-w * 0.3, h * 0.45, w * 0.5, h * 1.4), fill=(22, 26, 52))
    return Image.blend(base, glow.filter(ImageFilter.GaussianBlur(max(w, h) // 7)), 0.95).convert("RGBA")


def chip(d, x, y, text, fnt, fill=(36, 46, 74, 255), color=WHITE, pad=(26, 14)):
    """Rounded label; returns its right edge."""
    width = d.textlength(text, font=fnt)
    height = fnt.size + 2 * pad[1]
    d.rounded_rectangle((x, y, x + width + 2 * pad[0], y + height), radius=height / 2, fill=fill)
    d.text((x + pad[0], y + pad[1] - 3), text, font=fnt, fill=color)
    return x + width + 2 * pad[0]


def landscape(light, dark):
    w, h = 1280, 720
    img = backdrop(w, h)
    place(img, phone(light, 600), 800, 110, angle=-7)
    place(img, phone(dark, 640), 965, 62, angle=6)

    d = ImageDraw.Draw(img, "RGBA")
    mark = logo(64)
    img.alpha_composite(mark, (70, 76))
    d.text((146, 82), "VaultSpec", font=font("bold", 38), fill=WHITE)
    d.text((146, 126), "Authenticator", font=font("medium", 22), fill=GRAY)

    d.text((66, 226), "BIGGEST", font=font("bold", 108), fill=WHITE)
    d.text((66, 336), "UPDATE YET", font=font("bold", 108), fill=AMBER)

    f = font("semibold", 30)
    x = chip(d, 70, 506, "New design", f)
    chip(d, x + 14, 506, "Encrypted cloud backup", f)
    chip(d, 70, 584, "MAJOR UPDATE", font("bold", 28), fill=ACCENT + (255,))
    img.convert("RGB").save(ROOT / "thumbnail.png")


def vertical(light, dark):
    w, h = 1080, 1920
    img = backdrop(w, h)
    d = ImageDraw.Draw(img, "RGBA")

    mark = logo(84)
    img.alpha_composite(mark, (int((w - 84) / 2), 110))
    for y, text, fnt, color in (
        (214, "VAULTSPEC AUTHENTICATOR", font("semibold", 32), GRAY),
        (280, "BIGGEST", font("bold", 150), WHITE),
        (430, "UPDATE YET", font("bold", 150), AMBER),
    ):
        d.text(((w - d.textlength(text, font=fnt)) / 2, y), text, font=fnt, fill=color)

    place(img, phone(light, 1010), 70, 730, angle=7)
    place(img, phone(dark, 1060), 462, 700, angle=-6)

    d = ImageDraw.Draw(img, "RGBA")
    f = font("semibold", 38)
    labels = ["New design", "Encrypted cloud backup"]
    widths = [d.textlength(t, font=f) + 60 for t in labels]
    x = (w - sum(widths) - 18) / 2
    for text, width in zip(labels, widths):
        chip(d, x, 1772, text, f, fill=(20, 22, 30, 235), pad=(30, 18))
        x += width + 18
    img.convert("RGB").save(ROOT / "thumbnail-vertical.png")


if __name__ == "__main__":
    light = frame_at("home_light", 1.5)
    dark = frame_at("notif", 0.6)
    landscape(light, dark)
    vertical(light, dark)
    print("promo/thumbnail.png  promo/thumbnail-vertical.png")
