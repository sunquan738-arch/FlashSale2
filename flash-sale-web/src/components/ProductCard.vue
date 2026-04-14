<template>
  <div class="card" @click="goDetail">
    <span class="badge">限时直降</span>
    <img class="cover" :src="product.imageUrl" :alt="product.name" />
    <div class="content">
      <h3 class="title">{{ product.name }}</h3>
      <div class="price-row">
        <span class="price">￥{{ Number(product.originalPrice).toFixed(2) }}</span>
      </div>
      <div class="meta">库存 {{ product.stock }}</div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";

const props = defineProps({
  product: {
    type: Object,
    required: true
  }
});

const router = useRouter();
function goDetail() {
  router.push(`/products/${props.product.id}`);
}
</script>

<style scoped>
.card {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  background: #fff;
  box-shadow: var(--card-shadow);
  cursor: pointer;
  transition: transform 0.2s ease;
}

.card:hover {
  transform: translateY(-4px);
}

.badge {
  position: absolute;
  left: 10px;
  top: 10px;
  z-index: 2;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #fff;
  background: linear-gradient(120deg, #ff4400, #ff7a00);
}

.cover {
  width: 100%;
  height: 220px;
  object-fit: cover;
  display: block;
}

.content {
  padding: 12px;
}

.title {
  margin: 0;
  font-size: 16px;
  line-height: 1.4;
}

.price-row {
  margin-top: 8px;
}

.price {
  color: #ff2f00;
  font-size: 26px;
  font-weight: 700;
}

.meta {
  margin-top: 8px;
  color: var(--muted);
  font-size: 13px;
}
</style>
