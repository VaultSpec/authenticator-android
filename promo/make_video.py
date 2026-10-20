#!/usr/bin/env python3
"""Builds the VaultSpec update video from the screen recordings in promo/footage.

  python promo/make_video.py

Narration comes from ElevenLabs when an API key is available: ELEVENLABS_API_KEY in the
environment, or in promo/.env, or as the only line of promo/.elevenlabs_key. Generated audio is cached in
promo/audio, so re-runs only pay for scenes whose text changed. Without a key the video is
rendered silent with captions, timed from the word count.

Needs ffmpeg and Pillow.
"""
import hashlib
import json
import os
import subprocess
import sys
import urllib.request
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent
FOOTAGE, AUDIO, BUILD = ROOT / "footage", ROOT / "audio", ROOT / "build"
FONTS = ROOT.parent / "app/src/main/res/font"
OUTPUT = ROOT / "VaultSpec-update.mp4"

W, H, FPS = 1080, 1920, 30
# The recording plays inside a Galaxy S25-style body: flat screen, thin even bezel, centred
# punch-hole camera, volume and power keys on the right edge.
PHONE_W, PHONE_H, PHONE_X, PHONE_Y, PHONE_R = 612, 1374, 234, 300, 46   # the screen itself
BEZEL, RAIL = 9, 6               # black border around the screen, then the metal side rail
BODY = BEZEL + RAIL
BODY_COLOR, BODY_EDGE = (58, 66, 84), (126, 136, 158)   # navy rail and its lighter outer edge
LEAD, TAIL = 0.35, 0.45          # silence before / after each scene's narration
WORDS_PER_SECOND = 2.6           # only used to time a silent render
VOICE_ID = os.environ.get("ELEVENLABS_VOICE_ID", "nPczCjzI2devNBz1zQrb")  # "Brian"
MODEL_ID = "eleven_multilingual_v2"

ACCENT = (10, 132, 255)
WHITE, GRAY = (255, 255, 255), (174, 174, 180)

# Each scene: headline on screen, what the voice says, what the captions show (defaults to the
# spoken text), and which parts of which recordings play, as (clip, start, end) in seconds.
SCENES = [
    dict(
        kind="title",
        headline="VaultSpec Authenticator",
        say="VaultSpec Authenticator 2.0 is out now on Google Play. It's our biggest update yet. "
            "And it's coming soon to the App Store, for iPhone, iPad and Mac.",
    ),
    dict(
        headline="A complete redesign",
        say="First, a complete redesign. A cleaner layout, codes that are easier to read, "
            "and crisp icons for more than three thousand services.",
        footage=[("home_light", 0.5, 11.0)],
    ),
    dict(
        headline="Light, dark, pitch black",
        say="It feels at home in light mode, dark mode, and pitch black.",
        footage=[("themes2", 7.2, 10.0), ("themes2", 13.0, 15.5), ("themes2", 17.3, 22.3)],
    ),
    dict(
        headline="Encrypted cloud backups",
        say="Now the big one: encrypted cloud backups. Create a free VaultSpec account, "
            "enter the code we email you, and you're in.",
        footage=[("account", 8.5, 13.5), ("account", 17.0, 42.5)],
    ),
    dict(
        headline="Two layers of protection",
        say="Your backup is protected by two layers. Your account password signs you in. "
            "Your vault password encrypts everything on your phone before it's uploaded, "
            "and it never leaves your device. We can't read your codes.",
        footage=[("cloud", 1.0, 15.5)],
    ),
    dict(
        headline="Backs up on your schedule",
        say="Choose how often it backs up: daily, weekly, or monthly. "
            "You'll get a quiet notification each time it's done.",
        footage=[("cloud", 15.5, 24.5), ("notif", 3.6, 7.6)],
    ),
    dict(
        headline="Restore on any phone",
        say="Got a new phone? Sign in, enter your vault password, "
            "and every account is back in seconds.",
        footage=[("restore", 0.5, 3.5), ("restore", 23.0, 26.5), ("restore", 31.0, 38.5), ("notif", 0.2, 2.7)],
    ),
    dict(
        kind="outro",
        headline="Update now",
        say="Update VaultSpec from the Google Play Store to get it today. If you like VaultSpec "
            "and want to support my work, you can buy me a coffee at buy me a coffee dot com, "
            "slash dhruvesh sixteen. Thanks for using VaultSpec.",
        caption="Update VaultSpec from the Google Play Store to get it today. If you like VaultSpec "
                "and want to support my work, you can buy me a coffee at buymeacoffee.com/dhruvesh16. "
                "Thanks for using VaultSpec.",
        hold=1.5,
    ),
]


