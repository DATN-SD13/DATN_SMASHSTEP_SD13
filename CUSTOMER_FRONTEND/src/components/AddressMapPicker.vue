<script setup>
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { commerceService, commerceError } from '../services/commerceService'

const props = defineProps({ address: String, point: Object, disabled: Boolean })
const emit = defineEmits(['selected'])
const open = ref(false)
const canvas = ref(null), query = ref(props.address || ''), rows = ref([]), selected = ref(null)
const busy = ref(false), locating = ref(false), searched = ref(false), expanded = ref(false)
const error = ref(''), mapError = ref(''), tilesReady = ref(false), applied = ref(false)
const locked = computed(() => props.disabled || busy.value || locating.value)
const searchQuery = computed(() => query.value.normalize('NFC').replace(/\s+/gu, ' ').trim().toLowerCase())
let map, tiles, marker, accuracyCircle, resizeObserver, loadingTimer, resizeFrame
let version = 0, disposed = false
const icon = L.divIcon({ className: 'delivery-pin', html: '<span aria-hidden="true">●</span>', iconSize: [30, 38], iconAnchor: [15, 34] })
const validPoint = p => p && Number.isFinite(p.latitude) && Number.isFinite(p.longitude)

async function show() {
  if (props.disabled || open.value) return
  open.value = true
  await nextTick()
  if (!disposed && open.value) initialize()
}
function hide() {
  open.value = false; version++; busy.value = false; locating.value = false
  clearTimeout(loadingTimer); cancelAnimationFrame(resizeFrame)
  resizeObserver?.disconnect(); resizeObserver = null
  map?.remove(); map = null; tiles = null; marker = null; accuracyCircle = null
  tilesReady.value = false; mapError.value = ''; error.value = ''; rows.value = []; searched.value = false
  expanded.value = false
}
function fitMap() {
  cancelAnimationFrame(resizeFrame)
  resizeFrame = requestAnimationFrame(() => map?.invalidateSize({ pan: false }))
}
function markerAt(point) {
  if (!map || !validPoint(point)) return
  const coordinates = [point.latitude, point.longitude]
  if (!marker) {
    marker = L.marker(coordinates, { icon, draggable: !props.disabled, title: 'Kéo ghim đến địa chỉ nhận hàng', keyboard: true }).addTo(map)
    marker.on('dragend', () => { const p = marker.getLatLng(); chooseCoordinates(p.lat, p.lng) })
  } else marker.setLatLng(coordinates)
  accuracyCircle?.remove(); accuracyCircle = null
  if (point.accuracy) accuracyCircle = L.circle(coordinates, { radius: point.accuracy, color: '#2879e3', fillOpacity: 0.1, weight: 1 }).addTo(map)
  map.setView(coordinates, Math.max(map.getZoom(), 16))
}
function restorePoint() {
  version++; busy.value = false; locating.value = false; rows.value = []; searched.value = false; error.value = ''
  query.value = props.address || ''
  selected.value = validPoint(props.point) ? { ...props.point, label: props.address || 'Vị trí nhận hàng đã lưu', street: '', ward: '', province: '' } : null
  applied.value = !!selected.value
  if (selected.value) markerAt(selected.value)
  else { marker?.remove(); marker = null; accuracyCircle?.remove(); accuracyCircle = null }
}
function initialize() {
  if (disposed || map || !canvas.value) return
  try {
    map = L.map(canvas.value, { scrollWheelZoom: false, zoomControl: false }).setView(
      validPoint(props.point) ? [props.point.latitude, props.point.longitude] : [21.0285, 105.8542], validPoint(props.point) ? 16 : 12)
    L.control.zoom({ zoomInTitle: 'Phóng to bản đồ', zoomOutTitle: 'Thu nhỏ bản đồ' }).addTo(map)
    tiles = L.tileLayer(import.meta.env.VITE_MAP_TILE_URL || 'https://{s}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: import.meta.env.VITE_MAP_TILE_ATTRIBUTION || '© <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap</a> · <a href="https://www.hotosm.org/" target="_blank" rel="noopener noreferrer">HOT</a> · <a href="https://www.openstreetmap.fr/fonds-de-carte/" target="_blank" rel="noopener noreferrer">OSM France</a>'
    })
    tiles.on('tileload', () => { if (!disposed) { tilesReady.value = true; mapError.value = ''; clearTimeout(loadingTimer) } })
    tiles.on('tileerror', () => { if (!disposed && !tilesReady.value) mapError.value = 'Chưa tải được bản đồ. Kiểm tra kết nối mạng rồi bấm Tải lại bản đồ.' })
    tiles.addTo(map)
    map.on('click', e => { if (!props.disabled) chooseCoordinates(e.latlng.lat, e.latlng.lng) })
    resizeObserver = new ResizeObserver(fitMap)
    resizeObserver.observe(canvas.value)
    loadingTimer = setTimeout(() => { if (!disposed && !tilesReady.value) mapError.value = 'Bản đồ đang tải chậm. Bạn có thể tải lại hoặc tiếp tục nhập địa chỉ bằng tay.' }, 12000)
    restorePoint(); fitMap()
  } catch {
    mapError.value = 'Chưa mở được bản đồ. Bạn vẫn có thể nhập địa chỉ ở các ô bên dưới.'
    map?.remove(); map = null
  }
}
function retryTiles() {
  mapError.value = ''; tilesReady.value = false
  if (!map) { initialize(); return }
  tiles?.redraw(); fitMap()
  clearTimeout(loadingTimer)
  loadingTimer = setTimeout(() => { if (!disposed && !tilesReady.value) mapError.value = 'Chưa tải được bản đồ. Vui lòng kiểm tra kết nối mạng.' }, 12000)
}
function queryChanged() {
  version++; busy.value = false; locating.value = false; searched.value = false; rows.value = []; error.value = ''
}
async function search() {
  if (locked.value) return
  const value = searchQuery.value
  if (query.value.length > 200 || /[\u0000-\u001f\u007f-\u009f\u2028\u2029<>]/u.test(query.value)) {
    error.value = 'Địa chỉ tìm kiếm tối đa 200 ký tự, không chứa ký tự điều khiển hoặc < >.'; return
  }
  if (value.length < 3) { error.value = 'Nhập ít nhất 3 ký tự để tìm địa chỉ.'; return }
  const current = ++version; busy.value = true; error.value = ''; rows.value = []; searched.value = false
  try {
    const places = await commerceService.mapSearch(value)
    if (!disposed && current === version) { rows.value = places; searched.value = true }
  } catch (e) { if (!disposed && current === version) error.value = commerceError(e) }
  finally { if (!disposed && current === version) busy.value = false }
}
function choosePlace(place) {
  if (props.disabled) return
  version++; busy.value = false; locating.value = false; selected.value = { ...place, accuracy: null }
  applied.value = false; error.value = ''; rows.value = []; searched.value = false; query.value = place.label
  markerAt(selected.value)
}
async function chooseCoordinates(latitude, longitude, accuracy = null) {
  if (props.disabled) return
  const current = ++version; busy.value = true; locating.value = false; error.value = ''; rows.value = []; searched.value = false; applied.value = false
  selected.value = { latitude, longitude, accuracy, street: '', ward: '', province: '', label: 'Vị trí vừa chọn trên bản đồ' }
  markerAt(selected.value)
  try {
    const places = await commerceService.mapReverse(latitude, longitude)
    if (disposed || current !== version) return
    if (places[0]) { selected.value = { ...places[0], latitude, longitude, accuracy }; query.value = places[0].label }
    else error.value = 'Chưa có tên đường tại điểm này. Vẫn có thể dùng vị trí này và nhập số nhà, đường bằng tay.'
  } catch (e) { if (!disposed && current === version) error.value = commerceError(e) + ' Bạn vẫn có thể dùng ghim và nhập địa chỉ bằng tay.' }
  finally { if (!disposed && current === version) busy.value = false }
}
function locate() {
  if (locked.value) return
  if (!navigator.geolocation) { error.value = 'Thiết bị không hỗ trợ định vị. Hãy tìm hoặc chọn điểm trên bản đồ.'; return }
  const current = ++version; locating.value = true; error.value = ''
  navigator.geolocation.getCurrentPosition(position => {
    if (disposed || current !== version) return
    locating.value = false
    chooseCoordinates(position.coords.latitude, position.coords.longitude, position.coords.accuracy <= 100000 ? position.coords.accuracy : null)
  }, e => {
    if (disposed || current !== version) return
    locating.value = false
    error.value = e.code === 1 ? 'Bạn chưa cho phép truy cập vị trí. Có thể chọn địa chỉ bằng ô tìm kiếm hoặc bấm trên bản đồ.' : 'Chưa lấy được vị trí. Hãy thử lại hoặc chọn điểm trên bản đồ.'
  }, { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 })
}
function apply() {
  if (!selected.value || locked.value || applied.value) return
  applied.value = true; emit('selected', { ...selected.value })
  hide()
}
watch(() => props.point, restorePoint, { deep: true })
watch(() => props.disabled, value => { if (marker) { if (value) marker.dragging.disable(); else marker.dragging.enable() } })
onBeforeUnmount(() => {
  disposed = true; hide()
})
</script>

