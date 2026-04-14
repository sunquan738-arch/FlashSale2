<template>
  <div>
    <div class="row">
      <span>库存进度</span>
      <span :class="{ low: lowStock }">{{ stock }}/{{ total }}</span>
    </div>
    <el-progress
      :percentage="percent"
      :color="barColor"
      :stroke-width="12"
      :show-text="false"
    />
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  stock: {
    type: Number,
    default: 0
  },
  total: {
    type: Number,
    default: 100
  }
});

const percent = computed(() => {
  if (!props.total) return 0;
  return Math.max(0, Math.min(100, Math.round((props.stock / props.total) * 100)));
});

const lowStock = computed(() => percent.value <= 20);
const barColor = computed(() => (lowStock.value ? "#f56c6c" : "#ff4400"));
</script>

<style scoped>
.row {
  margin-bottom: 8px;
  display: flex;
  justify-content: space-between;
  color: #666;
  font-size: 13px;
}

.low {
  color: #f56c6c;
  font-weight: 700;
}
</style>
