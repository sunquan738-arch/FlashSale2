<template>
  <section class="banner">
    <div class="left">
      <h3>🔥 今日爆款秒杀</h3>
      <p>每天多场，先到先得，错过不补。</p>
      <RouterLink to="/seckill">
        <el-button type="primary" size="large">进入秒杀会场</el-button>
      </RouterLink>
    </div>
    <div class="right">
      <div class="item" v-for="item in activities.slice(0, 3)" :key="item.id">
        <div class="name">{{ item.productName || `活动 #${item.id}` }}</div>
        <div class="price">￥{{ Number(item.seckillPrice).toFixed(2) }}</div>
        <div class="status">{{ item.activityStatus }}</div>
      </div>
    </div>
  </section>
</template>

<script setup>
defineProps({
  activities: {
    type: Array,
    default: () => []
  }
});
</script>

<style scoped>
.banner {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 14px;
  border-radius: 18px;
  padding: 18px;
  color: #fff;
  box-shadow: var(--card-shadow);
  background: linear-gradient(120deg, #ff4400 0%, #ff7b00 100%);
}

.left h3 {
  margin: 0;
  font-size: 28px;
}

.left p {
  margin: 8px 0 16px;
  color: rgba(255, 255, 255, 0.9);
}

.right {
  display: grid;
  gap: 10px;
}

.item {
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.3);
  padding: 10px 12px;
  border-radius: 12px;
}

.name {
  font-weight: 600;
}

.price {
  font-size: 22px;
  margin-top: 4px;
}

.status {
  margin-top: 4px;
  font-size: 13px;
  color: #ffeccf;
}

@media (max-width: 900px) {
  .banner {
    grid-template-columns: 1fr;
  }
}
</style>
