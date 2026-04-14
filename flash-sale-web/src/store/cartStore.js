import { defineStore } from "pinia";

export const useCartStore = defineStore("cart", {
  state: () => ({
    items: []
  }),
  getters: {
    count: (state) => state.items.reduce((sum, item) => sum + item.quantity, 0)
  },
  actions: {
    addItem(product) {
      const exist = this.items.find((i) => i.id === product.id);
      if (exist) {
        exist.quantity += 1;
      } else {
        this.items.push({ ...product, quantity: 1 });
      }
    },
    clear() {
      this.items = [];
    }
  }
});
