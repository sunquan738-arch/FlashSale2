-- 若 product 表尚未包含 version 字段，请执行本脚本
ALTER TABLE `product`
    ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `stock`;
