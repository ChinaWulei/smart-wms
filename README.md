# 智能仓储管理系统 WMS

技术栈：Spring Boot 3、Spring Data JPA、PostgreSQL、Vue 3、Vite。Shipping Job 使用 PostgreSQL `jsonb` 保存出库单快照。

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

前后端分开部署：

```bash
# 后端和 PostgreSQL
docker compose -f docker-compose.backend.yml up -d --build

# 前端，VITE_API_BASE 必须是浏览器能够访问的后端地址
VITE_API_BASE=http://你的后端地址:8080/api docker compose -f docker-compose.frontend.yml up -d --build
```

Windows PowerShell 中部署前端：

```powershell
$env:VITE_API_BASE="http://你的后端地址:8080/api"
docker compose -f docker-compose.frontend.yml up -d --build
```

访问：

- 前端：http://localhost
- 后端：http://localhost:8080/api/dashboard
- PostgreSQL：localhost:5432 / postgres / postgres / smart_wms

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
GOOGLE_API_KEY=你的Gemini API Key
GEMINI_MODEL=gemini-2.5-flash
AI_INTERNAL_TOKEN=请设置一个随机长字符串
```

悬浮 AI 助手由独立的 Python `ai-service` 提供，使用 LangGraph 编排：

- Rules Agent：读取 `ai-service/knowledge/wms_rules.md` 回答规则问题。
- Analytics Agent：通过 Spring Boot 内部只读接口分析当前仓库数据。
- Report Agent：生成并保存运营报表，返回 PDF 下载链接。

Gemini API Key 可在 Google AI Studio 创建。`ai-service` 使用
`langchain-google-genai` 调用 Gemini Developer API。

Python 服务不直接连接 PostgreSQL。部署时 `backend` 与 `ai-service` 的
`AI_INTERNAL_TOKEN` 必须保持一致，生产环境不要使用默认值 `change-me`。

未配置 AI 时，系统会生成规则型基础报告，不影响库存操作。

## AWS部署

1. 在 AWS RDS 创建 PostgreSQL 数据库 `smart_wms`，使用 `DDL_AUTO=update` 由 JPA 建表。
2. 在 EC2 安装 Docker 和 Docker Compose。
3. 上传本目录到 EC2，设置环境变量：

```bash
export DB_URL='jdbc:postgresql://你的-rds-endpoint:5432/smart_wms'
export DB_USERNAME='postgres'
export DB_PASSWORD='你的RDS密码'
export GOOGLE_API_KEY='你的Gemini API Key'
export GEMINI_MODEL='gemini-2.5-flash'
export AI_INTERNAL_TOKEN='请设置一个随机长字符串'
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
