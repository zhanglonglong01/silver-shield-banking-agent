<script setup lang="ts">
defineProps<{ eyebrow: string; title: string; description: string }>()
</script>

<template>
  <header class="page-heading">
    <p class="eyebrow">{{ eyebrow }}</p>
    <div><h1>{{ title }}</h1><p>{{ description }}</p></div>
    <slot />
  </header>
</template>
