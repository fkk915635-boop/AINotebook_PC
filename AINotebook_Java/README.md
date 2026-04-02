# 🧠 AI 智能记事本 (AINotebook Pro)

这是一个从 Python 项目升级而来的全栈 Web 应用。
采用 **Spring Cloud** 微服务架构，前端使用 **Vue 3 + Tailwind CSS**，数据库使用 **MySQL 5.7**。

## 🌟 项目亮点
- **微服务架构**: 基于 Spring Cloud (Eureka, Gateway) 构建，具备良好的扩展性。
- **原木色设计**: 专为 15-25 岁青少年设计的“原木思维空间”，色调温暖、简约、现代。
- **AI 深度洞察**: 集成 DeepSeek API，对历史记录进行深度思维分析（支持模拟模式）。
- **账号体系**: 提供注册/登录与 JWT 鉴权，未登录无法访问笔记接口。
- **极简交互**: 响应式设计，适配手机与桌面端。

## 🛠️ 技术栈
- **后端**: Java 8, Spring Boot 2.7, Spring Cloud 2021, MyBatis Plus, JWT
- **前端**: Vue 3, Vite, Tailwind CSS, Lucide Icons, Axios
- **数据库**: MySQL 5.7

## 🚀 快速启动

### 1. 数据库准备
使用 Navicat 或其他工具连接本地 MySQL 5.7，执行项目根目录下的 `ainotebook.sql` 文件。

### 2. 启动后端
按顺序启动以下服务：
1. **Eureka Server**: `ainotebook-eureka` (端口: 8761)
2. **Auth Service**: `ainotebook-auth-service` (端口: 8082)
3. **Note Service**: `ainotebook-note-service` (端口: 8081)
4. **Gateway**: `ainotebook-gateway` (端口: 8080)

### 3. 启动前端
进入 `ainotebook-frontend` 目录：
```bash
npm install
npm run dev
```
访问地址: [http://localhost:3000](http://localhost:3000)

打开后会先进入 `/login` 登录页，注册账号后登录即可进入笔记页。

## 🎨 样式说明
项目采用“原木色” (Wood Theme) 调色板：
- 背景: `#F5F1E9` (米色)
- 卡片: `#FDFCFB` (珍珠白)
- 文字: `#22333B` (深岩色)
- 强调: `#A68A64` (木纹金)

## 📂 目录结构
- `ainotebook-eureka`: 服务注册与发现
- `ainotebook-gateway`: API 网关 (处理 CORS 与 路由)
- `ainotebook-auth-service`: 认证微服务 (注册/登录、JWT 发放)
- `ainotebook-note-service`: 笔记管理核心微服务
- `ainotebook-common`: 通用工具与依赖
- `ainotebook-frontend`: Vue 3 前端项目
