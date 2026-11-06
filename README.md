# Mall 商城服务端

本项目是商城官网和后台管理系统的 Java 服务端，基于 Spring Boot、MyBatis 和 Spring Security 构建，包含商城门户接口与后台管理接口。

## 服务模块

- `mall-portal`：官网商城接口，默认端口 `8085`
- `mall-admin`：后台管理接口，默认端口 `8080`
- `mall-common`：公共工具和基础配置
- `mall-mbg`：MyBatis 数据访问代码
- `mall-security`：认证与权限模块

## 运行环境

- JDK 17
- Maven 3.9+
- Docker 和 Docker Compose
- MySQL 8.0
- Redis 7
- MongoDB 7
- RabbitMQ 3.13
- RustFS（S3 兼容对象存储）

## 数据库文件

当前项目使用的数据库文件位于：

```text
document/sql/mall-local.sql
```

该文件是本地商城数据库的完整导出，用于初始化或覆盖 `mall` 数据库。执行覆盖前请先备份线上数据库。

初始化示例：

```bash
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS mall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -p mall < document/sql/mall-local.sql
```

其他结构变更和数据迁移脚本位于 `document/sql/migrations/`，请按文件名顺序执行。

## 启动基础设施

项目基础设施配置位于 `document/docker/`。启动 MySQL、Redis、MongoDB、RabbitMQ 和对象存储：

```bash
cd document/docker
docker compose -f docker-compose-env.yml up -d
```

检查容器：

```bash
docker compose -f docker-compose-env.yml ps
```

生产环境请通过环境变量或服务器上的配置文件提供数据库密码、Redis 密码、RabbitMQ 密码和对象存储密钥，不要将真实密码提交到仓库。

## 本地启动服务

先确认基础设施和数据库已启动，再执行：

```bash
mvn -DskipTests package -pl mall-admin,mall-portal -am
```

启动后台接口：

```bash
java -jar mall-admin/target/mall-admin-1.0-SNAPSHOT.jar --spring.profiles.active=dev --server.port=8080
```

启动商城接口：

```bash
java -jar mall-portal/target/mall-portal-1.0-SNAPSHOT.jar --spring.profiles.active=dev --server.port=8085
```

健康检查地址：

```text
http://127.0.0.1:8080/actuator/health
http://127.0.0.1:8085/actuator/health
```

## 生产部署

生产环境建议使用独立的 systemd 服务运行两个 JAR：

```ini
ExecStart=/usr/bin/java -Xms128m -Xmx512m -jar /opt/macro/backend/mall-admin.jar
ExecStart=/usr/bin/java -Xms128m -Xmx512m -jar /opt/macro/backend/mall-portal.jar
```

发布流程：

1. 在本地完成代码构建并确认生成两个 JAR。
2. 将 `mall-admin` 和 `mall-portal` 的 JAR 上传到服务器部署目录。
3. 更新服务器上的生产环境变量。
4. 重启对应服务并检查日志和健康检查接口。

```bash
sudo systemctl restart mall-admin mall-portal
sudo systemctl status mall-admin mall-portal --no-pager
```

官网和后台前端通过 Nginx 反向代理访问：

- `/api/` → `mall-portal:8085`
- `/admin-api/` → `mall-admin:8080`

## 配置文件

开发配置位于各模块的 `src/main/resources/application-dev.yml`，生产配置位于 `application-prod.yml`。数据库连接、Redis、MongoDB、RabbitMQ 和对象存储配置应通过服务器环境变量覆盖。

## 目录结构

```text
mall-master/
├── mall-admin/       后台管理接口
├── mall-portal/      官网商城接口
├── mall-common/      公共模块
├── mall-mbg/         数据访问代码
├── mall-security/    安全认证模块
├── document/docker/  Docker Compose 配置
└── document/sql/     数据库初始化与迁移脚本
```

## 多语言数据

商品、品牌、分类等内容的多语言字段使用数据库中的 `translations` JSON 字段保存。新增或修改多语言内容后，重启服务即可由接口返回最新数据。
