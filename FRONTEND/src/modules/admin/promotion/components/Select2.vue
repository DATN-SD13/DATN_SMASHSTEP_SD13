<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number, Boolean, null, undefined],
    default: undefined
  },
  options: {
    type: Array,
    default: () => []
  },
  placeholder: {
    type: String,
    default: 'Chọn một mục...'
  },
  searchable: {
    type: Boolean,
    default: true
  },
  clearable: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  searchPlaceholder: {
    type: String,
    default: 'Tìm kiếm...'
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const isOpen = ref(false)
const searchKeyword = ref('')
const rootRef = ref(null)
const searchInputRef = ref(null)

const normalizedOptions = computed(() => {
  return props.options.map(opt => {
    if (typeof opt === 'object' && opt !== null) {
      return {
        value: opt.value !== undefined ? opt.value : opt.id,
        label: opt.label !== undefined ? opt.label : (opt.name || opt.text || String(opt.value)),
        disabled: Boolean(opt.disabled)
      }
    }
    return {
      value: opt,
      label: String(opt),
      disabled: false
    }
  })
})

const filteredOptions = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return normalizedOptions.value
  return normalizedOptions.value.filter(opt =>
    String(opt.label).toLowerCase().includes(kw)
  )
})

const selectedOption = computed(() => {
  return normalizedOptions.value.find(opt => opt.value === props.modelValue)
})

const displayLabel = computed(() => {
  if (selectedOption.value) {
    return selectedOption.value.label
  }
  return ''
})

function toggleDropdown() {
  if (props.disabled) return
  if (isOpen.value) {
    closeDropdown()
  } else {
    openDropdown()
  }
}

function openDropdown() {
  if (props.disabled) return
  isOpen.value = true
  searchKeyword.value = ''
  if (props.searchable) {
    nextTick(() => {
      searchInputRef.value?.focus()
    })
  }
}

function closeDropdown() {
  isOpen.value = false
  searchKeyword.value = ''
}

function selectOption(option) {
  if (option.disabled) return
  emit('update:modelValue', option.value)
  emit('change', option.value)
  closeDropdown()
}

function handleClear(e) {
  e.stopPropagation()
  emit('update:modelValue', undefined)
  emit('change', undefined)
}

function handleClickOutside(event) {
  if (rootRef.value && !rootRef.value.contains(event.target)) {
    closeDropdown()
  }
}

function handleKeydown(event) {
  if (event.key === 'Escape') {
    closeDropdown()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div
    ref="rootRef"
    class="ss-select2-container"
    :class="{
      'is-open': isOpen,
      'is-disabled': disabled
    }"
  >
    <!-- Display Box -->
    <div
      class="ss-select2-selection"
      tabindex="0"
      :aria-expanded="isOpen"
      @click="toggleDropdown"
      @keydown.enter.prevent="toggleDropdown"
      @keydown.space.prevent="toggleDropdown"
    >
      <span v-if="displayLabel" class="ss-select2-rendered">
        {{ displayLabel }}
      </span>
      <span v-else class="ss-select2-placeholder">
        {{ placeholder }}
      </span>

      <span class="ss-select2-actions">
        <i
          v-if="clearable && modelValue !== undefined && modelValue !== null && modelValue !== '' && modelValue !== 'all'"
          class="bi bi-x-circle-fill ss-select2-clear"
          title="Xóa lựa chọn"
          @click="handleClear"
        ></i>
        <i class="bi bi-chevron-down ss-select2-arrow"></i>
      </span>
    </div>

    <!-- Dropdown Menu -->
    <div v-if="isOpen" class="ss-select2-dropdown">
      <!-- Search Input -->
      <div v-if="searchable" class="ss-select2-search">
        <i class="bi bi-search ss-select2-search-icon"></i>
        <input
          ref="searchInputRef"
          v-model="searchKeyword"
          type="text"
          class="ss-select2-search-input"
          :placeholder="searchPlaceholder"
          @click.stop
        />
      </div>

      <!-- Options List -->
      <ul class="ss-select2-results" role="listbox">
        <li
          v-for="opt in filteredOptions"
          :key="String(opt.value)"
          class="ss-select2-result"
          :class="{
            'is-selected': opt.value === modelValue,
            'is-disabled': opt.disabled
          }"
          role="option"
          :aria-selected="opt.value === modelValue"
          @click.stop="selectOption(opt)"
        >
          <span class="ss-select2-result-text">{{ opt.label }}</span>
          <i v-if="opt.value === modelValue" class="bi bi-check2 ss-select2-check"></i>
        </li>
        <li v-if="filteredOptions.length === 0" class="ss-select2-no-results">
          <i class="bi bi-inbox me-1"></i> Không tìm thấy kết quả
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.ss-select2-container {
  position: relative;
  display: inline-block;
  width: 100%;
  font-family: inherit;
  font-size: 14px;
}

.ss-select2-selection {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 40px;
  padding: 6px 12px;
  background-color: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s ease;
  outline: none;
}

.ss-select2-selection:hover {
  border-color: #94a3b8;
}

.ss-select2-container.is-open .ss-select2-selection {
  border-color: #079fc9;
  box-shadow: 0 0 0 3px rgba(7, 159, 201, 0.15);
}

.ss-select2-container.is-disabled .ss-select2-selection {
  background-color: #f1f5f9;
  border-color: #e2e8f0;
  cursor: not-allowed;
  opacity: 0.7;
}

.ss-select2-rendered {
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.ss-select2-placeholder {
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.ss-select2-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: 8px;
  color: #64748b;
}

.ss-select2-clear {
  font-size: 14px;
  color: #94a3b8;
  transition: color 0.15s ease;
}

.ss-select2-clear:hover {
  color: #ef4444;
}

.ss-select2-arrow {
  font-size: 12px;
  transition: transform 0.2s ease;
}

.ss-select2-container.is-open .ss-select2-arrow {
  transform: rotate(180deg);
  color: #079fc9;
}

.ss-select2-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  z-index: 1050;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  animation: select2DropdownFadeIn 0.15s ease-out;
}

@keyframes select2DropdownFadeIn {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.ss-select2-search {
  position: relative;
  padding: 8px;
  border-bottom: 1px solid #f1f5f9;
  background-color: #f8fafc;
}

.ss-select2-search-icon {
  position: absolute;
  left: 18px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  font-size: 13px;
}

.ss-select2-search-input {
  width: 100%;
  padding: 6px 10px 6px 30px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 13px;
  outline: none;
  background-color: #ffffff;
  transition: border-color 0.15s ease;
}

.ss-select2-search-input:focus {
  border-color: #079fc9;
  box-shadow: 0 0 0 2px rgba(7, 159, 201, 0.1);
}

.ss-select2-results {
  list-style: none;
  margin: 0;
  padding: 4px;
  max-height: 220px;
  overflow-y: auto;
}

.ss-select2-results::-webkit-scrollbar {
  width: 6px;
}

.ss-select2-results::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

.ss-select2-result {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  color: #334155;
  font-size: 13.5px;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.ss-select2-result:hover {
  background-color: #f0faff;
  color: #079fc9;
}

.ss-select2-result.is-selected {
  background-color: #e0f7fc;
  color: #0369a1;
  font-weight: 600;
}

.ss-select2-result.is-disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}

.ss-select2-check {
  color: #079fc9;
  font-size: 16px;
}

.ss-select2-no-results {
  padding: 14px 12px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
}
</style>
