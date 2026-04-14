<template>
  <div class="detail" v-if="product">
    <div class="gallery">
      <img :src="product.imageUrl" :alt="product.name" />
    </div>

    <div class="info">
      <h1>{{ product.name }}</h1>
      <p class="desc">{{ product.description || "暂无商品描述" }}</p>

      <div class="price-box">
        <div class="origin">原价：<s>￥{{ Number(product.originalPrice).toFixed(2) }}</s></div>
        <div class="seckill">
          秒杀价：
          <span>￥{{ Number(activeActivity?.seckillPrice || product.originalPrice).toFixed(2) }}</span>
        </div>
      </div>

      <StockProgress :stock="activeActivity?.seckillStock || product.stock" :total="Math.max(product.stock, 100)" />

      <div class="countdown-row" v-if="activeActivity">
        <span>{{ activeActivity.activityStatus === "未开始" ? "活动倒计时" : "结束倒计时" }}</span>
        <Countdown
          :target-time="activeActivity.activityStatus === '未开始' ? activeActivity.startTime : activeActivity.endTime"
          @finished="loadData"
        />
      </div>

      <div class="actions">
        <el-button
          type="primary"
          size="large"
          :disabled="!activeActivity || activeActivity.activityStatus !== '进行中'"
          :loading="seckillLoading"
          @click="onSeckill"
        >
          立即秒杀
        </el-button>
      </div>

      <p class="tips">提示：点击秒杀后请在页面轮询结果，系统会异步创建订单。</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { getProductDetailApi } from "../api/product";
import { doSeckillApi, getSeckillActivitiesApi, querySeckillResultApi } from "../api/seckill";
import Countdown from "../components/Countdown.vue";
import StockProgress from "../components/StockProgress.vue";

const route = useRoute();
const router = useRouter();

const product = ref(null);
const activities = ref([]);
const seckillLoading = ref(false);
let pollTimer = null;

const activeActivity = computed(() => {
  return activities.value.find((a) => Number(a.productId) === Number(route.params.id)) || null;
});

async function loadData() {
  product.value = await getProductDetailApi(route.params.id);
  activities.value = await getSeckillActivitiesApi();
}

async function onSeckill() {
  if (!localStorage.getItem("token")) {
    router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`);
    return;
  }
  if (!activeActivity.value) {
    ElMessage.warning("该商品暂无秒杀活动");
    return;
  }

  seckillLoading.value = true;
  try {
    const msg = await doSeckillApi(activeActivity.value.id);
    ElMessage.success(msg || "抢购请求已提交，请稍候查询结果");
    startPolling();
  } finally {
    seckillLoading.value = false;
  }
}

function startPolling() {
  clearInterval(pollTimer);
  pollTimer = setInterval(async () => {
    const res = await querySeckillResultApi(activeActivity.value.id);
    if (res.status === "抢购成功") {
      clearInterval(pollTimer);
      ElMessage.success(`抢购成功，订单号：${res.orderNo}`);
      router.push("/my-orders");
    }
    if (res.status === "抢购失败") {
      clearInterval(pollTimer);
      ElMessage.error("抢购失败");
    }
  }, 1500);
}

onMounted(loadData);
onBeforeUnmount(() => clearInterval(pollTimer));
</script>

<style scoped>
.detail {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 22px;
}

.gallery img {
  width: 100%;
  border-radius: 18px;
  display: block;
  box-shadow: var(--card-shadow);
}

.info {
  background: #fff;
  border-radius: 18px;
  padding: 20px;
  box-shadow: var(--card-shadow);
}

.info h1 {
  margin: 0;
}

.desc {
  color: #666;
}

.price-box {
  margin: 14px 0;
  padding: 14px;
  border-radius: 12px;
  background: #fff6f1;
}

.origin {
  color: #999;
}

.seckill {
  margin-top: 8px;
  color: #444;
}

.seckill span {
  color: #ff3a00;
  font-size: 34px;
  font-weight: 800;
}

.countdown-row {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  color: #666;
}

.actions {
  margin-top: 16px;
}

.tips {
  font-size: 12px;
  color: #999;
}

@media (max-width: 980px) {
  .detail {
    grid-template-columns: 1fr;
  }
}
</style>