def run(*args):
    subprocess.run(["ffmpeg", "-v", "error", "-y", *map(str, args)], check=True)


def duration(path):
    out = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(path)],
        check=True, capture_output=True, text=True,
    )
    return float(out.stdout.strip())


def font(weight, size):
    return ImageFont.truetype(str(FONTS / f"inter_{weight}.ttf"), size)


# ── Narration ──

def api_key():
    """Key from the environment, promo/.env (ELEVENLABS_API_KEY=...), or promo/.elevenlabs_key."""
    key = os.environ.get("ELEVENLABS_API_KEY", "").strip()
    for name in (".env", ".elevenlabs_key"):
        file = ROOT / name
        if key or not file.exists():
            continue
        for line in file.read_text().splitlines():
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            if "=" in line:
                name_part, _, value = line.partition("=")
                if name_part.strip().removeprefix("export ").strip() == "ELEVENLABS_API_KEY":
                    key = value.strip().strip("\"'")
            elif not key:
                key = line  # a bare key on its own line
    return key or None


def narration(index, text, key):
    """Path to the scene's narration audio, generating it if needed. None without a key."""
    tag = hashlib.sha256(f"{VOICE_ID}|{MODEL_ID}|{text}".encode()).hexdigest()[:10]
    path = AUDIO / f"{index:02d}_{tag}.mp3"
    if path.exists():
        return path
    if not key:
        return None
    AUDIO.mkdir(exist_ok=True)
    request = urllib.request.Request(
        f"https://api.elevenlabs.io/v1/text-to-speech/{VOICE_ID}?output_format=mp3_44100_128",
        data=json.dumps({
            "text": text,
            "model_id": MODEL_ID,
            "voice_settings": {"stability": 0.45, "similarity_boost": 0.8, "style": 0.15},
        }).encode(),
        headers={"xi-api-key": key, "Content-Type": "application/json", "Accept": "audio/mpeg"},
    )
    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            path.write_bytes(response.read())
    except urllib.error.HTTPError as e:
        sys.exit(f"ElevenLabs returned {e.code} for scene {index}: {e.read().decode(errors='replace')[:300]}")
    print(f"  narration {index:02d}: {duration(path):.1f}s")
    return path


# ── Artwork ──

def background():
    """Near-black canvas with a soft blue glow behind the phone."""
    img = Image.new("RGB", (W, H), (9, 9, 12))
    glow = Image.new("RGB", (W, H), (9, 9, 12))
    d = ImageDraw.Draw(glow)
    d.ellipse((-200, 500, W + 200, 1700), fill=(16, 34, 66))
    d.ellipse((180, 80, 900, 620), fill=(20, 24, 44))
    return Image.blend(img, glow.filter(ImageFilter.GaussianBlur(220)), 0.9)


