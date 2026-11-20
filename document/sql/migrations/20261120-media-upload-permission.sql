-- 后台图片上传权限。
-- 同一上传接口服务于商品、品牌、分类和营销图片，因此把权限赋给拥有对应业务资源的角色。
INSERT INTO ums_resource(create_time, name, url, description, category_id)
SELECT NOW(), '后台图片上传', '/media/upload', '商品、品牌、分类及营销图片上传', 1
WHERE NOT EXISTS (
    SELECT 1 FROM ums_resource WHERE url = '/media/upload'
);

INSERT INTO ums_role_resource_relation(role_id, resource_id)
SELECT DISTINCT granted.role_id, upload_resource.id
FROM ums_role_resource_relation granted
JOIN ums_resource business_resource ON business_resource.id = granted.resource_id
CROSS JOIN ums_resource upload_resource
WHERE upload_resource.url = '/media/upload'
  AND business_resource.url IN (
      '/product/**',
      '/sku/**',
      '/brand/**',
      '/productCategory/**',
      '/home/advertise/**'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM ums_role_resource_relation existing
      WHERE existing.role_id = granted.role_id
        AND existing.resource_id = upload_resource.id
  );
