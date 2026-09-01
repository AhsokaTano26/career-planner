<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <RouterView v-slot="{ Component }">
    <Transition name="page" mode="out-in">
      <div class="route-transition-shell" :key="route.fullPath">
        <component :is="Component" />
      </div>
    </Transition>
  </RouterView>
</template>