def logo(size, color=WHITE):
    """The VaultSpec mark: twelve rounded bars (same geometry as the in-app splash)."""
    bars = [(21, 34), (27, 42), (33, 52), (39, 62), (45, 72), (51, 78),
            (57, 78), (63, 72), (69, 62), (75, 52), (81, 42), (87, 34)]
    scale = 4  # supersample for smooth edges
    s = size * scale / 108
    img = Image.new("RGBA", (size * scale, size * scale), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for cx, h in bars:
        d.rounded_rectangle(
            ((cx - 2) * s, (54 - h / 2) * s, (cx + 2) * s, (54 + h / 2) * s), radius=2 * s, fill=color,
        )
    return img.resize((size, size), Image.LANCZOS)


def app_store_icon(size):
    """App Store-style icon: blue rounded square with a white "A" built from three strokes."""
    k = 4
    n = size * k
    gradient = Image.new("RGB", (1, n))
    top, bottom = (27, 197, 255), (30, 108, 241)
    for y in range(n):
        t = y / (n - 1)
        gradient.putpixel((0, y), tuple(round(a + (b - a) * t) for a, b in zip(top, bottom)))
    tile = gradient.resize((n, n))
    shape = Image.new("L", (n, n), 0)
    ImageDraw.Draw(shape).rounded_rectangle((0, 0, n - 1, n - 1), radius=int(n * 0.225), fill=255)
    icon = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    icon.paste(tile, (0, 0), shape)

    d = ImageDraw.Draw(icon)
    u = n / 100
    width = int(7.5 * u)

    def stroke(x1, y1, x2, y2):
        d.line((x1 * u, y1 * u, x2 * u, y2 * u), fill=WHITE, width=width)
        for x, y in ((x1, y1), (x2, y2)):  # round caps
            d.ellipse((x * u - width / 2, y * u - width / 2, x * u + width / 2, y * u + width / 2), fill=WHITE)

    stroke(36.5, 62, 56.5, 27)      # left leg, running up past the apex
    stroke(43.5, 27, 50, 38.5)      # short stroke meeting it at the apex
    stroke(56, 49, 70.5, 74)        # right leg
    stroke(22, 62, 52, 62)          # crossbar, left part
    stroke(66, 62, 78, 62)          # crossbar, right part
    stroke(28, 74, 30.5, 69.5)      # foot of the left leg
    return icon.resize((size, size), Image.LANCZOS)


def centered(draw, y, text, fnt, fill):
    width = draw.textlength(text, font=fnt)
    draw.text(((W - width) / 2, y), text, font=fnt, fill=fill)


def wrap(draw, text, fnt, max_width):
    lines, line = [], ""
    for word in text.split():
        trial = f"{line} {word}".strip()
        if draw.textlength(trial, font=fnt) <= max_width or not line:
            line = trial
        else:
            lines.append(line)
            line = word
    return lines + [line] if line else lines


def scene_card(scene, path):
    """Static layer of a scene: background, eyebrow and headline (plus full artwork for cards)."""
    img = background()
    d = ImageDraw.Draw(img)
    kind = scene.get("kind")
    if kind == "title":
        mark = logo(300)
        img.paste(mark, ((W - 300) // 2, 560), mark)
        centered(d, 920, "VaultSpec", font("bold", 104), WHITE)
        centered(d, 1040, "Authenticator", font("medium", 60), GRAY)
        pill = "VERSION 2.0  ·  OUT NOW"
        f = font("semibold", 32)
        pw = d.textlength(pill, font=f) + 72
        d.rounded_rectangle(((W - pw) / 2, 1190, (W + pw) / 2, 1266), radius=38, fill=ACCENT)
        centered(d, 1208, pill, f, WHITE)

        # "Coming soon to the App Store" row: icon on the left, two lines of text beside it
        icon_size, gap = 112, 28
        small, big = font("medium", 32), font("bold", 50)
        text_w = max(d.textlength("Coming soon to the", font=small), d.textlength("App Store", font=big))
        left = int((W - (icon_size + gap + text_w)) / 2)
        icon = app_store_icon(icon_size)
        img.paste(icon, (left, 1336), icon)
        d.text((left + icon_size + gap, 1340), "Coming soon to the", font=small, fill=GRAY)
        d.text((left + icon_size + gap, 1380), "App Store", font=big, fill=WHITE)
        centered(d, 1476, "iPhone  ·  iPad  ·  Mac", font("semibold", 30), GRAY)
    elif kind == "outro":
        mark = logo(200)
        img.paste(mark, ((W - 200) // 2, 360), mark)
        centered(d, 610, "Enjoying VaultSpec?", font("bold", 76), WHITE)
        centered(d, 716, "You can support my work", font("medium", 44), GRAY)
        d.rounded_rectangle((110, 880, W - 110, 1200), radius=44, fill=(28, 28, 32))
        centered(d, 930, "BUY ME A COFFEE", font("semibold", 30), ACCENT)
        centered(d, 1000, "buymeacoffee.com", font("bold", 62), WHITE)
        centered(d, 1082, "/dhruvesh16", font("bold", 62), (255, 184, 77))
        # The call to action: update from Google Play, as a solid button under the coffee card
        cta = "Update now on Google Play"
        f = font("bold", 44)
        cw = d.textlength(cta, font=f) + 96
        d.rounded_rectangle(((W - cw) / 2, 1280, (W + cw) / 2, 1384), radius=52, fill=ACCENT)
        centered(d, 1304, cta, f, WHITE)
        centered(d, 1430, "Version 2.0 is out now", font("medium", 34), GRAY)
    else:
        centered(d, 92, "VAULTSPEC  ·  WHAT'S NEW", font("semibold", 28), ACCENT)
        centered(d, 150, scene["headline"], font("bold", 68), WHITE)
        # Soft shadow under the phone
        shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        ImageDraw.Draw(shadow).rounded_rectangle(
            (PHONE_X - BODY - 6, PHONE_Y - BODY + 18, PHONE_X + PHONE_W + BODY + 6, PHONE_Y + PHONE_H + BODY + 30),
            radius=PHONE_R + BODY, fill=(0, 0, 0, 170),
        )
        img.paste(shadow.filter(ImageFilter.GaussianBlur(28)), (0, 0), shadow.filter(ImageFilter.GaussianBlur(28)))
    img.save(path)


def phone_mask(path):
    mask = Image.new("L", (PHONE_W * 3, PHONE_H * 3), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, PHONE_W * 3 - 1, PHONE_H * 3 - 1), radius=PHONE_R * 3, fill=255)
    mask.resize((PHONE_W, PHONE_H), Image.LANCZOS).save(path)


def phone_frame(path):
    """Galaxy S25-style body drawn around (and over the edge of) the footage."""
    k = 3  # supersample
    img = Image.new("RGBA", (W * k, H * k), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def box(inset):  # the screen rectangle grown outwards by [inset]
        return ((PHONE_X - inset) * k, (PHONE_Y - inset) * k,
                (PHONE_X + PHONE_W + inset) * k, (PHONE_Y + PHONE_H + inset) * k)

    # Side keys on the right edge: volume rocker above the power key
    right = PHONE_X + PHONE_W + BODY
    for top, height in ((PHONE_Y + 300, 150), (PHONE_Y + 500, 86)):
        d.rounded_rectangle(((right - 4) * k, top * k, (right + 4) * k, (top + height) * k),
                            radius=4 * k, fill=BODY_EDGE)

    # Metal rail with a lighter outer edge, then the black bezel
    d.rounded_rectangle(box(BODY), radius=(PHONE_R + BODY) * k, fill=BODY_EDGE)
    d.rounded_rectangle(box(BODY - 1.5), radius=(PHONE_R + BODY - 1.5) * k, fill=BODY_COLOR)
    d.rounded_rectangle(box(BEZEL), radius=(PHONE_R + BEZEL) * k, fill=(4, 4, 6))

    # Cut the screen opening so the footage shows through
    opening = Image.new("L", img.size, 255)
    ImageDraw.Draw(opening).rounded_rectangle(box(0), radius=PHONE_R * k, fill=0)
    alpha = img.getchannel("A")
    img.putalpha(Image.composite(alpha, Image.new("L", img.size, 0), opening))

    # Punch-hole front camera, centred near the top of the screen
    d = ImageDraw.Draw(img)
    cx, cy, r = (PHONE_X + PHONE_W / 2) * k, (PHONE_Y + 30) * k, 10 * k
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(0, 0, 0, 255))
    d.ellipse((cx - r * 0.45, cy - r * 0.45, cx + r * 0.45, cy + r * 0.45), fill=(18, 20, 30, 255))

    img.resize((W, H), Image.LANCZOS).save(path)


def caption_chunks(text, speech_seconds):
    """Splits caption text into short phrases timed in proportion to their length."""
    words, chunks, current = text.split(), [], []
    for word in words:
        current.append(word)
        ends_phrase = word[-1] in ".,?!:;"
        if len(current) >= 8 or (ends_phrase and len(current) >= 4):
            chunks.append(" ".join(current))
            current = []
    if current:
        if chunks and len(current) < 3:
            chunks[-1] += " " + " ".join(current)
        else:
            chunks.append(" ".join(current))
    total = sum(len(c) for c in chunks)
    timed, t = [], LEAD
    for chunk in chunks:
        length = speech_seconds * len(chunk) / total
        timed.append((chunk, t, t + length))
        t += length
    return timed


def caption_image(text, path, card):
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    f = font("semibold", 40)
    lines = wrap(d, text, f, W - 140)
    y = (1600 if card else 1742) + (2 - min(len(lines), 2)) * 26
    for line in lines:
        centered(d, y, line, f, WHITE)
        y += 54
    img.save(path)


# ── Assembly ──

def footage_clip(index, segments, seconds):
    """Joins the scene's recording segments and retimes them to exactly [seconds]."""
    out = BUILD / f"fg_{index:02d}.mp4"
    total = sum(end - start for _, start, end in segments)
    speed = max(total / seconds, 0.85)  # never slow real footage much; hold the last frame instead
    inputs, parts = [], []
    for n, (clip, start, end) in enumerate(segments):
        inputs += ["-ss", f"{start}", "-t", f"{end - start}", "-i", FOOTAGE / f"{clip}.mp4"]
        parts.append(f"[{n}:v]scale={PHONE_W}:{PHONE_H}:flags=lanczos,setsar=1,fps={FPS}[v{n}]")
    joined = "".join(f"[v{n}]" for n in range(len(segments)))
    graph = ";".join(parts) + (
        f";{joined}concat=n={len(segments)}:v=1:a=0,setpts=PTS/{speed:.4f},fps={FPS},"
        f"tpad=stop_mode=clone:stop_duration={seconds},trim=duration={seconds:.3f}[out]"
    )
    run(*inputs, "-filter_complex", graph, "-map", "[out]", "-an",
        "-c:v", "libx264", "-preset", "medium", "-crf", "16", "-pix_fmt", "yuv420p", out)
    return out


def build_scene(index, scene, audio, mask, frame):
    card = scene.get("kind") in ("title", "outro")
    speech = duration(audio) if audio else len(scene["say"].split()) / WORDS_PER_SECOND
    seconds = round(LEAD + speech + TAIL + scene.get("hold", 0), 2)

    base = BUILD / f"card_{index:02d}.png"
    scene_card(scene, base)

    inputs = ["-loop", "1", "-t", f"{seconds}", "-framerate", f"{FPS}", "-i", base]
    graph, last, n = [], "0:v", 1
    if not card:
        fg = footage_clip(index, scene["footage"], seconds)
        inputs += ["-i", fg, "-loop", "1", "-t", f"{seconds}", "-i", mask, "-loop", "1", "-t", f"{seconds}", "-i", frame]
        graph += [
            f"[{n}:v]format=rgba[fg];[{n + 1}:v]format=gray[mk];[fg][mk]alphamerge[phone]",
            f"[{last}][phone]overlay={PHONE_X}:{PHONE_Y}[withphone]",
            f"[withphone][{n + 2}:v]overlay=0:0[framed]",
        ]
        last, n = "framed", n + 3

    for c, (text, start, end) in enumerate(caption_chunks(scene.get("caption", scene["say"]), speech)):
        png = BUILD / f"cap_{index:02d}_{c:02d}.png"
        caption_image(text, png, card)
        inputs += ["-loop", "1", "-t", f"{seconds}", "-i", png]
        graph.append(f"[{last}][{n}:v]overlay=0:0:enable='between(t,{start:.2f},{end:.2f})'[c{c}]")
        last, n = f"c{c}", n + 1

    graph.append(
        f"[{last}]fade=t=in:st=0:d=0.3,fade=t=out:st={seconds - 0.3:.2f}:d=0.3,format=yuv420p[v]"
    )
    if audio:
        inputs += ["-i", audio]
        graph.append(f"[{n}:a]adelay={int(LEAD * 1000)}:all=1,apad,atrim=duration={seconds},aresample=44100[a]")
    else:
        inputs += ["-f", "lavfi", "-t", f"{seconds}", "-i", "anullsrc=channel_layout=stereo:sample_rate=44100"]
        graph.append(f"[{n}:a]atrim=duration={seconds}[a]")

    out = BUILD / f"scene_{index:02d}.mp4"
    run(*inputs, "-filter_complex", ";".join(graph), "-map", "[v]", "-map", "[a]",
        "-r", f"{FPS}", "-c:v", "libx264", "-preset", "medium", "-crf", "18", "-pix_fmt", "yuv420p",
        "-c:a", "aac", "-b:a", "192k", "-ac", "2", "-t", f"{seconds}", out)
    print(f"  scene {index:02d} {seconds:5.1f}s  {scene['headline']}")
    return out, seconds


def main():
    BUILD.mkdir(exist_ok=True)
    key = api_key()
    audio = [narration(i, s["say"], key) for i, s in enumerate(SCENES)]
    voiced = all(audio)
    if not voiced:
        audio = [None] * len(SCENES)
        print("No ElevenLabs key and no cached narration: rendering a silent, captioned video.")

    mask, frame = BUILD / "mask.png", BUILD / "frame.png"
    phone_mask(mask)
    phone_frame(frame)

    scenes = [build_scene(i, s, audio[i], mask, frame) for i, s in enumerate(SCENES)]
    listing = BUILD / "scenes.txt"
    listing.write_text("".join(f"file '{path}'\n" for path, _ in scenes))
    # Join the scenes; the picture is copied as-is and the voice is levelled to a streaming-friendly loudness
    audio_args = ["-af", "loudnorm=I=-16:TP=-1.5:LRA=11", "-ar", "44100", "-c:a", "aac", "-b:a", "192k"] if voiced else ["-c:a", "copy"]
    run("-f", "concat", "-safe", "0", "-i", listing, "-c:v", "copy", *audio_args, "-movflags", "+faststart", OUTPUT)
    total = sum(seconds for _, seconds in scenes)
    print(f"{OUTPUT.relative_to(ROOT.parent)}  {total:.0f}s  {'with narration' if voiced else 'silent'}")


if __name__ == "__main__":
    main()
