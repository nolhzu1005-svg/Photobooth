import os
import io
import base64
import time
import uuid
from flask import Flask, render_template, request, jsonify, send_from_directory, url_for
from flask_cors import CORS
import cv2
import numpy as np
# pyrefly: ignore [missing-import]
from PIL import Image

from utils.image_processor import ImageProcessor
from utils.strip_generator import StripGenerator

app = Flask(__name__, static_folder='static', template_folder='templates')
CORS(app)

# Thư mục lưu ảnh đã tạo để tải về và tạo QR code
UPLOAD_FOLDER = os.path.join(app.static_folder, 'uploads')
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

def base64_to_cv2(b64_string):
    """Chuyển đổi chuỗi Base64 Data URL sang OpenCV BGR numpy array"""
    if ',' in b64_string:
        b64_string = b64_string.split(',')[1]
    img_bytes = base64.b64decode(b64_string)
    nparr = np.frombuffer(img_bytes, np.uint8)
    img_bgr = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    return img_bgr

def cv2_to_base64(img_bgr, format='png'):
    """Chuyển đổi OpenCV BGR image sang chuỗi Base64 Data URL"""
    _, buffer = cv2.imencode(f'.{format}', img_bgr)
    b64_str = base64.b64encode(buffer).decode('utf-8')
    return f"data:image/{format};base64,{b64_str}"

def base64_to_pil(b64_string):
    """Chuyển đổi chuỗi Base64 sang PIL Image"""
    if ',' in b64_string:
        b64_string = b64_string.split(',')[1]
    img_bytes = base64.b64decode(b64_string)
    return Image.open(io.BytesIO(img_bytes)).convert("RGB")

@app.route('/')
def index():
    """Phục vụ giao diện Web Photobooth HTML5"""
    return render_template('index.html')

@app.route('/api/process-filter', methods=['POST'])
def process_filter():
    """
    REST API: Áp dụng bộ lọc OpenCV (Filter, làm mượt da, chỉnh sáng/tương phản)
    Input: JSON { image, filter_name, beauty_level, brightness, contrast, saturation }
    Output: JSON { status, image_processed }
    """
    try:
        data = request.json
        b64_img = data.get('image')
        filter_name = data.get('filter_name', 'none')
        beauty_level = float(data.get('beauty_level', 0.5))
        brightness = int(data.get('brightness', 0))
        contrast = int(data.get('contrast', 0))
        saturation = int(data.get('saturation', 0))

        if not b64_img:
            return jsonify({'status': 'error', 'message': 'Thiếu dữ liệu hình ảnh'}), 400

        # Xử lý bằng OpenCV
        cv2_img = base64_to_cv2(b64_img)
        processed_bgr = ImageProcessor.apply_filter(
            cv2_img, 
            filter_name=filter_name, 
            beauty_level=beauty_level,
            brightness=brightness,
            contrast=contrast,
            saturation=saturation
        )
        
        result_b64 = cv2_to_base64(processed_bgr)
        return jsonify({
            'status': 'success',
            'image_processed': result_b64
        })

    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500

@app.route('/api/create-strip', methods=['POST'])
def create_strip():
    """
    REST API: Ghép chuỗi ảnh đã chụp thành Photostrip chất lượng cao (Pillow)
    Input: JSON { images: [b64_1, b64_2, ...], layout, theme, custom_text, date_text }
    Output: JSON { status, strip_url, qr_url, b64_strip }
    """
    try:
        data = request.json
        images_b64 = data.get('images', [])
        layout = data.get('layout', '1x4')
        theme = data.get('theme', 'korean_pink')
        custom_text = data.get('custom_text', 'SNAPVIBE 2026')
        date_text = data.get('date_text', time.strftime("%Y.%m.%d"))
        stickers = data.get('stickers', [])

        if not images_b64:
            return jsonify({'status': 'error', 'message': 'Không có ảnh chụp'}), 400

        pil_imgs = [base64_to_pil(b64) for b64 in images_b64]

        # Ghép dải ảnh Photostrip bằng Pillow
        strip_canvas = StripGenerator.compose_photostrip(
            pil_imgs,
            layout=layout,
            theme=theme,
            custom_text=custom_text,
            date_text=date_text,
            stickers=stickers
        )

        # Lưu ảnh ra file static/uploads để tạo QR code tải về
        filename = f"strip_{uuid.uuid4().hex[:10]}.png"
        filepath = os.path.join(UPLOAD_FOLDER, filename)
        strip_canvas.save(filepath, format="PNG")

        # Đường dẫn tải về công khai
        download_url = request.host_url.rstrip('/') + url_for('static', filename=f'uploads/{filename}')
        
        # Tạo mã QR Code
        qr_img = StripGenerator.generate_qr_code(download_url)
        qr_filename = f"qr_{uuid.uuid4().hex[:10]}.png"
        qr_filepath = os.path.join(UPLOAD_FOLDER, qr_filename)
        qr_img.save(qr_filepath)
        qr_url = request.host_url.rstrip('/') + url_for('static', filename=f'uploads/{qr_filename}')

        # Base64 string cho preview trực tiếp
        buffered = io.BytesIO()
        strip_canvas.save(buffered, format="PNG")
        b64_strip = f"data:image/png;base64,{base64.b64encode(buffered.getvalue()).decode('utf-8')}"

        return jsonify({
            'status': 'success',
            'strip_url': download_url,
            'qr_url': qr_url,
            'b64_strip': b64_strip
        })

    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500

@app.route('/api/generate-gif', methods=['POST'])
def generate_gif():
    """
    REST API: Tạo ảnh GIF chuyển động từ các bức ảnh đã chụp
    """
    try:
        data = request.json
        images_b64 = data.get('images', [])
        if not images_b64:
            return jsonify({'status': 'error', 'message': 'Không có dữ liệu ảnh'}), 400

        pil_imgs = [base64_to_pil(b64) for b64 in images_b64]
        gif_bytes = StripGenerator.create_gif(pil_imgs, duration_ms=450)

        filename = f"animation_{uuid.uuid4().hex[:10]}.gif"
        filepath = os.path.join(UPLOAD_FOLDER, filename)
        with open(filepath, 'wb') as f:
            f.write(gif_bytes)

        gif_url = request.host_url.rstrip('/') + url_for('static', filename=f'uploads/{filename}')
        b64_gif = f"data:image/gif;base64,{base64.b64encode(gif_bytes).decode('utf-8')}"

        return jsonify({
            'status': 'success',
            'gif_url': gif_url,
            'b64_gif': b64_gif
        })

    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500

if __name__ == '__main__':
    print("=========================================================")
    print("  SNAPVIBE 2026 - PHOTOBOOTH ONLINE FLASK BACKEND SERVER ")
    print("  OpenCV + Pillow Image Processing Engine Active         ")
    print("  Server running on http://127.0.0.1:5000               ")
    print("=========================================================")
    app.run(host='0.0.0.0', port=5000, debug=True)
