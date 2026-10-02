import io
import os
import time
import math
from PIL import Image, ImageDraw, ImageFont, ImageOps
import qrcode
import numpy as np

class StripGenerator:
    """
    Tạo dải ảnh Photostrip, kết hợp Khung hình Hot Trend 2026, Sticker, 
    nhãn thời gian, xuất file PNG độ phân giải cao, GIF chuyển động và QR Code.
    """

    FRAME_THEMES = {
        "korean_pink": {
            "bg": (255, 228, 236),        # Soft Pink
            "card_bg": (255, 255, 255),
            "text": (220, 80, 130),
            "border": (255, 205, 220)
        },
        "y2k_gradient": {
            "bg": (35, 25, 60),            # Dark Y2K Purple
            "card_bg": (255, 255, 255),
            "text": (255, 110, 190),
            "border": (180, 100, 255)
        },
        "minimal_black": {
            "bg": (18, 18, 20),           # Midnight Dark
            "card_bg": (30, 30, 35),
            "text": (240, 240, 245),
            "border": (50, 50, 60)
        },
        "cream_vintage": {
            "bg": (250, 246, 235),        # Warm Cream / Ivory
            "card_bg": (255, 255, 255),
            "text": (90, 75, 65),
            "border": (230, 220, 200)
        },
        "idol_lilac": {
            "bg": (230, 220, 255),        # Pastel Lilac
            "card_bg": (255, 255, 255),
            "text": (130, 80, 220),
            "border": (205, 185, 245)
        },
        "cute_yellow": {
            "bg": (255, 248, 200),        # Pastel Yellow
            "card_bg": (255, 255, 255),
            "text": (230, 140, 30),
            "border": (255, 230, 150)
        }
    }

    STICKER_COLORS = {
        "✦": ((255, 215, 0), (255, 250, 220)),   # Gold Sparkle
        "♥": ((255, 75, 114), (255, 235, 240)),  # Heart Pink/Red
        "☺": ((255, 180, 0), (255, 248, 220)),   # Smile Yellow
        "☁": ((78, 168, 222), (235, 245, 255)),  # Cloud Blue
        "👑": ((255, 183, 3), (255, 245, 220)),   # Crown Gold
        "🎀": ((255, 112, 166), (255, 235, 245)), # Ribbon Pink
        "✨": ((255, 151, 112), (255, 240, 235)), # Stars Coral
        "🌸": ((255, 158, 187), (255, 240, 245)), # Blossom Pink
        "⚡": ((255, 209, 102), (255, 250, 230)), # Bolt Yellow
        "🐾": ((160, 110, 80), (245, 235, 230)),  # Paw Brown
        "🍒": ((230, 57, 70), (255, 235, 238)),   # Cherry Red
        "🌙": ((181, 23, 158), (250, 235, 255)),  # Moon Purple
    }

    @classmethod
    def get_sticker_font(cls, size):
        font_paths = [
            "C:\\Windows\\Fonts\\seguiemj.ttf",
            "C:\\Windows\\Fonts\\arial.ttf",
            "C:\\Windows\\Fonts\\segoeui.ttf",
        ]
        for p in font_paths:
            if os.path.exists(p):
                try:
                    return ImageFont.truetype(p, size)
                except Exception:
                    pass
        return ImageFont.load_default()

    @classmethod
    def create_sticker_badge(cls, symbol, size=44):
        """Tạo ảnh PIL RGBA của sticker hình tròn bo tròn sắc nét"""
        badge = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(badge)
        
        fg_color, bg_color = cls.STICKER_COLORS.get(symbol, ((220, 80, 130), (255, 240, 245)))
        
        # Viền viền trắng nổi bật
        draw.ellipse([1, 1, size - 1, size - 1], fill=(255, 255, 255, 255), outline=(220, 220, 230, 220), width=2)
        # Nền tròn sticker
        margin = 3
        draw.ellipse([margin, margin, size - margin, size - margin], fill=(*bg_color, 255))
        
        font = cls.get_sticker_font(int(size * 0.52))
        draw.text((size // 2, size // 2), symbol, fill=(*fg_color, 255), font=font, anchor="mm")
        return badge

    @staticmethod
    def create_rounded_image(pil_img, radius=16):
        """Tạo góc bo tròn cho từng tấm ảnh trong strip"""
        mask = Image.new('L', pil_img.size, 0)
        draw = ImageDraw.Draw(mask)
        draw.rounded_rectangle([(0, 0), pil_img.size], radius=radius, fill=255)
        output = Image.new('RGBA', pil_img.size, (0, 0, 0, 0))
        output.paste(pil_img, (0, 0), mask=mask)
        return output

    @classmethod
    def compose_photostrip(cls, pil_images, layout="1x4", theme="korean_pink", custom_text="SNAPVIBE 2026", date_text=None, stickers=None):
        """
        Ghép danh sách các ảnh PIL thành Dải ảnh Photostrip chất lượng cao (300 DPI ready)
        """
        if not date_text:
            date_text = time.strftime("%Y.%m.%d")

        theme_cfg = cls.FRAME_THEMES.get(theme, cls.FRAME_THEMES["korean_pink"])
        bg_color = theme_cfg["bg"]
        text_color = theme_cfg["text"]

        # Kích thước tiêu chuẩn Photostrip (tỷ lệ 1x4 dọc chuẩn Life4Cuts Hàn Quốc)
        target_photo_w = 400
        target_photo_h = 280
        padding = 24
        spacing = 16
        header_h = 60
        footer_h = 90

        num_photos = len(pil_images)
        if num_photos == 0:
            return None

        # Resize tất cả các ảnh về chuẩn
        resized_imgs = []
        for img in pil_images:
            img_rgba = img.convert("RGBA")
            # Crop to fill ratio 4:3
            fitted_img = ImageOps.fit(img_rgba, (target_photo_w, target_photo_h), Image.Resampling.LANCZOS)
            rounded_img = cls.create_rounded_image(fitted_img, radius=12)
            resized_imgs.append(rounded_img)

        # Tính kích thước canvas dựa theo bố cục (Layout)
        if layout == "1x4":
            canvas_w = target_photo_w + (padding * 2)
            canvas_h = header_h + (target_photo_h * num_photos) + (spacing * (num_photos - 1)) + footer_h + padding
            canvas = Image.new("RGBA", (canvas_w, canvas_h), bg_color)
            
            # Paste các ảnh theo chiều dọc
            y_offset = header_h
            for img in resized_imgs:
                canvas.paste(img, (padding, y_offset), img)
                y_offset += target_photo_h + spacing

        elif layout == "2x2":
            canvas_w = (target_photo_w * 2) + spacing + (padding * 2)
            canvas_h = header_h + (target_photo_h * 2) + spacing + footer_h + padding
            canvas = Image.new("RGBA", (canvas_w, canvas_h), bg_color)

            positions = [
                (padding, header_h),
                (padding + target_photo_w + spacing, header_h),
                (padding, header_h + target_photo_h + spacing),
                (padding + target_photo_w + spacing, header_h + target_photo_h + spacing)
            ]
            for idx, img in enumerate(resized_imgs[:4]):
                canvas.paste(img, positions[idx], img)

        elif layout == "double_strip":
            # 2 Dải 1x4 nằm song song (chuẩn in 4x6 photobooth)
            single_w = target_photo_w + (padding * 2)
            canvas_w = (single_w * 2) + 20
            canvas_h = header_h + (target_photo_h * min(num_photos, 4)) + (spacing * (min(num_photos, 4) - 1)) + footer_h + padding
            canvas = Image.new("RGBA", (canvas_w, canvas_h), bg_color)

            # Strip trái
            y_offset = header_h
            for img in resized_imgs[:4]:
                canvas.paste(img, (padding, y_offset), img)
                y_offset += target_photo_h + spacing

            # Strip phải
            y_offset = header_h
            right_x = single_w + 20 + padding
            for img in resized_imgs[:4]:
                canvas.paste(img, (right_x, y_offset), img)
                y_offset += target_photo_h + spacing

        else: # Default 1x4
            canvas_w = target_photo_w + (padding * 2)
            canvas_h = header_h + (target_photo_h * num_photos) + (spacing * (num_photos - 1)) + footer_h + padding
            canvas = Image.new("RGBA", (canvas_w, canvas_h), bg_color)
            y_offset = header_h
            for img in resized_imgs:
                canvas.paste(img, (padding, y_offset), img)
                y_offset += target_photo_h + spacing

        # Vẽ Header & Footer bằng PIL ImageDraw
        draw = ImageDraw.Draw(canvas)
        
        # Tiêu đề Header
        try:
            # Thử load font hệ thống nếu có
            font_title = ImageFont.truetype("arial.ttf", 22)
            font_subtitle = ImageFont.truetype("arial.ttf", 14)
        except Exception:
            font_title = ImageFont.load_default()
            font_subtitle = ImageFont.load_default()

        # Center Text Header
        draw.text((canvas_w // 2, 28), "★ " + custom_text.upper() + " ★", fill=text_color, font=font_title, anchor="mm")
        
        # Center Text Footer & Date
        footer_y = canvas_h - (footer_h // 2) - 5
        draw.text((canvas_w // 2, footer_y - 12), date_text + " • PHOTOBOOTH ONLINE", fill=text_color, font=font_subtitle, anchor="mm")
        draw.text((canvas_w // 2, footer_y + 10), "LIFE 4 CUTS • MEMORIES FOREVER", fill=text_color, font=font_subtitle, anchor="mm")

        # Vẽ Sticker nếu có
        if stickers:
            # Predefined decorative positions based on strip layout
            if layout == "1x4":
                slot_positions = [
                    (10, 10),                                           # Header left
                    (canvas_w - 54, 10),                                # Header right
                    (canvas_w - 44, header_h + target_photo_h - 22),   # Frame 1 bottom-right
                    (6, header_h + target_photo_h + spacing + 10),      # Frame 2 top-left
                    (canvas_w - 44, header_h + (target_photo_h + spacing)*2 + 15), # Frame 3 right
                    (6, header_h + (target_photo_h + spacing)*3 - 10),  # Frame 4 left
                    (14, canvas_h - footer_h + 8),                      # Footer left
                    (canvas_w - 56, canvas_h - footer_h + 8),           # Footer right
                ]
            elif layout == "2x2":
                slot_positions = [
                    (14, 10),                                           # Header left
                    (canvas_w - 54, 10),                                # Header right
                    (padding + target_photo_w - 20, header_h - 15),     # Photo 1 top-right
                    (padding - 10, header_h + target_photo_h + spacing - 15), # Photo 3 left
                    (canvas_w - padding - 30, header_h + target_photo_h + spacing + 10),
                    (16, canvas_h - footer_h + 10),                     # Footer left
                    (canvas_w - 56, canvas_h - footer_h + 10),          # Footer right
                ]
            else: # double_strip or default
                slot_positions = [
                    (14, 10),
                    (canvas_w - 54, 10),
                    (canvas_w // 2 - 22, 10),
                    (14, canvas_h - footer_h + 10),
                    (canvas_w - 54, canvas_h - footer_h + 10),
                ]

            for idx, stk in enumerate(stickers):
                if isinstance(stk, dict):
                    symbol = stk.get("symbol") or stk.get("text") or "✦"
                    x = stk.get("x")
                    y = stk.get("y")
                    stk_img = stk.get("image")
                else:
                    symbol = str(stk)
                    x = None
                    y = None
                    stk_img = None

                if not stk_img:
                    stk_img = cls.create_sticker_badge(symbol, size=44)

                if x is None or y is None:
                    pos_idx = idx % len(slot_positions)
                    x, y = slot_positions[pos_idx]

                canvas.paste(stk_img, (int(x), int(y)), stk_img)

        return canvas

    @staticmethod
    def create_gif(pil_images, duration_ms=500):
        """Tạo ảnh GIF động nhảy qua các khung hình đã chụp"""
        if not pil_images:
            return None
        
        resized = []
        for img in pil_images:
            fit_img = ImageOps.fit(img.convert("RGB"), (400, 300), Image.Resampling.LANCZOS)
            resized.append(fit_img)

        gif_bytes = io.BytesIO()
        resized[0].save(
            gif_bytes,
            format='GIF',
            save_all=True,
            append_images=resized[1:],
            duration=duration_ms,
            loop=0
        )
        gif_bytes.seek(0)
        return gif_bytes.getvalue()

    @staticmethod
    def generate_qr_code(url):
        """Tạo mã QR Code chứa đường dẫn download ảnh"""
        qr = qrcode.QRCode(
            version=1,
            error_correction=qrcode.constants.ERROR_CORRECT_M,
            box_size=8,
            border=2,
        )
        qr.add_data(url)
        qr.make(fit=True)
        img = qr.make_image(fill_color="black", back_color="white")
        return img
