# 智能仓储管理系统 WMS

技术栈：Spring Boot 3、Spring Data JPA、MySQL、Vue 3、Vite。系统预留 AWS RDS、EC2、S3 和外部 AI 服务配置。

## 模块

- 商品管理：新增、编辑、删除、查询、分类筛选，删除前检查库存和流水。
- 仓库与库位：仓库、库区、货架、库位，库位容量和占用校验。
- 入库/出库/盘点：所有库存变化都在事务内写入库存流水，库存不允许为负数。
- 库存查询与预警：按商品、仓库、库位查询，低于安全库存自动生成预警。
- 数据看板：商品总数、库存总量、今日出入库、预警数、热门出库、趋势和仓库占比。
- AI仓储助手：后端先聚合结构化数据，再传给 AI 生成补货建议、异常分析和运营报告，支持 PDF 导出。
- 文件存储：商品图片字段可存 URL 或 S3 Key，`S3PlaceholderService` 已预留 Bucket/Region URL 拼接入口，后续可接入 AWS SDK 上传签名。

## 本地运行

```bash
cd smart-wms
docker compose up --build
```

访问：

- 前端：http://localhost
- 后端：http://localhost:8080/api/dashboard
- MySQL：localhost:3306 / root / root / smart_wms

也可以分开运行：

```bash
cd backend
mvn spring-boot:run

cd ../frontend
npm install
npm run dev
```

## AI配置

AI不会直接查询数据库。`AiReportService` 调用 `DashboardService.structuredAiData()` 生成 JSON，再由 `AiTextClient` 传给外部模型。

环境变量：

```bash
AI_ENDPOINT=https://api.openai.com/v1/chat/completions
AI_API_KEY=你的Key
AI_MODEL=gpt-4o-mini
```

未配置 AI 时，系统会生成规则型基础报告，不影响库存操作。

## AWS部署

1. 在 AWS RDS 创建 MySQL 8 数据库 `smart_wms`，导入 `db/init.sql` 或将后端 `DDL_AUTO=update` 交给 JPA 建表。
2. 在 EC2 安装 Docker 和 Docker Compose。
3. 上传本目录到 EC2，设置环境变量：

```bash
export DB_URL='jdbc:mysql://你的-rds-endpoint:3306/smart_wms?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=true'
export DB_USERNAME='admin'
export DB_PASSWORD='你的RDS密码'
export AI_ENDPOINT='https://api.openai.com/v1/chat/completions'
export AI_API_KEY='你的Key'
export AWS_S3_BUCKET='你的bucket'
export AWS_REGION='ap-southeast-1'
```

4. 修改 `docker-compose.yml` 后端环境变量引用 RDS，或直接用 ECS/EC2 systemd 注入变量。
5. 放通 EC2 安全组 80/443，RDS 安全组仅允许 EC2 访问 3306。

## 关键接口

- `GET/POST/PUT/DELETE /api/products`
- `GET/POST /api/warehouses`
- `GET/POST /api/locations`
- `POST /api/inbound`
- `POST /api/outbound`
- `POST /api/inventory-checks`
- `GET /api/stocks`
- `GET /api/movements`
- `GET/PUT /api/alerts`
- `GET /api/dashboard`
- `POST/GET /api/ai/reports`
- `GET /api/ai/reports/{id}/pdf`

## 本次验证结果

已在当前环境执行前端生产构建：

```bash
cd frontend && npm.cmd run build
```

结果：构建通过，产物生成到 `frontend/dist`。

当前机器没有 `mvn`/`javac`，无法直接编译 Spring Boot 后端。建议在具备 Maven 与 JDK 17 的环境执行：

```bash
cd backend && mvn test
```
