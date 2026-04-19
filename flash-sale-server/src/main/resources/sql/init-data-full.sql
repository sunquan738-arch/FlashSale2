-- flash-sale-server test seed data
USE flash_sale;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `seckill_order`;
DELETE FROM `orders`;
DELETE FROM `mq_outbox_event`;
DELETE FROM `seckill_activity`;
DELETE FROM `product`;
DELETE FROM `user`;

-- default password is 123456 (PBKDF2_SHA256)
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `phone`, `status`, `create_time`, `update_time`) VALUES
(1, 'alice', 'pbkdf2_sha256$120000$2QmMzyRI0jkOhwA2pv2LiQ==$pVjCPEe8wnhAutN+l7cbE4eAM/bezexiJJ7ayU2yobk=', 'Alice', '13800000001', 1, NOW(3), NOW(3)),
(2, 'bob', 'pbkdf2_sha256$120000$qdvFzfwp/qPDUo9AY+yptw==$BOkJElfl5uxwSx1REQHEGuhzQ3Zhe/1XToq6H8rjX2I=', 'Bob', '13800000002', 1, NOW(3), NOW(3)),
(3, 'charlie', 'pbkdf2_sha256$120000$1+eVDumseAiDBRU2aXfNcA==$W8nwzJVOAkSg7zKKCYuMOIlbee4WrgtUlACtUq8W/Jo=', 'Charlie', '13800000003', 1, NOW(3), NOW(3)),
(4, 'david', 'pbkdf2_sha256$120000$0CUD+wz0U5HfZiY5WumN+g==$ndGdI981C7KwOIS6Tbfr8BB8r6hladXxliCcX9ZJvI4=', 'David', '13800000004', 1, NOW(3), NOW(3)),
(5, 'eva', 'pbkdf2_sha256$120000$fG1siFxzlyMtVvzFWWY0Ow==$FbOOBu8x84eMzl/n1oAVHKAvjGjXPokRJVk4yxWB/5E=', 'Eva', '13800000005', 1, NOW(3), NOW(3));

INSERT INTO `product` (`id`, `name`, `description`, `original_price`, `stock`, `status`, `create_time`, `update_time`) VALUES
(1, 'Mechanical Keyboard K87', '87-key hot-swappable keyboard with RGB', 299.00, 120, 1, NOW(3), NOW(3)),
(2, 'Gaming Mouse M5', 'Lightweight gaming mouse with PAW3395', 199.00, 180, 1, NOW(3), NOW(3)),
(3, 'Gaming Headset H9', '7.1 surround headset with noise-cancel mic', 259.00, 90, 1, NOW(3), NOW(3)),
(4, '27-inch Monitor', '2K 170Hz gaming monitor', 1499.00, 60, 1, NOW(3), NOW(3)),
(5, 'Phone Stand', 'Foldable aluminum desktop phone stand', 39.90, 500, 1, NOW(3), NOW(3)),
(6, 'Power Bank 20000mAh', '22.5W fast charging power bank', 169.00, 220, 1, NOW(3), NOW(3)),
(7, 'Bluetooth Speaker S2', 'Portable waterproof bluetooth speaker', 129.00, 160, 1, NOW(3), NOW(3)),
(8, 'Type-C Cable', '1m 100W braided cable', 29.90, 1000, 1, NOW(3), NOW(3)),
(9, 'Wireless Charger', '15W magnetic wireless charger', 99.00, 300, 1, NOW(3), NOW(3)),
(10, 'Laptop Cooling Pad', 'Dual-fan silent cooling pad', 79.00, 140, 1, NOW(3), NOW(3));

-- Dynamic activity windows for easier local testing and pressure testing:
-- id=1 upcoming, id=2 active now, id=3 ended
INSERT INTO `seckill_activity` (`id`, `product_id`, `seckill_price`, `seckill_stock`, `start_time`, `end_time`, `status`, `create_time`, `update_time`) VALUES
(1, 1, 199.00, 50, DATE_ADD(NOW(3), INTERVAL 30 MINUTE), DATE_ADD(NOW(3), INTERVAL 180 MINUTE), 1, NOW(3), NOW(3)),
(2, 2, 99.00, 80, DATE_SUB(NOW(3), INTERVAL 20 MINUTE), DATE_ADD(NOW(3), INTERVAL 120 MINUTE), 1, NOW(3), NOW(3)),
(3, 3, 149.00, 40, DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), 1, NOW(3), NOW(3));

SET FOREIGN_KEY_CHECKS = 1;
