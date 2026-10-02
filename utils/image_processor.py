import cv2
import numpy as np
from PIL import Image

class ImageProcessor:
    """
    Xử lý ảnh bằng OpenCV và NumPy
    Cung cấp các thuật toán bộ lọc màu (Filters), chỉnh độ sáng, tương phản, 
    làm mượt da (Beauty Filter) và hiệu ứng đồ họa.
    """

    @staticmethod
    def bgr_to_rgb(image_np):
        return cv2.cvtColor(image_np, cv2.COLOR_BGR2RGB)

    @staticmethod
    def rgb_to_bgr(image_np):
        return cv2.cvtColor(image_np, cv2.COLOR_RGB2BGR)

    @staticmethod
    def apply_beauty_filter(img, intensity=0.6):
        """
        Thuật toán làm mượt da (Beauty Filter) sử dụng Bilateral Filter của OpenCV.
        Giữ nguyên chi tiết sắc nét ở mắt và tóc, làm mịn cấu trúc da.
        """
        if intensity <= 0:
            return img

        # Bilateral filter lọc nhiễu giữ nguyên cạnh/chi tiết
        d = int(7 + intensity * 6)
        sigma_color = int(40 + intensity * 50)
        sigma_space = int(40 + intensity * 50)
        
        smoothed = cv2.bilateralFilter(img, d, sigma_color, sigma_space)
        
        # Trộn ảnh đã làm mịn với ảnh gốc theo độ đậm (intensity)
        output = cv2.addWeighted(smoothed, intensity, img, 1 - intensity, 0)
        return output

    @staticmethod
    def adjust_brightness_contrast_saturation(img, brightness=0, contrast=0, saturation=0):
        """
        Điều chỉnh độ sáng (-100 đến 100), độ tương phản (-100 đến 100) và độ bão hòa màu (-100 đến 100).
        """
        # Convert to float for precise calculations
        result = img.astype(np.float32)

        # Brightness adjustment
        if brightness != 0:
            result = result + (brightness * 1.5)

        # Contrast adjustment
        if contrast != 0:
            factor = (259.0 * (contrast + 255.0)) / (255.0 * (259.0 - contrast))
            result = factor * (result - 128.0) + 128.0

        result = np.clip(result, 0, 255).astype(np.uint8)

        # Saturation adjustment using HSV color space
        if saturation != 0:
            hsv = cv2.cvtColor(result, cv2.COLOR_BGR2HSV).astype(np.float32)
            h, s, v = cv2.split(hsv)
            sat_factor = 1.0 + (saturation / 100.0)
            s = np.clip(s * sat_factor, 0, 255)
            hsv_merged = cv2.merge([h, s, v]).astype(np.uint8)
            result = cv2.cvtColor(hsv_merged, cv2.COLOR_HSV2BGR)

        return result

    @staticmethod
    def apply_vignette(img, amount=0.4):
        """
        Tạo hiệu ứng Vignette (tối dần 4 góc ảnh)
        """
        if amount <= 0:
            return img
        rows, cols = img.shape[:2]
        kernel_x = cv2.getGaussianKernel(cols, cols * (1.1 - amount * 0.5))
        kernel_y = cv2.getGaussianKernel(rows, rows * (1.1 - amount * 0.5))
        kernel = kernel_y * kernel_x.T
        mask = kernel / kernel.max()
        
        vignette_img = np.copy(img)
        for i in range(3):
            vignette_img[:, :, i] = vignette_img[:, :, i] * mask
        return np.clip(vignette_img, 0, 255).astype(np.uint8)

    @classmethod
    def apply_filter(cls, img, filter_name="none", beauty_level=0.5, brightness=0, contrast=0, saturation=0):
        """
        Áp dụng toàn bộ quy trình bộ lọc màu OpenCV lên ảnh BGR
        """
        processed = img.copy()

        # 1. Làm mịn da trước
        if beauty_level > 0:
            processed = cls.apply_beauty_filter(processed, beauty_level)

        # 2. Áp dụngpreset bộ lọc màu OpenCV
        if filter_name == "korean_pink":
            # Tông hồng phấn pastel Hàn Quốc
            b, g, r = cv2.split(processed.astype(np.float32))
            r = r * 1.15 + 10
            g = g * 1.02 + 5
            b = b * 1.08 + 12
            processed = cv2.merge([b, g, r])
            processed = np.clip(processed, 0, 255).astype(np.uint8)
            processed = cls.adjust_brightness_contrast_saturation(processed, brightness=5, contrast=10, saturation=15)

        elif filter_name == "vintage_90s":
            # Tông hoài cổ 90s (Sepia/Warm Amber)
            kernel = np.array([
                [0.272, 0.534, 0.131],
                [0.349, 0.686, 0.168],
                [0.393, 0.769, 0.189]
            ])
            processed = cv2.transform(processed, kernel)
            processed = np.clip(processed, 0, 255).astype(np.uint8)
            processed = cls.apply_vignette(processed, amount=0.35)

        elif filter_name == "bw_contrast":
            # Đen trắng tương phản cao (Monochrome Classic)
            gray = cv2.cvtColor(processed, cv2.COLOR_BGR2GRAY)
            # Áp dụng CLAHE (Contrast Limited Adaptive Histogram Equalization)
            clahe = cv2.createCLAHE(clipLimit=2.5, tileGridSize=(8, 8))
            gray_clahe = clahe.apply(gray)
            processed = cv2.cvtColor(gray_clahe, cv2.COLOR_GRAY2BGR)

        elif filter_name == "y2k_cyber":
            # Phong cách Y2K Neon/Cyberpunk
            hsv = cv2.cvtColor(processed, cv2.COLOR_BGR2HSV).astype(np.float32)
            h, s, v = cv2.split(hsv)
            s = s * 1.4 + 20
            v = v * 1.08
            hsv_boosted = cv2.merge([h, s, v])
            hsv_boosted = np.clip(hsv_boosted, 0, 255).astype(np.uint8)
            processed = cv2.cvtColor(hsv_boosted, cv2.COLOR_HSV2BGR)
            processed = cls.adjust_brightness_contrast_saturation(processed, contrast=18, saturation=25)

        elif filter_name == "warm_retro":
            # Phong cáchRetro ấm áp
            b, g, r = cv2.split(processed.astype(np.float32))
            r = r * 1.18 + 15
            g = g * 1.05 + 8
            b = b * 0.88
            processed = cv2.merge([b, g, r])
            processed = np.clip(processed, 0, 255).astype(np.uint8)

        elif filter_name == "soft_glow":
            # Hiệu ứng mộng mơ Soft Glow (Dreamy Aura)
            blurred = cv2.GaussianBlur(processed, (21, 21), 0)
            processed = cv2.addWeighted(processed, 0.75, blurred, 0.35, 5)

        elif filter_name == "cinema_teal":
            # Tone điện ảnh Teal & Orange
            b, g, r = cv2.split(processed.astype(np.float32))
            b = b * 1.15 + 10  # Boost Teal/Blue in shadows
            r = r * 1.12 + 8   # Boost Orange/Red in highlights
            processed = cv2.merge([b, g, r])
            processed = np.clip(processed, 0, 255).astype(np.uint8)

        # 3. Tùy chỉnh tham số phụ từ người dùng (Brightness, Contrast, Saturation)
        if brightness != 0 or contrast != 0 or saturation != 0:
            processed = cls.adjust_brightness_contrast_saturation(
                processed, brightness=brightness, contrast=contrast, saturation=saturation
            )

        return processed
