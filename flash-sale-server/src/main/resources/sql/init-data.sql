-- flash-sale-server test seed data
USE flash_sale;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `seckill_order`;
DELETE FROM `orders`;
DELETE FROM `seckill_activity`;
DELETE FROM `product`;
DELETE FROM `user`;

INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `phone`, `status`, `create_time`, `update_time`) VALUES
(1, 'alice', '123456', 'Alice', '13800000001', 1, NOW(3), NOW(3)),
(2, 'bob', '123456', 'Bob', '13800000002', 1, NOW(3), NOW(3)),
(3, 'charlie', '123456', 'Charlie', '13800000003', 1, NOW(3), NOW(3)),
(4, 'david', '123456', 'David', '13800000004', 1, NOW(3), NOW(3)),
(5, 'eva', '123456', 'Eva', '13800000005', 1, NOW(3), NOW(3));

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

-- Relative to 2026-04-14: one not started, one ongoing, one ended
INSERT INTO `seckill_activity` (`id`, `product_id`, `seckill_price`, `seckill_stock`, `start_time`, `end_time`, `status`, `create_time`, `update_time`) VALUES
(1, 1, 199.00, 50, '2026-04-15 10:00:00.000', '2026-04-15 12:00:00.000', 1, NOW(3), NOW(3)),
(2, 2, 99.00, 80, '2026-04-14 00:00:00.000', '2026-04-14 23:59:59.000', 1, NOW(3), NOW(3)),
(3, 3, 149.00, 40, '2026-04-10 10:00:00.000', '2026-04-12 22:00:00.000', 1, NOW(3), NOW(3));

SET FOREIGN_KEY_CHECKS = 1;
