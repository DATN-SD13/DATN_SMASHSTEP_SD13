<script setup>
import {
  nextTick,
  onBeforeUnmount,
  ref
} from 'vue'

import { Html5Qrcode } from 'html5-qrcode'

const emit = defineEmits([
  'close',
  'scanned'
])

const readerId =
  `cccd-reader-${Date.now()}`

const scanner = ref(null)
const fileInput = ref(null)
const previewUrl = ref('')

const starting = ref(false)
const scanning = ref(false)
const processingImage = ref(false)

const error = ref('')

let finished = false

/* ================================
   XỬ LÝ DỮ LIỆU CCCD
================================ */

function normalizeText(value) {
  return String(value ?? '').trim()
}

function clearPreview() {
  if (previewUrl.value) {
    URL.revokeObjectURL(
      previewUrl.value
    )

    previewUrl.value = ''
  }
}

function parseDate(value) {
  const text =
    normalizeText(value)
      .replace(/\D/g, '')

  if (text.length !== 8) {
    return ''
  }

  const day =
    text.slice(0, 2)

  const month =
    text.slice(2, 4)

  const year =
    text.slice(4, 8)

  return `${year}-${month}-${day}`
}

function parseGender(value) {
  const text =
    normalizeText(value)
      .toLowerCase()

  if (text === 'nam') {
    return 1
  }

  if (
    text === 'nữ' ||
    text === 'nu'
  ) {
    return 2
  }

  return 0
}

function parseCccd(text) {
  const parts =
    String(text)
      .split('|')
      .map(item => item.trim())

  if (parts.length < 6) {
    throw new Error(
      'QR không đúng định dạng CCCD.'
    )
  }

  return {
    cccd:
      parts[0] || '',

    oldCccd:
      parts[1] || '',

    name:
      parts[2] || '',

    dob:
      parseDate(parts[3]),

    gender:
      parseGender(parts[4]),

    address:
      parts[5] || '',

    issueDate:
      parseDate(parts[6]),

    raw:
      text
  }
}

/* ================================
   CAMERA
================================ */

async function stopScanner() {
  const current =
    scanner.value

  if (!current) {
    scanning.value = false
    return
  }

  try {
    /*
     * stop() chỉ dùng khi camera
     * thực sự đang chạy.
     */
    if (scanning.value) {
      await current.stop()
    }
  } catch (e) {
    console.warn(
      'Không thể dừng camera:',
      e
    )
  }

  try {
    await current.clear()
  } catch (e) {
    console.warn(
      'Không thể clear scanner:',
      e
    )
  }

  scanner.value = null
  scanning.value = false
}

async function handleSuccess(
  decodedText
) {
  if (finished) {
    return
  }

  try {
    const data =
      parseCccd(decodedText)

    finished = true

    await stopScanner()

    emit(
      'scanned',
      data
    )

  } catch (e) {
    error.value =
      e?.message ||
      'Không đọc được dữ liệu CCCD.'
  }
}

async function startScanner() {
  if (
    scanning.value ||
    starting.value ||
    processingImage.value
  ) {
    return
  }

  error.value = ''

/*
 * Mở camera thì bỏ ảnh preview.
 */
clearPreview()

starting.value = true
finished = false

  await nextTick()

  try {
    await stopScanner()

    await nextTick()

    const html5QrCode =
      new Html5Qrcode(
        readerId
      )

    scanner.value =
      html5QrCode

    await html5QrCode.start(
      {
        facingMode:
          'environment'
      },

      {
        fps: 10,

        qrbox: {
          width: 280,
          height: 180
        },

        /*
         * Cho phép thử nhiều định dạng
         * kích thước camera hơn.
         */
        aspectRatio: 1.777778
      },

      handleSuccess,

      () => {
        /*
         * Không hiện lỗi mỗi frame.
         */
      }
    )

    scanning.value = true

  } catch (e) {
    console.error(e)

    error.value =
      'Không mở được camera. ' +
      'Bạn có thể chọn ảnh QR CCCD bên dưới.'

  } finally {
    starting.value = false
  }
}