<template>
  <div class="address-map-control">
    <button type="button" class="button button-primary map-toggle" :disabled="disabled" :aria-expanded="open" @click="open ? hide() : show()"><span class="map-toggle-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="m3 6 6-3 6 3 6-3v15l-6 3-6-3-6 3V6Z"/><path d="M9 3v15M15 6v15"/></svg></span><span class="map-toggle-text">{{ open ? 'Ẩn bản đồ' : point ? 'Đổi vị trí trên bản đồ' : 'Chọn địa chỉ trên bản đồ' }}</span><svg class="map-toggle-chevron" :class="{ rotated: open }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="m6 9 6 6 6-6"/></svg></button>
  <section v-if="open" class="address-map-picker" aria-label="Chọn địa chỉ trên bản đồ">
    <header class="map-heading"><div><strong>Chọn địa chỉ trên bản đồ</strong><p>Tìm địa điểm hoặc bấm vào bản đồ để ghim nơi nhận hàng.</p></div><span class="map-label">Bản đồ trực tiếp</span></header>
    <div class="map-search" role="search" aria-label="Tìm địa điểm giao hàng">
      <input v-model="query" type="search" aria-label="Tìm địa chỉ trên bản đồ" maxlength="200" autocomplete="off" autocapitalize="off" :spellcheck="false" placeholder="Ví dụ: hoài đức, hà nội" :disabled="disabled" @input="queryChanged" @keydown.enter.stop.prevent="search">
      <button type="button" class="button button-primary" :disabled="locked || searchQuery.length < 3" @click="search">{{ busy ? 'Đang tìm…' : 'Tìm địa chỉ' }}</button>
    </div>
    <ul v-if="rows.length" class="map-results" aria-label="Kết quả tìm địa chỉ"><li v-for="(place, index) in rows" :key="index"><button type="button" :disabled="disabled" @click="choosePlace(place)"><span class="result-pin" aria-hidden="true">⌖</span><span>{{ place.label }}<small>Chọn địa điểm này trên bản đồ</small></span><span aria-hidden="true">→</span></button></li></ul>
    <p v-else-if="searched" class="map-message" role="status">Không tìm thấy địa điểm phù hợp. Thử thêm tên tỉnh / thành phố hoặc chọn điểm trên bản đồ.</p>
    <div class="map-tools"><button type="button" class="text-button" :disabled="locked" @click="locate">⌖ {{ locating ? 'Đang lấy vị trí…' : 'Dùng vị trí hiện tại' }}</button><button type="button" class="text-button" :disabled="disabled" :aria-expanded="expanded" @click="expanded = !expanded">{{ expanded ? 'Thu gọn bản đồ' : 'Mở rộng bản đồ' }}</button></div>
    <div class="map-stage" :class="{ expanded, 'map-disabled': disabled }">
      <div ref="canvas" class="address-map-canvas" role="region" aria-label="Bản đồ tương tác: bấm để ghim địa chỉ nhận hàng"></div>
      <span v-if="!tilesReady && !mapError" class="map-loading" role="status">Đang tải bản đồ…</span>
      <span v-else class="map-tip">Bấm trên bản đồ hoặc kéo ghim để chọn vị trí</span>
    </div>
    <p v-if="mapError" class="map-message map-warning" role="alert">{{ mapError }} <button type="button" class="text-button" :disabled="disabled" @click="retryTiles">Tải lại bản đồ</button></p>
    <p v-if="error" class="map-message map-warning" role="alert">{{ error }}</p>
    <div v-if="selected" class="map-selected" aria-live="polite"><div><strong>{{ selected.label }}</strong><small>{{ selected.latitude.toFixed(6) }}, {{ selected.longitude.toFixed(6) }}<template v-if="selected.accuracy"> · Sai số khoảng {{ Math.round(selected.accuracy) }} m</template></small><span>{{ applied ? 'Đã dùng vị trí này. Kiểm tra lại số nhà, tỉnh và phường ở dưới.' : 'Bấm “Dùng địa chỉ này” để điền vào thông tin nhận hàng.' }}</span></div><button type="button" class="button" :class="applied ? 'button-soft' : 'button-primary'" :disabled="locked || applied" @click="apply">{{ busy ? 'Đang tra địa chỉ…' : applied ? '✓ Đã chọn' : 'Dùng địa chỉ này' }}</button></div>
    <p class="map-privacy">Bạn vẫn có thể nhập địa chỉ bằng tay. Vị trí hiện tại chỉ được lấy khi bạn cho phép. Bản đồ: OpenStreetMap · Tìm địa chỉ: Photon.</p>
  </section>
  </div>
