<script setup>
import {useAttrs} from 'vue'
import Select2Control from '../../product/components/Select2Control.vue'
defineOptions({inheritAttrs:false})
defineProps({modelValue:{default:''},disabled:Boolean})
const emit=defineEmits(['update:modelValue','change'])
const attrs=useAttrs()
</script>
<template>
 <span class="statistics-select" :data-label="attrs['aria-label']">
  <Select2Control v-bind="attrs" :model-value="modelValue" :disabled="disabled" :allow-clear="false" :search="true" dropdown-class="statistics-select-dropdown" @update:model-value="emit('update:modelValue',$event)" @change="emit('change',$event)"><slot/></Select2Control>
 </span>
</template>
<style scoped>
.statistics-select{display:inline-block;min-width:145px;max-width:100%;width:auto;vertical-align:middle;font-size:12px;text-align:left}.statistics-select :deep(.select2-container){display:block;width:100%!important;min-width:0}.statistics-select :deep(.select2-selection--single){min-height:40px;height:40px;border:1px solid #d3e3ee;border-radius:8px;background:#fff;display:flex;align-items:center}.statistics-select :deep(.select2-selection__rendered){color:#4e7188;line-height:38px;padding-left:12px;padding-right:28px;font-size:12px}.statistics-select :deep(.select2-selection__arrow){height:38px;right:6px}.statistics-select :deep(.select2-container--open .select2-selection--single),.statistics-select :deep(.select2-container--focus .select2-selection--single){border-color:#168fce;box-shadow:0 0 0 3px #168fce10}.statistics-select :deep(.select2-container--disabled .select2-selection--single){background:#f4f7fa;opacity:.65}
</style>
<style>
.statistics-select-dropdown{z-index:6000!important;font-size:12px;color:#4e7188}.statistics-select-dropdown .select2-dropdown{border-color:#d3e3ee!important;border-radius:8px!important;overflow:hidden}.statistics-select-dropdown .select2-search__field{border:1px solid #d3e3ee!important;border-radius:6px;padding:8px!important;outline-color:#168fce}.statistics-select-dropdown .select2-results__option{padding:9px 12px}.statistics-select-dropdown .select2-results__option--highlighted{background:#168fce!important;color:white!important}
</style>
