<template>
  <div>
    <el-carousel class="hero" height="300px">
      <el-carousel-item v-for="(item, index) in banners" :key="index">
        <div class="hero-item" :style="{ background: item.bg }">
          <h2>{{ item.title }}</h2>
          <p>{{ item.desc }}</p>
        </div>
      </el-carousel-item>
    </el-carousel>

    <div class="section-title">
      <h2>秒杀专场</h2>
      <RouterLink class="brand-text" to="/seckill">查看全部</RouterLink>
    </div>
    <SeckillBanner :activities="activities" />

    <div class="section-title">
      <h2>猜你喜欢</h2>
    </div>
    <div class="waterfall">
      <ProductCard v-for="product in products" :key="product.id" :product="product" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import ProductCard from "../components/ProductCard.vue";
import SeckillBanner from "../components/SeckillBanner.vue";
import { getProductListApi } from "../api/product";
import { getSeckillActivitiesApi } from "../api/seckill";

const products = ref([]);
const activities = ref([]);
const banners = [
  {
    title: "今晚 20:00 超级秒杀夜",
    desc: "爆款直降，最高立减 70%",
    bg: "linear-gradient(135deg,#ff4d00,#ff8a00)"
  },
  {
    title: "品牌日会场",
    desc: "限量单品，抢完即止",
    bg: "linear-gradient(135deg,#ff6a00,#ff2d55)"
  },
  {
    title: "新人专享补贴",
    desc: "首单领券再减 20 元",
    bg: "linear-gradient(135deg,#ff7a18,#ff3c00)"
  }
];

async function loadData() {
  products.value = await getProductListApi();
  activities.value = await getSeckillActivitiesApi();
}

onMounted(loadData);
</script>

<style scoped>
.hero {
  border-radius: 18px;
  overflow: hidden;
  box-shadow: var(--card-shadow);
}

.hero-item {
  height: 100%;
  padding: 32px;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hero-item h2 {
  margin: 0;
  font-size: 38px;
}

.hero-item p {
  margin-top: 10px;
  font-size: 18px;
}

.waterfall {
  column-count: 4;
  column-gap: 14px;
}

.waterfall :deep(.card) {
  break-inside: avoid;
  margin-bottom: 14px;
}

@media (max-width: 1100px) {
  .waterfall {
    column-count: 3;
  }
}

@media (max-width: 820px) {
  .waterfall {
    column-count: 2;
  }

  .hero-item h2 {
    font-size: 28px;
  }
}
</style>
