<script setup lang="ts">
withDefaults(defineProps<{
  name: 'menu' | 'close' | 'logout' | 'grid'
  label?: string
}>(), { label: '' })

const paths = {
  menu: ['M4 6h16', 'M4 12h16', 'M4 18h16'],
  close: ['M6 6l12 12', 'M18 6L6 18'],
  logout: ['M10 5H5v14h5', 'M14 8l4 4-4 4', 'M8 12h10'],
  grid: ['M5 5h5v5H5z', 'M14 5h5v5h-5z', 'M5 14h5v5H5z', 'M14 14h5v5h-5z'],
} as const
</script>

<template>
  <svg class="app-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="square" stroke-linejoin="miter" aria-hidden="true">
    <path v-for="path in paths[name]" :key="path" :d="path" />
  </svg>
</template>
