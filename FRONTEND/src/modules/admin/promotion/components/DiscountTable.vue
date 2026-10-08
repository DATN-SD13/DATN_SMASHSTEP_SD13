<script setup>
defineProps({
  rows: {
    type: Array,
    default: () => []
  },

  page: {
    type: Number,
    default: 1
  },

  pageSize: {
    type: Number,
    default: 5
  }
})

const emit = defineEmits([
  'view',
  'edit',
  'toggle',
  'delete'
])

function isActive(x) {
  return x.status === 1
}

function canToggle(x) {
  return x.timeStatus !== 3
}

function timeStatusClass(x) {
// ket thuc
  if (x.timeStatus === 3) {
    return 'danger'
  }
// tat hdong
  if (x.status === 0) {
    return 'danger'
  }
// sap dien ra
  if (x.timeStatus === 1) {
    return 'warning'
  }

// dang dien ra
  if (x.timeStatus === 2) {
    return 'success'
  }

  return ''
}
</script>

<template>
  <tbody>

    <tr
      v-for="(x, i) in rows"
      :key="x.id ?? x.code"
    >

      <!-- STT -->
      <td class="c">
        {{ (page - 1) * pageSize + i + 1 }}
      </td>

      <!-- ma -->
      <td>
        <span class="ss-code">
          {{ x.code }}
        </span>
      </td>

      <!-- ten -->
      <td>
        {{ x.name }}
      </td>

      <!-- hinh thuc -->
      <td>
        <span
          class="ss-pill"
          :class="{ warn: x.type === 'Cá nhân' }"
        >
          {{ x.type }}
        </span>
      </td>

      <!-- gia tri -->
      <td>
        {{ x.value }}
      </td>

      <!-- ngay bat dau -->
      <td class="nowrap">
        {{ x.start }}
      </td>

      <!-- date -->
      <td class="nowrap">
        {{ x.end }}
      </td>

      <!-- trang thai -->
      <td>
        <span
          class="ss-pill dot"
          :class="timeStatusClass(x)"
        >
          {{ x.timeStatusLabel }}
        </span>
      </td>

      <!-- Thao tác -->
      <td>
        <div class="ss-row-actions">

          <!-- detail -->
          <button
            type="button"
            class="ss-icon-btn"
            title="Xem chi tiết"
            @click="emit('view', x.id)"
          >
            <i class="bi bi-eye"></i>
          </button>

          <!-- edit 
          <button
            type="button"
            class="ss-icon-btn"
            title="Sửa"
            @click="emit('edit', x.id)"
          >
            <i class="bi bi-pencil"></i>
          </button> -->

          <!-- bat / tat hdong -->
          <button
            type="button"
            class="ss-icon-btn"
            :class="{ danger: isActive(x) }"
            :disabled="x.timeStatus === 3"
            :title="
              x.timeStatus === 3
                ? 'Phiếu đã kết thúc'
                : isActive(x)
                  ? 'Ngừng hoạt động'
                  : 'Bật hoạt động'
            "
            @click="x.timeStatus !== 3 && emit('toggle', x)"
          >
            <i
              class="bi"
              :class="
                isActive(x)
                  ? 'bi-power'
                  : 'bi-check-circle'
              "
            ></i>
          </button>

          <!-- xoa 
          <button
            type="button"
            class="ss-icon-btn danger"
            title="Xóa"
            @click="emit('delete', x)"
          >
            <i class="bi bi-trash"></i>
          </button> -->

        </div>
      </td>

    </tr>

    <!-- ko co du lieu -->
    <tr v-if="!rows.length">
      <td
        colspan="9"
        class="ss-empty"
      >
        <i class="bi bi-inbox"></i>
        Không có phiếu giảm giá phù hợp.
      </td>
    </tr>

  </tbody>
</template>

<style scoped>
.ss-icon-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
