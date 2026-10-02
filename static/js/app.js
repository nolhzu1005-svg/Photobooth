/* ==========================================================================
   SNAPVIBE PRO 2026 - PHOTOBOOTH ONLINE JAVASCRIPT ENGINE
   Web Studio Panel & Mobile Responsive
   ========================================================================== */

document.addEventListener('DOMContentLoaded', () => {
    // --- Application State ---
    const state = {
        currentStep: 1,
        facingMode: 'user',
        isMirrored: true,
        timerSeconds: 5,
        targetShots: 4,
        capturedPhotos: [],
        processedPhotos: [],
        selectedPhotoIndex: 0,
        
        activeFilter: 'none',
        beautyLevel: 0.65,
        brightness: 0,
        contrast: 0,
        saturation: 0,

        activeLayout: '1x4',
        activeTheme: 'korean_pink',
        customHeader: 'SNAPVIBE 2026 ✦',
        customDate: new Date().toISOString().split('T')[0].replace(/-/g, '.'),
        selectedStickers: [],

        currentStripUrl: '',
        currentQrUrl: '',
        currentGifUrl: '',
        currentB64Strip: ''
    };

    // --- DOM Elements ---
    const webcam = document.getElementById('webcam');
    const cameraCanvas = document.getElementById('cameraCanvas');
    const flashOverlay = document.getElementById('flashOverlay');
    const countdownBox = document.getElementById('countdownBox');
    const countdownNumber = document.getElementById('countdownNumber');
    
    const selTimer = document.getElementById('selTimer');
    const selShots = document.getElementById('selShots');
    const btnStartCapture = document.getElementById('btnStartCapture');
    const fileUpload = document.getElementById('fileUpload');
    const thumbList = document.getElementById('thumbList');
    const photoCount = document.getElementById('photoCount');
    const btnProceedToFilter = document.getElementById('btnProceedToFilter');
    const btnSwitchCamera = document.getElementById('btnSwitchCamera');
    const btnToggleMirror = document.getElementById('btnToggleMirror');

    // Filter Step Elements
    const photoTabs = document.getElementById('photoTabs');
    const filterPreviewImg = document.getElementById('filterPreviewImg');
    const filterLoading = document.getElementById('filterLoading');
    const presetBtns = document.querySelectorAll('.preset-btn');
    const rngBeauty = document.getElementById('rngBeauty');
    const rngBrightness = document.getElementById('rngBrightness');
    const rngContrast = document.getElementById('rngContrast');
    const rngSaturation = document.getElementById('rngSaturation');
    const valBeauty = document.getElementById('valBeauty');
    const valBrightness = document.getElementById('valBrightness');
    const valContrast = document.getElementById('valContrast');
    const valSaturation = document.getElementById('valSaturation');
    const btnBackToCamera = document.getElementById('btnBackToCamera');
    const btnApplyAllPhotos = document.getElementById('btnApplyAllPhotos');
    const btnProceedToFrame = document.getElementById('btnProceedToFrame');

    // Frame Step Elements
    const pillBtns = document.querySelectorAll('.pill-btn');
    const themeCards = document.querySelectorAll('.theme-card');
    const txtCustomHeader = document.getElementById('txtCustomHeader');
    const txtCustomDate = document.getElementById('txtCustomDate');
    const stripResultImg = document.getElementById('stripResultImg');
    const btnBackToFilter = document.getElementById('btnBackToFilter');
    const btnProceedToPreview = document.getElementById('btnProceedToPreview');

    // Sticker Studio Elements
    const stickerBtns = document.querySelectorAll('.sticker-btn');
    const btnClearStickers = document.getElementById('btnClearStickers');
    const txtCustomSticker = document.getElementById('txtCustomSticker');
    const btnAddCustomSticker = document.getElementById('btnAddCustomSticker');
    const activeStickersContainer = document.getElementById('activeStickersContainer');

    // Preview Step Elements
    const previewFinalImg = document.getElementById('previewFinalImg');
    const btnBackToFrame = document.getElementById('btnBackToFrame');
    const btnProceedToExport = document.getElementById('btnProceedToExport');

    // Export Step Elements
    const exportQrImg = document.getElementById('exportQrImg');
    const btnDownloadPng = document.getElementById('btnDownloadPng');
    const btnGenerateGif = document.getElementById('btnGenerateGif');
    const btnPrintStrip = document.getElementById('btnPrintStrip');
    const btnRestartApp = document.getElementById('btnRestartApp');

    // --- WebRTC Camera ---
    let mediaStream = null;

    async function initCamera() {
        if (mediaStream) {
            mediaStream.getTracks().forEach(track => track.stop());
        }
        try {
            mediaStream = await navigator.mediaDevices.getUserMedia({
                video: { facingMode: state.facingMode, width: { ideal: 1280 }, height: { ideal: 960 } },
                audio: false
            });
            webcam.srcObject = mediaStream;
        } catch (err) {
            console.warn('Camera error:', err);
        }
    }

    btnSwitchCamera.addEventListener('click', () => {
        state.facingMode = (state.facingMode === 'user') ? 'environment' : 'user';
        initCamera();
    });

    btnToggleMirror.addEventListener('click', () => {
        state.isMirrored = !state.isMirrored;
        webcam.classList.toggle('no-mirror', !state.isMirrored);
    });

    initCamera();

    selTimer.addEventListener('change', (e) => state.timerSeconds = parseInt(e.target.value));
    selShots.addEventListener('change', (e) => state.targetShots = parseInt(e.target.value));
    txtCustomDate.value = state.customDate;

    // Camera Capture Sequence
    btnStartCapture.addEventListener('click', async () => {
        btnStartCapture.disabled = true;
        state.capturedPhotos = [];
        updateThumbnails();

        for (let i = 0; i < state.targetShots; i++) {
            await runCountdown(state.timerSeconds);
            triggerFlash();
            captureSingleFrame();
            updateThumbnails();
            await new Promise(r => setTimeout(r, 600));
        }

        btnStartCapture.disabled = false;
        if (state.capturedPhotos.length > 0) {
            state.processedPhotos = [...state.capturedPhotos];
            btnProceedToFilter.disabled = false;
        }
    });

    function runCountdown(seconds) {
        return new Promise(resolve => {
            let count = seconds;
            countdownBox.style.display = 'flex';
            countdownNumber.innerText = count;

            const timer = setInterval(() => {
                count--;
                if (count > 0) {
                    countdownNumber.innerText = count;
                } else {
                    clearInterval(timer);
                    countdownBox.style.display = 'none';
                    resolve();
                }
            }, 1000);
        });
    }

    function triggerFlash() {
        flashOverlay.classList.add('active');
        setTimeout(() => flashOverlay.classList.remove('active'), 180);
    }

    function captureSingleFrame() {
        const ctx = cameraCanvas.getContext('2d');
        cameraCanvas.width = webcam.videoWidth || 640;
        cameraCanvas.height = webcam.videoHeight || 480;

        ctx.save();
        if (state.isMirrored && state.facingMode === 'user') {
            ctx.translate(cameraCanvas.width, 0);
            ctx.scale(-1, 1);
        }
        ctx.drawImage(webcam, 0, 0, cameraCanvas.width, cameraCanvas.height);
        ctx.restore();

        const b64Data = cameraCanvas.toDataURL('image/jpeg', 0.92);
        state.capturedPhotos.push(b64Data);
    }

    fileUpload.addEventListener('change', (e) => {
        const files = Array.from(e.target.files);
        if (files.length === 0) return;

        state.capturedPhotos = [];
        let loaded = 0;
        files.slice(0, 6).forEach(file => {
            const reader = new FileReader();
            reader.onload = (evt) => {
                state.capturedPhotos.push(evt.target.result);
                loaded++;
                if (loaded === Math.min(files.length, 6)) {
                    state.processedPhotos = [...state.capturedPhotos];
                    updateThumbnails();
                    btnProceedToFilter.disabled = false;
                }
            };
            reader.readAsDataURL(file);
        });
    });

    function updateThumbnails() {
        photoCount.innerText = state.capturedPhotos.length;
        if (state.capturedPhotos.length === 0) {
            thumbList.innerHTML = `<div class="thumb-placeholder">Chưa có ảnh. Bấm "Bắt đầu chụp" để bắt đầu!</div>`;
            return;
        }

        thumbList.innerHTML = '';
        state.capturedPhotos.forEach((b64, idx) => {
            const item = document.createElement('div');
            item.className = 'thumb-item';
            item.innerHTML = `<img src="${b64}" alt="Photo ${idx+1}">`;
            thumbList.appendChild(item);
        });
    }

    // --- STEP NAVIGATION ---
    function navigateToStep(step) {
        state.currentStep = step;
        document.querySelectorAll('.studio-section').forEach(sec => sec.classList.remove('active'));
        document.querySelectorAll('.step').forEach((st, idx) => {
            st.classList.toggle('active', idx + 1 <= step);
        });

        if (step === 1) document.getElementById('secCamera').classList.add('active');
        if (step === 2) {
            document.getElementById('secFilter').classList.add('active');
            setupFilterStep();
        }
        if (step === 3) {
            document.getElementById('secFrame').classList.add('active');
            requestRenderPhotostrip();
        }
        if (step === 4) {
            document.getElementById('secPreview').classList.add('active');
        }
        if (step === 5) {
            document.getElementById('secExport').classList.add('active');
            if (typeof confetti === 'function') confetti({ particleCount: 70, spread: 60, origin: { y: 0.6 } });
        }
    }

    btnProceedToFilter.addEventListener('click', () => navigateToStep(2));
    btnBackToCamera.addEventListener('click', () => navigateToStep(1));
    btnProceedToFrame.addEventListener('click', () => navigateToStep(3));
    btnBackToFilter.addEventListener('click', () => navigateToStep(2));
    btnProceedToPreview.addEventListener('click', () => navigateToStep(4));
    btnBackToFrame.addEventListener('click', () => navigateToStep(3));
    btnProceedToExport.addEventListener('click', () => navigateToStep(5));

    // STEP 2: OPENCV FILTER
    function setupFilterStep() {
        photoTabs.innerHTML = '';
        state.capturedPhotos.forEach((_, idx) => {
            const tab = document.createElement('button');
            tab.className = `photo-tab ${idx === state.selectedPhotoIndex ? 'active' : ''}`;
            tab.innerText = `Ảnh ${idx + 1}`;
            tab.addEventListener('click', () => {
                state.selectedPhotoIndex = idx;
                setupFilterStep();
            });
            photoTabs.appendChild(tab);
        });
        applyOpenCVFilter();
    }

    presetBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            presetBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            state.activeFilter = btn.dataset.filter;
            applyOpenCVFilter();
        });
    });

    rngBeauty.addEventListener('input', (e) => {
        valBeauty.innerText = `${e.target.value}%`;
        state.beautyLevel = parseInt(e.target.value) / 100.0;
        debouncedFilterApply();
    });
    rngBrightness.addEventListener('input', (e) => {
        valBrightness.innerText = e.target.value;
        state.brightness = parseInt(e.target.value);
        debouncedFilterApply();
    });
    rngContrast.addEventListener('input', (e) => {
        valContrast.innerText = e.target.value;
        state.contrast = parseInt(e.target.value);
        debouncedFilterApply();
    });
    rngSaturation.addEventListener('input', (e) => {
        valSaturation.innerText = e.target.value;
        state.saturation = parseInt(e.target.value);
        debouncedFilterApply();
    });

    let filterDebounceTimer = null;
    function debouncedFilterApply() {
        clearTimeout(filterDebounceTimer);
        filterDebounceTimer = setTimeout(applyOpenCVFilter, 150);
    }

    async function applyOpenCVFilter() {
        const rawB64 = state.capturedPhotos[state.selectedPhotoIndex];
        if (!rawB64) return;
        filterLoading.style.display = 'flex';

        try {
            const response = await fetch('/api/process-filter', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    image: rawB64,
                    filter_name: state.activeFilter,
                    beauty_level: state.beautyLevel,
                    brightness: state.brightness,
                    contrast: state.contrast,
                    saturation: state.saturation
                })
            });
            const result = await response.json();
            if (result.status === 'success') {
                filterPreviewImg.src = result.image_processed;
                state.processedPhotos[state.selectedPhotoIndex] = result.image_processed;
            }
        } catch (err) {
            console.error('Lỗi OpenCV Filter:', err);
        } finally {
            filterLoading.style.display = 'none';
        }
    }

    btnApplyAllPhotos.addEventListener('click', async () => {
        filterLoading.style.display = 'flex';
        for (let i = 0; i < state.capturedPhotos.length; i++) {
            try {
                const response = await fetch('/api/process-filter', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        image: state.capturedPhotos[i],
                        filter_name: state.activeFilter,
                        beauty_level: state.beautyLevel,
                        brightness: state.brightness,
                        contrast: state.contrast,
                        saturation: state.saturation
                    })
                });
                const result = await response.json();
                if (result.status === 'success') {
                    state.processedPhotos[i] = result.image_processed;
                }
            } catch (err) {
                console.error(err);
            }
        }
        filterLoading.style.display = 'none';
        alert('Đã áp dụng bộ lọc OpenCV cho tất cả bức ảnh!');
    });

    // STEP 3: FRAME STUDIO
    pillBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            pillBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            state.activeLayout = btn.dataset.layout;
            requestRenderPhotostrip();
        });
    });

    themeCards.forEach(card => {
        card.addEventListener('click', () => {
            themeCards.forEach(c => c.classList.remove('active'));
            card.classList.add('active');
            state.activeTheme = card.dataset.theme;
            requestRenderPhotostrip();
        });
    });

    txtCustomHeader.addEventListener('input', (e) => {
        state.customHeader = e.target.value;
        debouncedStripRender();
    });
    txtCustomDate.addEventListener('input', (e) => {
        state.customDate = e.target.value;
        debouncedStripRender();
    });

    // STICKER STUDIO LOGIC
    stickerBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const stickerSymbol = btn.dataset.sticker || btn.innerText.trim();
            const idx = state.selectedStickers.indexOf(stickerSymbol);
            if (idx > -1) {
                state.selectedStickers.splice(idx, 1);
                btn.classList.remove('active');
            } else {
                state.selectedStickers.push(stickerSymbol);
                btn.classList.add('active');
            }
            updateStickerBadgesUI();
            requestRenderPhotostrip();
        });
    });

    if (btnAddCustomSticker && txtCustomSticker) {
        const addCustomStickerHandler = () => {
            const val = txtCustomSticker.value.trim();
            if (val) {
                state.selectedStickers.push(val);
                txtCustomSticker.value = '';
                updateStickerBadgesUI();
                requestRenderPhotostrip();
            }
        };

        btnAddCustomSticker.addEventListener('click', addCustomStickerHandler);
        txtCustomSticker.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') addCustomStickerHandler();
        });
    }

    if (btnClearStickers) {
        btnClearStickers.addEventListener('click', () => {
            state.selectedStickers = [];
            stickerBtns.forEach(b => b.classList.remove('active'));
            updateStickerBadgesUI();
            requestRenderPhotostrip();
        });
    }

    function updateStickerBadgesUI() {
        if (!activeStickersContainer) return;
        activeStickersContainer.innerHTML = '';
        state.selectedStickers.forEach((stk, index) => {
            const pill = document.createElement('span');
            pill.className = 'sticker-badge-pill';
            pill.innerHTML = `${stk} <i class="fa-solid fa-xmark btn-remove-badge" data-index="${index}"></i>`;
            pill.querySelector('.btn-remove-badge').addEventListener('click', (e) => {
                e.stopPropagation();
                state.selectedStickers.splice(index, 1);
                // Sync button active class
                stickerBtns.forEach(b => {
                    if ((b.dataset.sticker || b.innerText.trim()) === stk && !state.selectedStickers.includes(stk)) {
                        b.classList.remove('active');
                    }
                });
                updateStickerBadgesUI();
                requestRenderPhotostrip();
            });
            activeStickersContainer.appendChild(pill);
        });
    }

    let stripDebounceTimer = null;
    function debouncedStripRender() {
        clearTimeout(stripDebounceTimer);
        stripDebounceTimer = setTimeout(requestRenderPhotostrip, 200);
    }

    async function requestRenderPhotostrip() {
        if (state.processedPhotos.length === 0) return;

        try {
            const response = await fetch('/api/create-strip', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    images: state.processedPhotos,
                    layout: state.activeLayout,
                    theme: state.activeTheme,
                    custom_text: state.customHeader,
                    date_text: state.customDate,
                    stickers: state.selectedStickers
                })
            });
            const result = await response.json();
            if (result.status === 'success') {
                stripResultImg.src = result.b64_strip;
                previewFinalImg.src = result.b64_strip;
                exportQrImg.src = result.qr_url;
                btnDownloadPng.href = result.strip_url;

                state.currentStripUrl = result.strip_url;
                state.currentQrUrl = result.qr_url;
                state.currentB64Strip = result.b64_strip;
            }
        } catch (err) {
            console.error('Lỗi Pillow Photostrip:', err);
        }
    }

    // STEP 5: EXPORT & GIF
    btnGenerateGif.addEventListener('click', async () => {
        btnGenerateGif.disabled = true;
        btnGenerateGif.innerHTML = `<i class="fa-solid fa-circle-notch fa-spin"></i> Đang Tạo GIF...`;

        try {
            const response = await fetch('/api/generate-gif', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ images: state.processedPhotos })
            });
            const result = await response.json();
            if (result.status === 'success') {
                const a = document.createElement('a');
                a.href = result.gif_url;
                a.download = 'snapvibe_animation.gif';
                a.click();
            }
        } catch (err) {
            console.error('Lỗi tạo GIF:', err);
        } finally {
            btnGenerateGif.disabled = false;
            btnGenerateGif.innerHTML = `<i class="fa-solid fa-film"></i> Tạo & Tải Ảnh GIF Động`;
        }
    });

    btnPrintStrip.addEventListener('click', () => {
        const win = window.open('');
        win.document.write(`
            <html>
                <head><title>In Photostrip - SnapVibe 2026</title></head>
                <body style="margin:0; display:flex; justify-content:center; align-items:center; height:100vh;">
                    <img src="${previewFinalImg.src}" style="max-height:95vh;" onload="window.print();window.close();" />
                </body>
            </html>
        `);
        win.document.close();
    });

    btnRestartApp.addEventListener('click', () => {
        state.capturedPhotos = [];
        state.processedPhotos = [];
        updateThumbnails();
        btnProceedToFilter.disabled = true;
        navigateToStep(1);
    });
});
