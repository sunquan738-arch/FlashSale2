import request from "../utils/request";

export function getMyOrdersApi() {
  return request({
    url: "/orders/my",
    method: "get"
  });
}
