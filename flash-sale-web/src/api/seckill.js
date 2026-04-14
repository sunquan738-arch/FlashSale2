import request from "../utils/request";

export function getSeckillActivitiesApi() {
  return request({
    url: "/seckill/activities",
    method: "get"
  });
}

export function getSeckillActivityDetailApi(id) {
  return request({
    url: `/seckill/activities/${id}`,
    method: "get"
  });
}

export function doSeckillApi(activityId) {
  return request({
    url: `/seckill/do/${activityId}`,
    method: "post"
  });
}

export function querySeckillResultApi(activityId) {
  return request({
    url: `/seckill/result/${activityId}`,
    method: "get"
  });
}
