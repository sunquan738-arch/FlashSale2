import request from "../utils/request";

export function getProductListApi() {
  return request({
    url: "/products",
    method: "get"
  });
}

export function getProductDetailApi(id) {
  return request({
    url: `/products/${id}`,
    method: "get"
  });
}
