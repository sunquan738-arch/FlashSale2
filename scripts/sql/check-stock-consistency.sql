-- Stock consistency check for seckill activity
-- Usage:
--   SET @activity_id = 2;
--   SOURCE scripts/sql/check-stock-consistency.sql;

SET @activity_id = IFNULL(@activity_id, 2);

SELECT id AS activity_id,
       product_id,
       seckill_stock AS activity_stock
FROM seckill_activity
WHERE id = @activity_id;

SELECT product_id,
       stock AS product_stock
FROM product
WHERE id = (SELECT product_id FROM seckill_activity WHERE id = @activity_id);

SELECT activity_id,
       product_id,
       COUNT(1) AS seckill_order_count
FROM seckill_order
WHERE activity_id = @activity_id
GROUP BY activity_id, product_id;

SELECT a.id                                   AS activity_id,
       a.product_id,
       a.seckill_stock                        AS db_activity_stock,
       p.stock                                AS db_product_stock,
       COALESCE(s.cnt, 0)                     AS seckill_success_count,
       (a.seckill_stock + COALESCE(s.cnt, 0)) AS activity_initial_stock_guess
FROM seckill_activity a
LEFT JOIN product p ON p.id = a.product_id
LEFT JOIN (
    SELECT activity_id, COUNT(1) AS cnt
    FROM seckill_order
    WHERE activity_id = @activity_id
    GROUP BY activity_id
) s ON s.activity_id = a.id
WHERE a.id = @activity_id;
