# 🧠 AI 智能记事本 (AINotebook Pro) - 社交版

这是一个基于 **Spring Cloud** 微服务架构的全栈 Web 应用，集成了 **DeepSeek AI** 洞察与 **Redis** 高性能社交功能。

---

## 🌟 项目亮点
- **微服务架构**: 基于 Spring Cloud (Eureka, Gateway) 构建，具备良好的扩展性。
- **AI 深度洞察**: 集成 DeepSeek API，自动为笔记生成思维报告和社区摘要。
- **高性能社交**: 使用 **Redis** 实现秒级点赞交互与去重，参考黑马点评经典设计。
- **原木色设计**: 温暖、简约的 UI 视觉系统，适配多端。

---

## 🛠️ 技术栈
- **后端**: Java 8, Spring Boot 2.7, Spring Cloud 2021, MyBatis Plus, MySQL 5.7, Redis, JWT
- **前端**: Vue 3, Vite, Tailwind CSS, Lucide Icons, Axios

---

## 🚀 快速启动指南

### 1. 基础设施准备
1. **MySQL**: 执行项目根目录下的 `ainotebook.sql` 脚本，初始化数据库。
2. **Redis**: 确保你的 Redis 服务已启动。
   - 当前配置地址: `192.168.88.101:6379`
   - 密码: `123456`
   - *如需修改，请前往 `ainotebook-note-service` 的 `application.yml`。*

### 2. 启动后端微服务 (按顺序)
请在 IDE 中依次运行以下服务的启动类：
1. **Eureka Server**: `ainotebook-eureka` (端口: 8761) - *注册中心*
2. **Auth Service**: `ainotebook-auth-service` (端口: 8082) - *认证与鉴权*
3. **Note Service**: `ainotebook-note-service` (端口: 8081) - *核心业务与 AI/社交功能*
4. **Gateway**: `ainotebook-gateway` (端口: 8080) - *网关入口*

### 3. 启动前端页面
进入 `AINotebook_Java/ainotebook-frontend` 目录：
```bash
npm install
npm run dev
```
访问地址: [http://localhost:3000](http://localhost:3000)

---

## 📂 目录结构
- `ainotebook-eureka`: 服务注册与发现
- `ainotebook-gateway`: API 网关 (处理权限拦截与路由)
- `ainotebook-auth-service`: 认证微服务 (登录/注册、JWT 发放)
- `ainotebook-note-service`: 核心服务 (笔记管理、AI 分析、社区博客、点赞)
- `ainotebook-common`: 通用工具类 (JWT、Security)
- `ainotebook-frontend`: Vue 3 前端项目

---

## 📄 开源协议
MIT License