/* ================================
   XỬ LÝ ẢNH
================================ */

/*
 * Đọc File thành Image.
 */
function loadImage(file) {
  return new Promise(
    (resolve, reject) => {
      const image =
        new Image()

      const url =
        URL.createObjectURL(
          file
        )

      image.onload = () => {
        URL.revokeObjectURL(
          url
        )

        resolve(image)
      }

      image.onerror = () => {
        URL.revokeObjectURL(
          url
        )

        reject(
          new Error(
            'Không thể đọc ảnh.'
          )
        )
      }

      image.src = url
    }
  )
}

/*
 * Canvas -> File PNG.
 */
function canvasToFile(
  canvas,
  name
) {
  return new Promise(
    (resolve, reject) => {
      canvas.toBlob(
        blob => {
          if (!blob) {
            reject(
              new Error(
                'Không xử lý được ảnh.'
              )
            )

            return
          }

          resolve(
            new File(
              [blob],
              name,
              {
                type:
                  'image/png'
              }
            )
          )
        },

        'image/png',
        1
      )
    }
  )
}

/*
 * Resize ảnh:
 *
 * - ảnh quá lớn -> giảm xuống
 * - ảnh nhỏ -> phóng lên
 *
 * Mục đích là QR không quá nhỏ
 * đối với decoder.
 */
function getTargetSize(
  sourceWidth,
  sourceHeight
) {
  let width =
    sourceWidth

  let height =
    sourceHeight

  const longest =
    Math.max(
      width,
      height
    )

  /*
   * Ảnh nhỏ:
   * phóng lên.
   */
  if (longest < 1600) {
    const scale =
      1600 / longest

    width =
      Math.round(
        width * scale
      )

    height =
      Math.round(
        height * scale
      )
  }

  /*
   * Ảnh điện thoại có thể 4K-8K.
   * Không cần để canvas quá lớn.
   */
  const newLongest =
    Math.max(
      width,
      height
    )

  if (newLongest > 3000) {
    const scale =
      3000 / newLongest

    width =
      Math.round(
        width * scale
      )

    height =
      Math.round(
        height * scale
      )
  }

  return {
    width,
    height
  }
}

/*
 * Tạo canvas từ một vùng ảnh.
 */
function createCanvasFromImage(
  image,
  sx,
  sy,
  sw,
  sh,
  enhance = false
) {
  const {
    width,
    height
  } = getTargetSize(
    sw,
    sh
  )

  const canvas =
    document.createElement(
      'canvas'
    )

  canvas.width = width
  canvas.height = height

  const ctx =
    canvas.getContext(
      '2d',
      {
        willReadFrequently: true
      }
    )

  /*
   * Làm mượt khi resize.
   */
  ctx.imageSmoothingEnabled =
    true

  ctx.imageSmoothingQuality =
    'high'

  ctx.drawImage(
    image,

    sx,
    sy,
    sw,
    sh,

    0,
    0,
    width,
    height
  )

  if (enhance) {
    enhanceCanvas(
      ctx,
      width,
      height
    )
  }

  return canvas
}

/*
 * Grayscale + tăng contrast.
 */
function enhanceCanvas(
  ctx,
  width,
  height
) {
  const imageData =
    ctx.getImageData(
      0,
      0,
      width,
      height
    )

  const pixels =
    imageData.data

  /*
   * Contrast 55:
   * đủ mạnh với ảnh CCCD
   * nhưng không quá gắt.
   */
  const contrast = 55

  const factor =
    (
      259 *
      (contrast + 255)
    ) /
    (
      255 *
      (259 - contrast)
    )

  for (
    let i = 0;
    i < pixels.length;
    i += 4
  ) {
    const gray =
      0.299 *
        pixels[i] +
      0.587 *
        pixels[i + 1] +
      0.114 *
        pixels[i + 2]

    let value =
      factor *
        (gray - 128) +
      128

    value =
      Math.max(
        0,
        Math.min(
          255,
          value
        )
      )

    pixels[i] =
      value

    pixels[i + 1] =
      value

    pixels[i + 2] =
      value
  }

  ctx.putImageData(
    imageData,
    0,
    0
  )
}

