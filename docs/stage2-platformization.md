# Stage 2 Platformization Notes

## Modules
- flash-sale-server: existing business monolith (still handles auth/product/seckill/order).
- gateway-service: new traffic entry, JWT verification, traceId, unified gateway error, gateway routing.

## Routing
- /api/auth/** -> flash-sale-server
- /api/products/** -> flash-sale-server
- /api/seckill/** -> flash-sale-server
- /api/orders/** -> flash-sale-server

## Nacos
- Optional profile: `nacos`.
- Local fallback remains in `application.yml` and `application-dev.yml`.
- Nacos examples:
  - docs/nacos/flash-sale-server.yaml
  - docs/nacos/gateway-service.yaml

## Sentinel resources
- `flashsale.seckill.do`: hotspot param flow limit by `activityId`.
- `flashsale.order.write.chain`: flow + degrade protection for order persistence chain.
- Gateway route flow rules: route-auth / route-products / route-seckill / route-orders.

## Gray switch
- Frontend proxy target can route to gateway (`8090`) or old backend (`8080`) using `VITE_API_TARGET`.
- Gateway route URI can switch static/direct or service-discovery via `GATEWAY_ROUTE_URI`.
