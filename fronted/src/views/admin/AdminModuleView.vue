<script setup lang="ts">
import AdminWorkbench from './AdminWorkbenchView.vue'
import { useToast } from '../../composables/useToast'

defineProps<{ module: string }>()
const { show: notice } = useToast()
</script>

<template>
  <AdminWorkbench :module="module" @notice="notice" />
</template>