/*
 * Tạo nhiều phiên bản ảnh.
 *
 * Đây là phần quan trọng:
 * nếu QR quá nhỏ trong ảnh CCCD,
 * crop từng khu vực sẽ giúp decoder
 * nhìn QR lớn hơn.
 */
async function createImageVariants(
  file
) {
  const image =
    await loadImage(file)

  const w =
    image.naturalWidth

  const h =
    image.naturalHeight

  const files = []

  /*
   * 1. Toàn bộ ảnh đã resize.
   */
  {
    const canvas =
      createCanvasFromImage(
        image,
        0,
        0,
        w,
        h,
        false
      )

    files.push(
      await canvasToFile(
        canvas,
        'cccd-full.png'
      )
    )
  }

  /*
   * 2. Toàn bộ ảnh:
   * grayscale + contrast.
   */
  {
    const canvas =
      createCanvasFromImage(
        image,
        0,
        0,
        w,
        h,
        true
      )

    files.push(
      await canvasToFile(
        canvas,
        'cccd-enhanced.png'
      )
    )
  }

  /*
   * 3. Nửa trái.
   */
  {
    const canvas =
      createCanvasFromImage(
        image,
        0,
        0,
        Math.ceil(w * 0.6),
        h,
        true
      )

    files.push(
      await canvasToFile(
        canvas,
        'cccd-left.png'
      )
    )
  }

  /*
   * 4. Nửa phải.
   */
  {
    const startX =
      Math.floor(
        w * 0.4
      )

    const canvas =
      createCanvasFromImage(
        image,
        startX,
        0,
        w - startX,
        h,
        true
      )

    files.push(
      await canvasToFile(
        canvas,
        'cccd-right.png'
      )
    )
  }

  /*
   * 5. Phần trên.
   */
  {
    const canvas =
      createCanvasFromImage(
        image,
        0,
        0,
        w,
        Math.ceil(
          h * 0.65
        ),
        true
      )

    files.push(
      await canvasToFile(
        canvas,
        'cccd-top.png'
      )
    )
  }

  /*
   * 6. Phần dưới.
   */
  {
    const startY =
      Math.floor(
        h * 0.35
      )

    const canvas =
      createCanvasFromImage(
        image,
        0,
        startY,
        w,
        h - startY,
        true
      )

    files.push(
      await canvasToFile(
        canvas,
        'cccd-bottom.png'
      )
    )
  }

  /*
   * 7-10.
   * Chia ảnh thành 4 vùng.
   *
   * Có overlap để QR nằm giữa
   * hai vùng vẫn không bị cắt mất.
   */

  const cropW =
    Math.ceil(
      w * 0.65
    )

  const cropH =
    Math.ceil(
      h * 0.65
    )

  const positions = [
    {
      x: 0,
      y: 0,
      name:
        'top-left'
    },

    {
      x:
        Math.max(
          0,
          w - cropW
        ),
      y: 0,
      name:
        'top-right'
    },

    {
      x: 0,
      y:
        Math.max(
          0,
          h - cropH
        ),
      name:
        'bottom-left'
    },

    {
      x:
        Math.max(
          0,
          w - cropW
        ),

      y:
        Math.max(
          0,
          h - cropH
        ),

      name:
        'bottom-right'
    }
  ]

  for (
    const position
    of positions
  ) {
    const canvas =
      createCanvasFromImage(
        image,

        position.x,
        position.y,

        Math.min(
          cropW,
          w - position.x
        ),

        Math.min(
          cropH,
          h - position.y
        ),

        true
      )

    files.push(
      await canvasToFile(
        canvas,
        `cccd-${position.name}.png`
      )
    )
  }

  return files
}

