import { computed, ref, watch } from 'vue';

// Pagination changes the visible rows only; the full selection is owned by the form.
export function useCampaignPagination(rows, pageSize = 10) {
  const page = ref(1);
  const total = computed(() => rows.value.length);
  const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));
  const offset = computed(() => (page.value - 1) * pageSize);
  const visibleRows = computed(() => rows.value.slice(offset.value, offset.value + pageSize));
  watch(pageCount, (count) => { page.value = Math.min(page.value, count); }, { flush: 'sync' });
  function setPage(value) {
    page.value = Math.max(1, Math.min(pageCount.value, Number(value) || 1));
  }
  function reset() { page.value = 1; }
  return { page, total, pageCount, offset, visibleRows, setPage, reset };
}
