<template>
  <header class="header">
    <div class="header-inner">
      <RouterLink class="logo" to="/">
        <span class="logo-badge">秒</span>
        <span>Flash Sale</span>
      </RouterLink>

      <nav class="nav">
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/seckill">秒杀活动</RouterLink>
        <RouterLink to="/my-orders">我的订单</RouterLink>
      </nav>

      <div class="actions">
        <el-badge :value="cartStore.count" :hidden="cartStore.count === 0">
          <el-button round>购物车</el-button>
        </el-badge>

        <template v-if="userStore.token">
          <span class="nickname">Hi, {{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
          <el-button type="primary" plain @click="onLogout">退出</el-button>
        </template>
        <template v-else>
          <RouterLink to="/login">
            <el-button type="primary">登录</el-button>
          </RouterLink>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { useCartStore } from "../store/cartStore";
import { useUserStore } from "../store/userStore";

const router = useRouter();
const userStore = useUserStore();
const cartStore = useCartStore();

function onLogout() {
  userStore.logout();
  ElMessage.success("已退出登录");
  router.push("/login");
}
</script>

<style scoped>
.header {
  position: fixed;
  top: 0;
  z-index: 99;
  width: 100%;
  border-bottom: 1px solid #eee;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
}

.header-inner {
  max-width: 1240px;
  margin: 0 auto;
  height: 64px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 20px;
}

.logo-badge {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, #ff6a00, #ff2a00);
}

.nav {
  display: flex;
  gap: 20px;
  font-size: 15px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nickname {
  color: #555;
}

@media (max-width: 900px) {
  .nav {
    display: none;
  }
}
</style>