/* ================================
   QUÉT FILE
================================ */

async function tryScanFile(
  html5QrCode,
  file
) {
  try {
    /*
     * false:
     * không cần render ảnh vào #reader.
     */
    const decodedText =
      await html5QrCode.scanFile(
        file,
        false
      )

    return decodedText

  } catch {
    return null
  }
}

/* ================================
   QUÉT ẢNH CCCD
================================ */

function chooseImage() {
  if (
    processingImage.value ||
    starting.value
  ) {
    return
  }

  fileInput.value?.click()
}

async function scanImage(event) {
  const input =
    event.target

  const file =
    input.files?.[0]

  if (!file) {
    return
  }

  /*
 * Hiển thị ngay ảnh người dùng vừa chọn.
 */
clearPreview()

previewUrl.value =
  URL.createObjectURL(file)

  error.value = ''
  finished = false
  processingImage.value = true

  try {
    /*
     * Dừng camera trước.
     */
    await stopScanner()

    await nextTick()

    /*
     * Scanner riêng cho file.
     */
    const html5QrCode =
      new Html5Qrcode(
        readerId
      )

    scanner.value =
      html5QrCode

    let decodedText = null

    /*
     * =================================
     * LẦN 1:
     * thử ảnh gốc trước.
     * =================================
     */

    decodedText =
      await tryScanFile(
        html5QrCode,
        file
      )

    /*
     * =================================
     * Nếu ảnh gốc fail:
     * tạo các phiên bản xử lý.
     * =================================
     */

    if (!decodedText) {
      const variants =
        await createImageVariants(
          file
        )

      for (
        const variant
        of variants
      ) {
        decodedText =
          await tryScanFile(
            html5QrCode,
            variant
          )

        if (decodedText) {
          break
        }
      }
    }

    /*
     * Vẫn không đọc được.
     */
    if (!decodedText) {
      throw new Error(
        'QR_NOT_FOUND'
      )
    }

    /*
     * Đọc được QR.
     */
    const data =
      parseCccd(
        decodedText
      )

    finished = true

    try {
      await html5QrCode.clear()
    } catch {
      // Không cần xử lý.
    }

    scanner.value = null

    emit(
      'scanned',
      data
    )

  } catch (e) {
    console.error(
      'Lỗi quét ảnh CCCD:',
      e
    )

    if (
      e?.message ===
      'QR không đúng định dạng CCCD.'
    ) {
      error.value =
        'Đã đọc được mã QR nhưng QR này không phải dữ liệu CCCD.'

    } else {
      error.value =
        'Không tìm thấy mã QR CCCD trong ảnh. ' +
        'Hãy thử ảnh rõ hơn hoặc cắt gần vùng QR rồi tải lại.'
    }

  } finally {
    processingImage.value = false

    /*
     * Cho phép chọn lại chính file cũ.
     */
    input.value = ''
  }
}

/* ================================
   ĐÓNG
================================ */

async function close() {
  finished = true

  await stopScanner()

  clearPreview()

  emit('close')
}
onBeforeUnmount(() => {
  stopScanner()
  clearPreview()
})

nextTick(() => {
  startScanner()
})
</script>

<template>
  <Teleport to="body">

    <div
      class="cccd-mask"
      @click.self="close"
    >

      <section class="cccd-modal">

        <!-- HEADER -->

        <div class="cccd-head">

          <div>
            <h2>
              <i class="bi bi-upc-scan"></i>
              Quét mã QR CCCD
            </h2>

            <p>
              Đưa mã QR trên CCCD vào giữa
              khung camera hoặc tải ảnh CCCD lên.
            </p>
          </div>

          <button
            type="button"
            class="cccd-close"
            :disabled="processingImage"
            @click="close"
          >
            <i class="bi bi-x-lg"></i>
          </button>

        </div>

        <!-- CAMERA / PREVIEW -->

        <!-- CAMERA / PREVIEW -->

