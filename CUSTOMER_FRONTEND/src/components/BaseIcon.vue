<script setup>
defineProps({ name: String })
const paths = { search: 'm21 21-4.3-4.3 M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16', menu: 'M4 6h16M4 12h16M4 18h16', close: 'm6 6 12 12M6 18 18 6', arrow: 'M4 12h16m-6-6 6 6-6 6', left: 'm15 5-7 7 7 7', right: 'm9 5 7 7-7 7', filter: 'M4 7h16M7 12h10M10 17h4', image: 'M4 4h16v16H4z M4 16l5-5 4 4 3-3 4 4 M8 8h.01' }
</script>
<template><svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path :d="paths[name] || paths.arrow" /></svg></template>