</template>

<style scoped>
.address-map-control{grid-column:1/-1;min-width:0}.map-toggle{width:100%;min-height:58px;justify-content:flex-start;gap:12px;padding:11px 16px;border:1px solid #1764c9;border-radius:11px;background:#246fd4;color:#fff;box-shadow:0 5px 13px #246fd428;font-size:14px;font-weight:750}.map-toggle:hover{background:#185db8}.map-toggle:disabled{opacity:.65}.map-toggle-icon{display:grid;place-items:center;width:36px;height:36px;flex-shrink:0;border-radius:9px;background:#ffffff26}.map-toggle-icon svg{width:23px;height:23px}.map-toggle-text{flex:1;text-align:left}.map-toggle-chevron{width:19px;height:19px;flex-shrink:0;transition:transform .18s}.map-toggle-chevron.rotated{transform:rotate(180deg)}.address-map-picker{margin-top:12px}
.address-map-picker{grid-column:1/-1;min-width:0;padding:18px;border:1px solid #cbdff5;border-radius:14px;background:#f8fbff}
.map-heading{display:flex;align-items:flex-start;justify-content:space-between;gap:12px}.map-heading strong{font-size:14px;color:#143456}.map-heading p{font-size:12px;color:#64748b;margin-top:4px}.map-label{border:1px solid #d0e6f8;background:#edf7ff;color:#2672ad;border-radius:20px;padding:3px 9px;font-size:10px;white-space:nowrap}
.map-search{display:flex;gap:8px;margin:14px 0 10px}.map-search input{min-width:0;flex:1;background:white;border:1px solid #cedcea;border-radius:8px;padding:11px 12px}.map-search .button{padding:10px 16px;white-space:nowrap}
.map-tools{display:flex;justify-content:space-between;gap:12px;margin:12px 0}.map-tools .text-button{font-size:12px;min-height:30px}
.map-stage{position:relative;border-radius:10px;overflow:hidden;border:1px solid #cbd9e6;background:#e9eef1;isolation:isolate}.address-map-canvas{height:350px;width:100%;z-index:0}.expanded .address-map-canvas{height:520px}.map-disabled .address-map-canvas{pointer-events:none;opacity:.7}
.map-tip,.map-loading{position:absolute;bottom:24px;left:50%;transform:translateX(-50%);max-width:calc(100% - 30px);width:max-content;border-radius:20px;background:#ffffffed;color:#36526c;padding:6px 13px;font-size:11px;box-shadow:0 2px 8px #10243a1a;pointer-events:none;text-align:center}
.map-results{list-style:none;margin:0;padding:0;max-height:220px;overflow:auto;border:1px solid #d6e4ef;border-radius:9px;background:white}.map-results button{display:flex;align-items:center;gap:12px;width:100%;border:0;border-bottom:1px solid #edf2f7;background:white;text-align:left;padding:12px;cursor:pointer;color:#17365f;font-size:12px}.map-results button:hover,.map-results button:focus-visible{background:#edf7ff}.map-results button>span:nth-child(2){flex:1}.map-results small{display:block;margin-top:4px;color:#72859a;font-size:10px}.result-pin{display:grid;place-items:center;width:30px;height:30px;flex-shrink:0;border-radius:8px;background:#e9f5ff;font-size:20px;color:#2678c9}
.map-selected{display:flex;align-items:center;justify-content:space-between;gap:12px;background:#eaf5ff;padding:14px;margin-top:12px;border:1px solid #d2e8fa;border-radius:9px}.map-selected>div{display:grid;gap:5px;min-width:0}.map-selected strong{font-size:12px;overflow-wrap:anywhere}.map-selected small,.map-selected span{font-size:11px;color:#526b82}.map-selected .button{flex-shrink:0;font-size:12px;padding:10px 14px}.map-selected .button-soft:disabled{opacity:1}
.map-message{font-size:12px;line-height:1.6;padding:10px 12px;margin:10px 0;background:#edf5fc;border-radius:8px}.map-warning{background:#fff7e9;color:#865a20}.map-message .text-button{margin-left:5px}.map-privacy{font-size:10px;color:#718096;line-height:1.7;margin-top:12px}
:deep(.delivery-pin){background:none;border:0}:deep(.delivery-pin span){display:block;background:#2879e3;color:white;border:3px solid white;border-radius:50% 50% 50% 0;transform:rotate(-45deg);width:30px;height:30px;text-align:center;box-shadow:0 2px 6px #0005}:deep(.leaflet-control-attribution){font-size:9px}:deep(img.leaflet-tile){max-width:none!important;max-height:none!important}
@media(max-width:600px){.address-map-picker{padding:12px}.map-heading{display:block}.map-label{display:none}.map-search{flex-wrap:wrap}.map-search input{flex-basis:100%}.map-search .button{width:100%}.address-map-canvas{height:300px}.expanded .address-map-canvas{height:440px}.map-selected{flex-direction:column;align-items:stretch}.map-selected .button{width:100%}.map-tip{font-size:10px}.map-tools{gap:8px}.map-tools .text-button{font-size:11px}}
</style>
