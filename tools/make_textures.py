"""Draws the placeholder Necromancer textures: 128x128 skin + emissive glow map per variant, and spawn eggs.
UV offsets must match NecromancerModel.createBodyLayer(). Run: python3 tools/make_textures.py"""
import os, random, struct, zlib

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "necromancer", "textures")
VARIANTS = {  # robe, robe shadow, glow
    "wind":   ((88, 104, 132), (58, 68, 90),  (150, 225, 255)),
    "fire":   ((158, 56, 24),  (104, 34, 14), (255, 160, 40)),
    "wither": ((40, 32, 52),   (24, 19, 32),  (190, 160, 255)),
    "poison": ((44, 92, 40),   (28, 58, 26),  (110, 255, 60)),
    "lightning": ((176, 138, 32), (112, 86, 18), (255, 248, 140)),
}
SILVER, SILVER_DARK = (198, 202, 208), (130, 134, 142)
BONE, BONE_DARK = (156, 156, 150), (96, 96, 92)
SOCKET, STAFF, STAFF_GRAIN = (22, 20, 26), (34, 30, 40), (58, 50, 62)
LEATHER = (92, 62, 40)
CLEAR = (0, 0, 0, 0)


def write_png(path, w, h, px):
    raw = b"".join(b"\x00" + bytes(c for x in range(w) for c in px[y][x]) for y in range(h))
    chunk = lambda t, d: struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def faces(u, v, w, h, d):
    """Minecraft box-UV face rectangles (x, y, width, height)."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h)}


class Canvas:
    def __init__(self, seed):
        self.rng = random.Random(seed)
        self.px = [[CLEAR] * 128 for _ in range(128)]
        self.glow = [[CLEAR] * 128 for _ in range(128)]

    def col(self, c, k=1.0):
        n = self.rng.randint(-8, 8)
        return tuple(max(0, min(255, int(v * k) + n)) for v in c) + (255,)

    def fill(self, rect, fn):
        x0, y0, w, h = rect
        for y in range(h):
            for x in range(w):
                c = fn(x, y, w, h)
                if c is not None:
                    self.px[y0 + y][x0 + x] = c

    def lit(self, x, y, c):
        """Glowing pixel: drawn on the skin and on the emissive map."""
        self.px[y][x] = c + (255,)
        self.glow[y][x] = c + (255,)

    def box(self, u, v, w, h, d, fn):
        for name, r in faces(u, v, w, h, d).items():
            self.fill(r, lambda x, y, fw, fh, n=name: fn(n, x, y, fw, fh))


def skin(name, robe, shadow, glow):
    c = Canvas(name)
    grad = lambda base, y, h, k=0.3: c.col(base, 1.0 - k * y / max(1, h - 1))
    ragged = {x: c.rng.randint(0, 3) for x in range(64)}  # tattered hem depth per column

    # head: bone skull, dark eye sockets with glowing pupils, nose hole and a row of teeth
    c.box(0, 0, 8, 8, 8, lambda f, x, y, w, h: c.col(BONE, 1.0 if f == "front" else 0.85))
    fx, fy, _, _ = faces(0, 0, 8, 8, 8)["front"]
    for sx in (1, 5):
        for dx in (0, 1):
            for dy in (2, 3, 4):
                c.px[fy + dy][fx + sx + dx] = c.col(SOCKET)
    c.lit(fx + 2, fy + 3, glow); c.lit(fx + 5, fy + 3, glow)
    c.px[fy + 5][fx + 3] = c.col(SOCKET); c.px[fy + 5][fx + 4] = c.col(SOCKET)
    # jaw: the nose runs into a dark mouth slit (row 6); four square teeth on the chin (row 7), shaded corners
    for tx in range(1, 7):
        c.px[fy + 6][fx + tx] = c.col(SOCKET)
        c.px[fy + 7][fx + tx] = c.col(BONE_DARK) if tx in (1, 6) else c.col(BONE, 0.92 if tx % 2 else 1.0)
    # hood: robe shell, open face with a silver rim
    c.box(32, 0, 8, 8, 8, lambda f, x, y, w, h: (c.col(SILVER_DARK) if y < 1 or x in (0, w - 1) else CLEAR) if f == "front"
          else grad(shadow, y, h, 0.2))
    # crown band: silver with dark seams, glowing gem front-centre
    c.box(64, 0, 10, 2, 10, lambda f, x, y, w, h: c.col(SILVER, 1.0 if y == 0 else 0.8) if f != "bottom" else c.col(SILVER_DARK))
    gx, gy, _, _ = faces(64, 0, 10, 2, 10)["front"]
    c.lit(gx + 4, gy, glow); c.lit(gx + 5, gy, glow); c.lit(gx + 4, gy + 1, glow); c.lit(gx + 5, gy + 1, glow)
    c.box(104, 0, 1, 3, 1, lambda f, x, y, w, h: c.col(SILVER, 1.1 - 0.15 * y))
    c.box(110, 0, 2, 4, 1, lambda f, x, y, w, h: c.col(SILVER, 1.0 - 0.1 * y))
    sx, sy, _, _ = faces(110, 0, 2, 4, 1)["front"]
    c.lit(sx, sy + 1, glow); c.lit(sx + 1, sy + 1, glow)

    # ribcage: bone ribs with see-through gaps between them, solid sternum down the front and back
    def rib(f, x, y, w, h):
        if f in ("top", "bottom"):
            return c.col(BONE, 0.8) if f == "top" else CLEAR
        spine_col = f in ("front", "back") and x in (w // 2 - 1, w // 2)
        if y % 2 == 1 and not spine_col:
            return CLEAR
        return c.col(BONE, 0.95 if spine_col else 1.0 - 0.04 * y)
    c.box(0, 16, 8, 8, 5, rib)
    # spine: stacked vertebrae
    c.box(28, 16, 2, 12, 2, lambda f, x, y, w, h: c.col(BONE_DARK if y % 3 == 2 else BONE))
    # belt: silver with a glowing buckle gem
    c.box(44, 16, 9, 3, 6, lambda f, x, y, w, h: c.col(SILVER, 0.85 if y == 1 else 1.0))
    bx, by, _, _ = faces(44, 16, 9, 3, 6)["front"]
    for gx2 in (3, 4, 5):
        c.lit(bx + gx2, by + 1, glow)
    c.lit(bx + 4, by, glow); c.lit(bx + 4, by + 2, glow)

    # robe skirt: robe colour, glowing drips running down the front, tattered hem all round
    def robe_px(f, x, y, w, h):
        if f in ("top", "bottom"):
            return c.col(shadow)
        if y >= h - ragged[x + (7 if f == "back" else 0)]:
            return CLEAR
        return grad(robe, y, h, 0.4)
    c.box(0, 34, 9, 13, 6, robe_px)
    rx, ry, rw, rh = faces(0, 34, 9, 13, 6)["front"]
    for dx, top in ((1, 6), (3, 3), (5, 8), (7, 4)):
        for y in range(top, rh - ragged[dx]):
            c.lit(rx + dx, ry + y, glow if y > top + 1 else tuple(v // 2 for v in glow))
    for f in ("left", "right", "back"):
        sx, sy, sw, sh = faces(0, 34, 9, 13, 6)[f]
        for y in range(sh - 4, sh - ragged[2]):
            c.lit(sx + 2, sy + y, glow)

    # cape: darker, tattered, faint glowing drips on the outside
    def cape_px(f, x, y, w, h):
        if f in ("top", "bottom"):
            return c.col(shadow)
        if y >= h - ragged[(x * 3) % 64]:
            return CLEAR
        return grad(shadow, y, h, 0.25)
    c.box(32, 34, 10, 22, 1, cape_px)
    cx, cy, cw, ch = faces(32, 34, 10, 22, 1)["back"]
    for dx, top in ((2, 14), (5, 10), (8, 15)):
        for y in range(top, ch - ragged[(dx * 3) % 64]):
            c.lit(cx + dx, cy + y, glow)

    # bone arms with knobbly joints, short robe sleeves with a silver cuff
    c.box(56, 34, 2, 12, 2, lambda f, x, y, w, h: c.col(BONE_DARK if y in (5, 6) else BONE, 1.0 - 0.02 * y))
    c.box(98, 34, 4, 5, 4, lambda f, x, y, w, h: c.col(shadow) if f in ("top", "bottom")
          else c.col(SILVER, 0.9) if y == h - 1 else grad(robe, y, h, 0.2))
    # pauldrons: robe plates with a silver rim
    c.box(72, 34, 6, 5, 7, lambda f, x, y, w, h: c.col(SILVER) if (f == "top" and (x in (0, w - 1) or y in (0, h - 1)))
          or (f not in ("top", "bottom") and y == h - 1) else c.col(shadow, 1.1))
    # bone legs
    c.box(56, 50, 2, 12, 2, lambda f, x, y, w, h: c.col(BONE_DARK if y in (5, 6) else BONE, 1.0 - 0.02 * y))

    # staff shaft (2x32x2, grip 24 px down): ebony with a pale grain line, silver collar at the top,
    # leather-wrapped grip around the hand, silver ferrule at the foot
    def shaft(f, x, y, w, h):
        if f in ("top", "bottom"):
            return c.col(SILVER_DARK)
        if y <= 1 or y >= h - 2:
            return c.col(SILVER, 1.0 if y in (0, h - 1) else 0.8)
        if 20 <= y <= 27:
            return c.col(LEATHER, 1.0 if (y + x) % 2 else 0.75)
        return c.col(STAFF_GRAIN if x == (y // 5) % 2 else STAFF, 1.0 - 0.15 * (y % 5 == 0))
    c.box(0, 64, 2, 32, 2, shaft)
    # silver collar where the orb sits on the shaft
    c.box(48, 64, 3, 1, 3, lambda f, x, y, w, h: c.col(SILVER, 1.05 if f == "top" else 0.9))
    for f, (x0, y0, w, h) in faces(8, 64, 5, 5, 2).items():
        for y in range(h):
            for x in range(w):
                ring = x in (0, w - 1) or y in (0, h - 1)
                c.lit(x0 + x, y0 + y, glow if ring else (255, 255, 255) if f in ("front", "back") and 1 <= x <= w - 2 and 1 <= y <= h - 2 and (x, y) != (w // 2, h // 2) else glow)
    # halo: a hollow glowing ring around the orb (clipped corners read as round), see-through in the middle
    for f, (x0, y0, w, h) in faces(24, 64, 7, 7, 1).items():
        for y in range(h):
            for x in range(w):
                if f in ("front", "back"):
                    corner = (x in (0, w - 1)) and (y in (0, h - 1))
                    rim = x in (0, w - 1) or y in (0, h - 1)
                    if rim and not corner:
                        c.lit(x0 + x, y0 + y, tuple(int(v * 0.7) for v in glow))
                else:
                    c.lit(x0 + x, y0 + y, tuple(int(v * 0.55) for v in glow))

    base = os.path.join(ROOT, "entity", "necromancer", name)
    write_png(base + ".png", 128, 128, c.px)
    write_png(base + "_glow.png", 128, 128, c.glow)


def egg(name, robe, shadow, glow):
    rng = random.Random(name + "egg")
    px = [[CLEAR] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            dx, dy = (x - 7.5) / 5.5, (y - 8.5) / (7.0 if y > 8 else 7.8)
            if dx * dx + dy * dy <= 1.0:
                edge = dx * dx + dy * dy > 0.72
                c = shadow if edge else robe
                px[y][x] = tuple(max(0, min(255, v + rng.randint(-8, 8))) for v in c) + (255,)
    for x in (5, 7, 9, 11):  # crown
        px[3][x] = SILVER + (255,)
    for x in range(5, 11):
        px[4][x] = SILVER + (255,)
    px[7][6] = px[7][9] = glow + (255,)
    for x, y in ((6, 11), (9, 12), (8, 13)):
        px[y][x] = glow + (255,)
    write_png(os.path.join(ROOT, "item", name + "_necromancer_spawn_egg.png"), 16, 16, px)


for n, (r, s, g) in VARIANTS.items():
    skin(n, r, s, g)
    egg(n, r, s, g)
print("ok")
