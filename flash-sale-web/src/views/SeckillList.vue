<template>
  <div>
    <div class="section-title">
      <h2>秒杀活动列表</h2>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="12" :lg="8" v-for="item in activities" :key="item.id">
        <div class="activity-card">
          <div class="top">
            <div>
              <h3>{{ item.productName || `活动#${item.id}` }}</h3>
              <p class="status">{{ item.activityStatus }}</p>
            </div>
            <RouterLink :to="`/products/${item.productId}`">
              <el-button type="primary" plain>查看商品</el-button>
            </RouterLink>
          </div>

          <div class="price">秒杀价 ￥{{ Number(item.seckillPrice).toFixed(2) }}</div>
          <StockProgress :stock="item.seckillStock" :total="Math.max(item.seckillStock, 100)" />

          <div class="countdown-wrap">
            <span>{{ item.activityStatus === "未开始" ? "距开始" : "距结束" }}</span>
            <Countdown
              :target-time="item.activityStatus === '未开始' ? item.startTime : item.endTime"
              @finished="loadActivities"
            />
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { getSeckillActivitiesApi } from "../api/seckill";
import Countdown from "../components/Countdown.vue";
import StockProgress from "../components/StockProgress.vue";

const activities = ref([]);
async function loadActivities() {
  activities.value = await getSeckillActivitiesApi();
}

onMounted(loadActivities);
</script>

<style scoped>
.activity-card {
  margin-bottom: 16px;
  padding: 18px;
  border-radius: 16px;
  background: #fff;
  box-shadow: var(--card-shadow);
}

.top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

h3 {
  margin: 0;
}

.status {
  margin: 4px 0 0;
  color: #666;
  font-size: 13px;
}

.price {
  margin: 14px 0;
  color: #ff3a00;
  font-size: 28px;
  font-weight: 700;
}

.countdown-wrap {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #666;
}
</style>
