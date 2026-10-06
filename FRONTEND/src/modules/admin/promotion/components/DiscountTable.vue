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
</script>

<template>
  <tbody>

    <tr
      v-for="(x, i) in rows"
      :key="x.id ?? x.code"
    >

      <td class="c">
        {{ (page - 1) * pageSize + i + 1 }}
      </td>

      <td>
        <span class="ss-code">
          {{ x.code }}
        </span>
      </td>

      <td>
        {{ x.name }}
      </td>

      <td>
        <span
          class="ss-pill"
          :class="{ warn: x.type === 'Cá nhân' }"
        >
          {{ x.type }}
        </span>
      </td>

      <td>
        {{ x.value }}
      </td>

      <td class="nowrap">
        {{ x.start }}
      </td>

      <td class="nowrap">
        {{ x.end }}
      </td>

      <td>
        <span
          class="ss-pill dot"
          :class="isActive(x) ? 'success' : 'danger'"
        >
          {{ x.statusLabel }}
        </span>
      </td>

      <td>
        <div class="ss-row-actions">

          <!-- Xem -->
          <button
            class="ss-icon-btn"
            title="Xem chi tiết"
            @click="emit('view', x.id)"
          >
            <i class="bi bi-eye"></i>
          </button>

          <!-- Sửa -->
          <button
            class="ss-icon-btn"
            title="Sửa"
            @click="emit('edit', x.id)"
          >
            <i class="bi bi-pencil"></i>
          </button>

          <!-- Bật / ngừng -->
          <button
            class="ss-icon-btn danger"
            :title="isActive(x)
              ? 'Ngừng hoạt động'
              : 'Kích hoạt'"
            @click="emit('toggle', x)"
          >
            <i class="bi bi-power"></i>
          </button>

          <!-- Xóa -->
          <button
            class="ss-icon-btn danger"
            title="Xóa"
            @click="emit('delete', x)"
          >
            <i class="bi bi-trash"></i>
          </button>

        </div>
      </td>

    </tr>

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