<script setup lang="ts">
defineProps<{ eyebrow:string; title:string; description?:string }>()
</script>
<template><div class="page-title"><div><p class="eyebrow">{{ eyebrow }}</p><h1>{{ title }}</h1><p v-if="description">{{ description }}</p></div><div v-if="$slots.actions" class="page-actions" data-testid="page-actions" role="group" aria-label="页面操作"><slot name="actions"/></div></div></template>
