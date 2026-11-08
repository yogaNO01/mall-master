-- Retain Chinese and Portuguese copy; add English safely.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS add_storefront_language;
DELIMITER $$
CREATE PROCEDURE add_storefront_language(IN language_column VARCHAR(2))
BEGIN
 IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='cms_storefront_text' AND column_name=language_column) THEN
  SET @ddl=CONCAT('ALTER TABLE cms_storefront_text ADD COLUMN `',language_column,'` LONGTEXT NULL');
  PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
 END IF;
END$$
DELIMITER ;
CALL add_storefront_language('en');
DROP PROCEDURE add_storefront_language;
-- Product/brand/category translations JSON accepts en keys without schema changes.