<div class="cccd-reader-wrap">

  <!-- ẢNH VỪA CHỌN -->
  <img
    v-if="previewUrl"
    :src="previewUrl"
    class="cccd-image-preview"
    alt="Ảnh CCCD đã chọn"
  />

  <!-- CAMERA -->
  <div
    :id="readerId"
    class="cccd-reader"
    :class="{
      'reader-hidden': previewUrl
    }"
  ></div>

  <!-- LOADING KHI ĐANG QUÉT ẢNH -->
  <div
    v-if="processingImage"
    class="cccd-scan-overlay"
  >
    <div class="cccd-scan-loading">

      <span
        class="spinner-border spinner-border-sm"
      ></span>

      <span>
        Đang nhận diện mã QR...
      </span>

    </div>
  </div>

</div>

        <!-- ĐANG MỞ CAMERA -->

        <div
          v-if="starting"
          class="cccd-status"
        >
          <span
            class="spinner-border spinner-border-sm"
          ></span>

          Đang mở camera...
        </div>

        <!-- ĐANG XỬ LÝ ẢNH -->

        <div
          v-if="processingImage"
          class="cccd-processing"
        >
          <span
            class="spinner-border spinner-border-sm"
          ></span>

          <div>
            <strong>
              Đang tìm mã QR...
            </strong>

            <p>
              Hệ thống đang thử ảnh gốc
              và các vùng ảnh đã xử lý.
            </p>
          </div>
        </div>

        <!-- ERROR -->

        <div
          v-if="error"
          class="cccd-error"
        >
          <i
            class="bi bi-exclamation-triangle"
          ></i>

          <span>
            {{ error }}
          </span>
        </div>

        <!-- HƯỚNG DẪN -->

        <div class="cccd-note">

          <i
            class="bi bi-info-circle"
          ></i>

          <div>
            <strong>
              Nếu tải ảnh từ máy tính
            </strong>

            <p>
              Nên dùng ảnh gốc rõ nét.
              Nếu ảnh chụp toàn bộ CCCD
              vẫn không đọc được,
              hãy cắt gần vùng QR rồi tải lại.
            </p>
          </div>

        </div>

        <!-- INPUT FILE -->

        <input
          ref="fileInput"
          type="file"
          accept="
            image/jpeg,
            image/jpg,
            image/png,
            image/webp
          "
          hidden
          @change="scanImage"
        />

        <!-- BUTTON -->

        <div class="cccd-actions">

          <button
            type="button"
            class="ss-btn primary"
            :disabled="
              processingImage ||
              starting
            "
            @click="chooseImage"
          >
            <span
              v-if="processingImage"
              class="
                spinner-border
                spinner-border-sm
              "
            ></span>

            <i
              v-else
              class="bi bi-image"
            ></i>

            {{
              processingImage
                ? 'Đang quét ảnh...'
                : 'Chọn ảnh QR CCCD'
            }}
          </button>

          <button
            v-if="!scanning"
            type="button"
            class="ss-btn"
            :disabled="
              starting ||
              processingImage
            "
            @click="startScanner"
          >
            <i
              class="bi bi-camera"
            ></i>

            Mở lại camera
          </button>

          <button
            type="button"
            class="ss-btn"
            :disabled="processingImage"
            @click="close"
          >
            Hủy
          </button>

        </div>

      </section>

    </div>

  </Teleport>
</template>

<style scoped>
.cccd-mask {
  position: fixed;
  inset: 0;
  z-index: 99999;

  display: flex;
  align-items: center;
  justify-content: center;

  padding: 20px;

  background:
    rgba(15, 23, 42, 0.58);
}

.cccd-modal {
  width: min(580px, 100%);

  max-height: 95vh;
  overflow-y: auto;

  padding: 22px;

  background: #ffffff;

  border-radius: 18px;

  box-shadow:
    0 25px 70px
    rgba(15, 23, 42, 0.28);
}

