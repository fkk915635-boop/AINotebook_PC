# 🧠 AI 智能记事本 (AINotebook Pro)

本项目包含两个版本的智能记事本应用：
1. **Desktop 桌面版 (Python)**: 基于 PyQt5 开发的本地桌面客户端。
2. **Cloud 微服务版 (Java)**: 基于 Spring Cloud + Vue 3 的全栈微服务架构应用。

---

## 📂 项目结构

```text
AINotebook_PC/
├── AINotebook_Java/      # Java 微服务版源码
│   ├── ainotebook-eureka/    # 服务注册中心
│   ├── ainotebook-gateway/   # API 网关
│   ├── ainotebook-auth-service/ # 认证服务
│   ├── ainotebook-note-service/ # 笔记服务
│   └── ainotebook-frontend/   # Vue 3 前端
├── main.py               # Python 桌面版入口
├── data/                 # 本地数据存储 (notes.json)
└── ainotebook.sql        # 数据库初始化脚本
```

---

## 🖥️ 1. Python 桌面版启动指南

### 环境准备
- Python 3.8+
- 安装依赖:
  ```bash
  pip install -r requirements.txt
  ```

### 运行
```bash
python main.py
```

### 功能亮点
- **本地存储**: 笔记保存在本地 `data/notes.json`。
- **AI 洞察**: 集成 DeepSeek API，支持真实 AI 分析与离线模拟分析。
- **极简 UI**: 使用 PyQt5 构建，支持主题切换与快捷键。

---

## ☁️ 2. Java 微服务版启动指南

详细文档请查看 [AINotebook_Java/README.md](AINotebook_Java/README.md)。

### 快速启动步骤
1. **数据库**: 执行根目录下的 `ainotebook.sql`。
2. **后端**: 依次启动 Eureka -> Auth -> Note -> Gateway。
3. **前端**:
   ```bash
   cd AINotebook_Java/ainotebook-frontend
   npm install
   npm run dev
   ```

---

## 🛠️ 技术栈
- **Desktop**: Python, PyQt5, Requests, Qtawesome
- **Backend**: Java 8, Spring Boot, Spring Cloud, MyBatis Plus, MySQL, JWT
- **Frontend**: Vue 3, Vite, Tailwind CSS, Axios

---

## 📄 开源协议
MIT License
