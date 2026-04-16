<template>
  <div>
    <div class="section-title">
      <h2>我的订单</h2>
    </div>

    <el-empty v-if="orders.length === 0" description="暂无订单" />

    <div class="order-list" v-else>
      <div class="order-item" v-for="item in orders" :key="item.id">
        <div class="left">
          <div class="no">订单号：{{ item.orderNo || `NO.${item.id}` }}</div>
          <div class="time">创建时间：{{ formatTime(item.createTime) }}</div>
        </div>
        <div class="right">
          <div class="amount">￥{{ Number(item.totalAmount || 0).toFixed(2) }}</div>
          <el-tag :type="statusTagType(item)" effect="dark">{{ statusText(item) }}</el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { getMyOrdersApi } from "../api/order";

const orders = ref([]);

async function loadOrders() {
  try {
    const data = await getMyOrdersApi();
    orders.value = Array.isArray(data) ? data : [];
  } catch {
    orders.value = [];
  }
}

function statusText(item) {
  if (item?.resultStatus) {
    return item.resultStatus;
  }
  if (item?.orderStatus === 0) {
    return "进行中";
  }
  if (item?.orderStatus === 1 || item?.orderStatus === 3) {
    return "成功";
  }
  return "失败";
}

function statusTagType(item) {
  const text = statusText(item);
  if (text === "进行中") {
    return "warning";
  }
  if (text === "成功") {
    return "success";
  }
  return "danger";
}

function formatTime(value) {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

onMounted(loadOrders);
</script>

<style scoped>
.order-list {
  display: grid;
  gap: 14px;
}

.order-item {
  padding: 18px;
  border-radius: 16px;
  background: #fff;
  box-shadow: var(--card-shadow);
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid rgba(255, 68, 0, 0.08);
}

.left {
  min-width: 0;
}

.no {
  font-weight: 700;
  font-size: 16px;
}

.time {
  margin-top: 8px;
  font-size: 13px;
  color: #888;
}

.right {
  text-align: right;
  display: grid;
  gap: 10px;
  justify-items: end;
}

.amount {
  color: #ff4400;
  font-size: 24px;
  font-weight: 700;
}

@media (max-width: 768px) {
  .order-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
  }

  .right {
    width: 100%;
    justify-items: start;
    text-align: left;
  }
}
</style>