.cccd-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;

  gap: 20px;

  margin-bottom: 18px;
}

.cccd-head h2 {
  display: flex;
  align-items: center;

  gap: 8px;

  margin: 0;

  font-size: 20px;
}

.cccd-head p {
  margin: 6px 0 0;

  color: #64748b;

  font-size: 13px;
}

.cccd-close {
  width: 36px;
  height: 36px;

  flex-shrink: 0;

  display: flex;
  align-items: center;
  justify-content: center;

  border: 0;
  border-radius: 10px;

  background: #f1f5f9;
  color: #475569;

  cursor: pointer;
}

.cccd-close:hover {
  background: #e2e8f0;
}

.cccd-close:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.cccd-reader-wrap {
  position: relative;

  width: 100%;
  height: 320px;

  overflow: hidden;

  border-radius: 14px;

  background: #0f172a;
}

.cccd-reader {
  width: 100%;
  height: 100%;
  min-height: 320px;

  overflow: hidden;

  border-radius: 14px;

  background: #0f172a;
}

/*
 * Khi đã chọn ảnh thì ẩn phần camera,
 * nhưng vẫn giữ element #reader tồn tại
 * để html5-qrcode tiếp tục scanFile().
 */
.cccd-reader.reader-hidden {
  position: absolute;

  inset: 0;

  opacity: 0;

  pointer-events: none;
}

/*
 * Ảnh CCCD người dùng vừa chọn.
 */
.cccd-image-preview {
  position: absolute;

  inset: 0;

  z-index: 2;

  width: 100%;
  height: 100%;

  object-fit: contain;

  padding: 10px;

  background: #0f172a;
}

/*
 * Lớp loading nằm trên ảnh.
 */
.cccd-scan-overlay {
  position: absolute;

  inset: 0;

  z-index: 3;

  display: flex;
  align-items: flex-end;
  justify-content: center;

  padding: 18px;

  pointer-events: none;
}

.cccd-scan-loading {
  display: flex;
  align-items: center;

  gap: 8px;

  padding: 9px 14px;

  border-radius: 999px;

  background: rgba(15, 23, 42, 0.85);

  color: #ffffff;

  font-size: 13px;

  box-shadow:
    0 5px 20px
    rgba(0, 0, 0, 0.25);
}

.cccd-status {
  display: flex;
  align-items: center;

  gap: 8px;

  margin-top: 12px;

  color: #64748b;

  font-size: 13px;
}

.cccd-processing {
  display: flex;
  align-items: flex-start;

  gap: 10px;

  margin-top: 14px;

  padding: 12px 14px;

  border-radius: 10px;

  color: #1e40af;
  background: #eff6ff;

  font-size: 13px;
}

.cccd-processing strong {
  display: block;

  color: #1e3a8a;
}

.cccd-processing p {
  margin: 3px 0 0;

  line-height: 1.45;
}

.cccd-error {
  display: flex;
  align-items: flex-start;

  gap: 8px;

  margin-top: 14px;

  padding: 12px 14px;

  border-radius: 10px;

  color: #b91c1c;
  background: #fef2f2;

  font-size: 13px;
}

.cccd-note {
  display: flex;
  align-items: flex-start;

  gap: 10px;

  margin-top: 14px;

  padding: 12px 14px;

  border-radius: 10px;

  color: #475569;
  background: #f8fafc;

  font-size: 13px;
}

.cccd-note strong {
  color: #334155;
}

.cccd-note p {
  margin: 4px 0 0;

  line-height: 1.5;
}

.cccd-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;

  gap: 8px;

  margin-top: 18px;
}

.cccd-actions button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .cccd-mask {
    padding: 10px;
  }

  .cccd-modal {
    padding: 16px;
  }

  .cccd-actions {
    flex-direction: column;
  }

  .cccd-actions button {
    width: 100%;
  }
}
</style>