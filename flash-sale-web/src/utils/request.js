import axios from "axios";
import { ElMessage } from "element-plus";

const request = axios.create({
  baseURL: "/api",
  timeout: 15000
});

request.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => {
    const payload = response.data;
    if (payload && typeof payload.code !== "undefined") {
      if (payload.code === 200) {
        return payload.data;
      }
      const message = payload.message || "请求失败";
      ElMessage.error(message);
      const error = new Error(message);
      error.code = payload.code;
      return Promise.reject(error);
    }
    return payload;
  },
  (error) => {
    const status = error?.response?.status;
    if (status === 401) {
      localStorage.removeItem("token");
      localStorage.removeItem("userInfo");
      ElMessage.error("登录已过期，请重新登录");
      if (!location.pathname.includes("/login")) {
        location.href = `/login?redirect=${encodeURIComponent(location.pathname)}`;
      }
    } else {
      ElMessage.error(error?.response?.data?.message || error.message || "网络异常");
    }
    return Promise.reject(error);
  }
);

export default request;
