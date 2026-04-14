import { createRouter, createWebHistory } from "vue-router";

const routes = [
  {
    path: "/",
    name: "Home",
    component: () => import("../views/Home.vue")
  },
  {
    path: "/login",
    name: "Login",
    component: () => import("../views/Login.vue")
  },
  {
    path: "/seckill",
    name: "SeckillList",
    component: () => import("../views/SeckillList.vue")
  },
  {
    path: "/products/:id",
    name: "ProductDetail",
    component: () => import("../views/ProductDetail.vue")
  },
  {
    path: "/my-orders",
    name: "MyOrders",
    component: () => import("../views/MyOrders.vue"),
    meta: { requiresAuth: true }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 };
  }
});

router.beforeEach((to, from, next) => {
  if (!to.meta.requiresAuth) {
    next();
    return;
  }
  const token = localStorage.getItem("token");
  if (!token) {
    next(`/login?redirect=${encodeURIComponent(to.fullPath)}`);
    return;
  }
  next();
});

export default router;
