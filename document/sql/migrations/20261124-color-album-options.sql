ALTER TABLE pms_product_attribute_value
    MODIFY COLUMN value TEXT NULL COMMENT '规格选项以逗号分隔，参数为单值';
