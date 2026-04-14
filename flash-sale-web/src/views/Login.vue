<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <div class="logo">秒</div>
        <h2>Flash Sale 秒杀商城</h2>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.remember">记住账号</el-checkbox>
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="onSubmit">
          立即登录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useUserStore } from "../store/userStore";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const formRef = ref();
const loading = ref(false);
const form = reactive({
  username: userStore.rememberedUsername || "",
  password: "",
  remember: !!userStore.rememberedUsername
});

const rules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }]
};

function onSubmit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      await userStore.login(form);
      ElMessage.success("登录成功");
      router.push(route.query.redirect || "/");
    } finally {
      loading.value = false;
    }
  });
}
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 120px);
  display: grid;
  place-items: center;
}

.login-card {
  width: 420px;
  max-width: calc(100vw - 24px);
  background: #fff;
  padding: 28px 24px;
  border-radius: 18px;
  box-shadow: var(--card-shadow);
}

.brand {
  text-align: center;
  margin-bottom: 16px;
}

.logo {
  width: 56px;
  height: 56px;
  margin: 0 auto 10px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 26px;
  font-weight: 700;
  background: linear-gradient(135deg, #ff6a00, #ff2f00);
}

.brand h2 {
  margin: 0;
}

.submit {
  width: 100%;
}
</style>
