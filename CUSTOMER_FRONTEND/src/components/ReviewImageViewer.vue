<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
const props=defineProps({images:{type:Array,required:true},initial:{type:Number,default:0}})
const emit=defineEmits(['close'])
const current=ref(props.initial), panel=ref(null), failed=ref(false)
let focus, overflow
function move(delta){current.value=(current.value+delta+props.images.length)%props.images.length;failed.value=false}
function keys(e){
  if(e.key==='Escape'){e.stopPropagation();emit('close')}
  if(e.key==='ArrowLeft')move(-1)
  if(e.key==='ArrowRight')move(1)
  if(e.key==='Tab'){
    const controls=[...panel.value.querySelectorAll('button')],first=controls[0],last=controls.at(-1)
    if(e.shiftKey && document.activeElement===first){e.preventDefault();last.focus()}
    else if(!e.shiftKey && document.activeElement===last){e.preventDefault();first.focus()}
  }
}
onMounted(()=>{focus=document.activeElement;overflow=document.body.style.overflow;document.body.style.overflow='hidden';panel.value?.querySelector('button')?.focus()})
onBeforeUnmount(()=>{document.body.style.overflow=overflow;focus?.focus()})
</script>
<template><Teleport to="body"><div class="review-viewer" @click.self="emit('close')"><section ref="panel" class="review-viewer-panel" role="dialog" aria-modal="true" aria-label="Ảnh đánh giá sản phẩm" @keydown="keys"><button class="viewer-close" type="button" aria-label="Đóng ảnh" @click="emit('close')">×</button><p v-if="failed" role="status">Ảnh hiện không tải được.</p><img v-else :key="current" :src="images[current]" :alt="'Ảnh đánh giá '+(current+1)" @error="failed=true"/><div class="viewer-controls"><button v-if="images.length>1" type="button" aria-label="Ảnh trước" @click="move(-1)">←</button><span>{{current+1}} / {{images.length}}</span><button v-if="images.length>1" type="button" aria-label="Ảnh tiếp theo" @click="move(1)">→</button></div></section></div></Teleport></template>
<style scoped>
.review-viewer{position:fixed;inset:0;z-index:10000;background:#071323e8;display:grid;place-items:center;padding:24px}.review-viewer-panel{position:relative;text-align:center;max-width:100%;color:white}.review-viewer-panel img{display:block;max-width:min(100%,1000px);max-height:78vh;object-fit:contain;margin:auto;border-radius:8px}.review-viewer button{background:#fff;color:#152a43;border:0;border-radius:50%;width:40px;height:40px;cursor:pointer;font-size:24px}.viewer-close{position:absolute;right:0;top:-46px}.viewer-controls{display:flex;align-items:center;justify-content:center;gap:24px;margin-top:16px}.review-viewer button:focus-visible{outline:3px solid #69aaff;outline-offset:3px}
</style>
