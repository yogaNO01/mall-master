-- Product, brand, category and attribute translations use JSON and require no schema change.
-- Fixed storefront copy has dedicated language columns.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS add_storefront_spanish;
DELIMITER $$
CREATE PROCEDURE add_storefront_spanish()
BEGIN
 IF NOT EXISTS (
   SELECT 1 FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'cms_storefront_text' AND column_name = 'es'
 ) THEN
   ALTER TABLE cms_storefront_text ADD COLUMN es LONGTEXT NULL AFTER pt;
 END IF;
END$$
DELIMITER ;
CALL add_storefront_spanish();
DROP PROCEDURE add_storefront_spanish;
