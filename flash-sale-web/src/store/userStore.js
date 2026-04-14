import { defineStore } from "pinia";
import { loginApi } from "../api/auth";

export const useUserStore = defineStore("user", {
  state: () => ({
    token: localStorage.getItem("token") || "",
    userInfo: JSON.parse(localStorage.getItem("userInfo") || "null"),
    rememberedUsername: localStorage.getItem("rememberedUsername") || ""
  }),
  actions: {
    async login(payload) {
      const res = await loginApi({
        username: payload.username,
        password: payload.password
      });
      this.token = res.token;
      this.userInfo = res.user;
      localStorage.setItem("token", res.token);
      localStorage.setItem("userInfo", JSON.stringify(res.user));
      if (payload.remember) {
        this.rememberedUsername = payload.username;
        localStorage.setItem("rememberedUsername", payload.username);
      } else {
        this.rememberedUsername = "";
        localStorage.removeItem("rememberedUsername");
      }
    },
    logout() {
      this.token = "";
      this.userInfo = null;
      localStorage.removeItem("token");
      localStorage.removeItem("userInfo");
    }
  }
});
