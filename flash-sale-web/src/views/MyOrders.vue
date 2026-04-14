<template>
  <div>
    <div class="section-title">
      <h2>我的订单</h2>
    </div>

    <el-empty v-if="orders.length === 0" description="暂无订单" />

    <div class="order-list">
      <div class="order-item" v-for="item in orders" :key="item.id">
        <div class="left">
          <div class="no">{{ item.orderNo || `NO.${item.id}` }}</div>
          <div class="time">{{ item.createTime || "-" }}</div>
        </div>
        <div class="right">
          <div class="amount">￥{{ Number(item.totalAmount || 0).toFixed(2) }}</div>
          <el-tag :type="statusTagType(item.orderStatus)">{{ statusText(item.orderStatus) }}</el-tag>
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
    // 当前后端未实现该接口时，前端保持空态，不阻断页面展示。
    orders.value = [];
  }
}

function statusText(status) {
  const map = {
    0: "待支付",
    1: "已支付",
    2: "已取消",
    3: "已完成"
  };
  return map[status] || "未知状态";
}

function statusTagType(status) {
  const map = {
    0: "warning",
    1: "success",
    2: "info",
    3: ""
  };
  return map[status] || "info";
}

onMounted(loadOrders);
</script>

<style scoped>
.order-list {
  display: grid;
  gap: 12px;
}

.order-item {
  padding: 16px;
  border-radius: 14px;
  background: #fff;
  box-shadow: var(--card-shadow);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.no {
  font-weight: 700;
}

.time {
  margin-top: 6px;
  font-size: 13px;
  color: #888;
}

.right {
  text-align: right;
}

.amount {
  margin-bottom: 8px;
  color: #ff4400;
  font-size: 20px;
  font-weight: 700;
}
</style>
