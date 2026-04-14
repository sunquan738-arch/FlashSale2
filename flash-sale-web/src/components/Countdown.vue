<template>
  <span class="countdown">{{ text }}</span>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";

const props = defineProps({
  targetTime: {
    type: [String, Number, Date],
    required: true
  }
});

const emit = defineEmits(["finished"]);
const remain = ref(0);
let timer = null;
let fired = false;

const text = computed(() => {
  if (remain.value <= 0) {
    return "00:00:00";
  }
  const totalSeconds = Math.floor(remain.value / 1000);
  const h = String(Math.floor(totalSeconds / 3600)).padStart(2, "0");
  const m = String(Math.floor((totalSeconds % 3600) / 60)).padStart(2, "0");
  const s = String(totalSeconds % 60).padStart(2, "0");
  return `${h}:${m}:${s}`;
});

function calc() {
  const end = new Date(props.targetTime).getTime();
  remain.value = Math.max(0, end - Date.now());
  if (remain.value === 0 && !fired) {
    fired = true;
    emit("finished");
  }
}

function start() {
  clearInterval(timer);
  fired = false;
  calc();
  timer = setInterval(calc, 1000);
}

watch(() => props.targetTime, start);
onMounted(start);
onBeforeUnmount(() => clearInterval(timer));
</script>

<style scoped>
.countdown {
  font-family: Consolas, "SFMono-Regular", monospace;
  font-weight: 700;
  color: #ff4400;
}
</style>
