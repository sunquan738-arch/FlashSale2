<template>
  <div class="app-shell">
    <AppHeader />
    <main class="main-container">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import AppHeader from "./components/AppHeader.vue";
</script>
