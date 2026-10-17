<script setup>
defineProps({ name: String })
const paths = { search: 'm21 21-4.3-4.3 M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16', menu: 'M4 6h16M4 12h16M4 18h16', close: 'm6 6 12 12M6 18 18 6', arrow: 'M4 12h16m-6-6 6 6-6 6', left: 'm15 5-7 7 7 7', right: 'm9 5 7 7-7 7', filter: 'M4 7h16M7 12h10M10 17h4', image: 'M4 4h16v16H4z M4 16l5-5 4 4 3-3 4 4 M8 8h.01' }
Object.assign(paths, {
  user: 'M20 21v-2a7 7 0 0 0-14 0v2 M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8',
  pin: 'M20 10c0 6-8 11-8 11S4 16 4 10a8 8 0 1 1 16 0Z M12 13a3 3 0 1 0 0-6 3 3 0 0 0 0 6',
  shield: 'M12 3 4 6v6c0 5 8 9 8 9s8-4 8-9V6l-8-3Z m-4 9 3 3 5-6',
  check: 'm5 12 4 4L19 6',
  edit: 'm15 5 4 4 M5 15 15 5a3 3 0 0 1 4 4L9 19l-5 1 1-5Z',
  mail: 'M3 5h18v14H3Z m0 0 9 8 9-8',
  calendar: 'M4 5h16v16H4Z M4 10h16 M8 3v4 M16 3v4 M8 14h2 M14 14h2',
  home: 'm3 11 9-8 9 8 M5 10v11h14V10 M9 21v-7h6v7',
  office: 'M5 21V3h14v18 M3 21h18 M9 7h1 M14 7h1 M9 11h1 M14 11h1 M10 21v-5h4v5',
  lock: 'M5 10h14v11H5Z M8 10V7a4 4 0 0 1 8 0v3 M12 15v2'
})
</script>
<template><svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path :d="paths[name] || paths.arrow" /></svg></template>
