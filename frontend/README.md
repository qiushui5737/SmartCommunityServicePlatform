# 智慧社区物业管理系统 — 前端

> 基于 Vue 3 + Element Plus 的 SPA 前端应用，提供业主端和管理端双端工作台，支持 3D 车位可视化和 ECharts 数据概览。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue.js | 3.5.32 | 渐进式框架（Composition API） |
| Vite | 8.0.4 | 下一代构建工具 |
| Element Plus | 2.13.7 | 桌面端 UI 组件库 |
| Axios | 1.15.0 | HTTP 请求客户端 |
| Vue Router | 4.6.4 | 路由管理 |
| ECharts | 6.0.0 | 数据可视化图表 |
| Three.js | 0.184.0 | 3D WebGL 渲染引擎 |
| WangEditor | 5.1.23 | 富文本编辑器 |
| Sass | 1.99.0 | CSS 预处理器 |

## 项目结构

```
src/
├── api/
│   └── index.js              # Axios 实例 + 请求/响应拦截器
├── assets/                   # 静态资源
├── components/               # 公共组件
├── router/
│   └── index.js              # 路由配置 + 导航守卫
├── styles/
│   └── base.scss             # 全局样式
├── utils/
│   └── request.js            # HTTP 工具
├── views/
│   ├── Login.vue             # 登录页（蓝图风格）
│   ├── Register.vue          # 注册页
│   ├── owner/                # 业主端（9 个页面）
│   │   ├── Layout.vue        # 业主端布局框架
│   │   ├── OwnerHome.vue     # 首页概览
│   │   ├── OwnerBills.vue    # 在线缴费
│   │   ├── OwnerParking.vue  # 车位选购（3D 可视化）
│   │   ├── OwnerFacility.vue # 设施借用
│   │   ├── OwnerRepair.vue   # 设施报修
│   │   ├── OwnerFeedback.vue # 留言反馈
│   │   ├── OwnerAccessCard.vue # 门禁卡
│   │   ├── OwnerAnnouncement.vue # 社区公告
│   │   └── OwnerProfile.vue  # 个人中心
│   └── admin/                # 管理端（13 个页面）
│       ├── Layout.vue        # 管理端布局框架
│       ├── Dashboard.vue     # 数据概览（ECharts）
│       ├── UserManage.vue    # 用户管理
│       ├── FeeManage.vue     # 费用项目管理
│       ├── BuildingHouseManage.vue # 楼栋房屋管理
│       ├── BuildingVisual.vue # 楼栋平面图
│       ├── AdminParkingManage.vue  # 车位管理
│       ├── AdminFacilityManage.vue # 设施管理
│       ├── RepairManage.vue  # 报修管理
│       ├── AdminFeedbackManage.vue # 反馈管理
│       ├── AdminAccessCardManage.vue # 门禁卡管理
│       ├── AdminAccessLog.vue # 进出记录
│       └── AnnouncementManage.vue # 公告管理
├── App.vue                   # 根组件
└── main.js                   # 入口文件
```

## 环境要求

- **Node.js** 18+
- **npm** 9+

## 快速开始

### 1. 安装依赖

```bash
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

访问 `http://localhost:5173`，开发环境自动代理 `/api` 和 `/uploads` 请求到后端 `http://localhost:8081`。

### 3. 构建生产包

```bash
npm run build
```

生成 `dist/` 目录，部署到 Nginx 或其他静态服务器。

## 页面功能一览

### 业主端

| 页面 | 路由 | 功能说明 |
|------|------|---------|
| 首页概览 | `/owner/home` | 统计卡片、消息提醒、快捷入口 |
| 在线缴费 | `/owner/bills` | 账单列表、状态筛选、模拟支付 |
| 车位选购 | `/owner/parking` | Three.js 3D 车位地图、购买/释放 |
| 设施借用 | `/owner/facility` | 分类浏览、借用申请、归还 |
| 设施报修 | `/owner/repairs` | 提交报修、查看进度 |
| 留言反馈 | `/owner/feedback` | 双向沟通时间线 |
| 门禁卡 | `/owner/access-card` | 仿真卡面展示、一键挂失 |
| 社区公告 | `/owner/announcements` | 公告列表、详情查看 |
| 个人中心 | `/owner/profile` | 信息编辑、密码修改、头像上传 |

### 管理端

| 页面 | 路由 | 功能说明 |
|------|------|---------|
| 数据概览 | `/admin/dashboard` | ECharts 柱状图+饼图、统计卡片 |
| 用户管理 | `/admin/users` | CRUD、房屋绑定、状态切换 |
| 费用管理 | `/admin/fee` | 费用项目 CRUD |
| 楼栋房屋 | `/admin/building-house` | 三级级联管理、入住/退租 |
| 楼栋平面图 | `/admin/building-visual` | 3D 楼栋模型展示 |
| 车位管理 | `/admin/parking` | CRUD、锁定/解锁、统计 |
| 设施管理 | `/admin/facility` | CRUD、审批借用、软删除 |
| 报修管理 | `/admin/repair` | 查看工单、处理回复 |
| 反馈管理 | `/admin/feedback` | 回复反馈、关闭工单 |
| 门禁卡管理 | `/admin/access-card` | 发行、挂失、注销全生命周期 |
| 进出记录 | `/admin/access-log` | 多维查询、统计面板 |
| 公告管理 | `/admin/announcement` | 发布公告、置顶管理 |

## 亮点功能

### 车位 3D 可视化选购

使用 **Three.js** WebGL 渲染引擎，将后端车位网格数据渲染为可交互的 3D 方块矩阵：
- 按状态着色：🟢 空闲 | ⚫ 已售 | 🔴 锁定 | 🟡 预订
- 支持鼠标旋转、缩放、点击交互
- 楼栋切换自动重新渲染

### ECharts 数据概览

Dashboard 页面通过 **ECharts 6.0** 渲染：
- 📊 缴费状态柱状图（已缴/待缴/逾期）
- 🥧 房屋入住率环形饼图
- 响应式设计，组件销毁时自动释放实例

### 蓝图风格登录页

登录页采用全屏建筑蓝图 SVG 背景 + 右上角悬浮卡片的独特视觉风格，支持业主/管理员角色切换。

## 配套后端

后端仓库：[CommunityManagement](https://github.com/qiushui5737/CommunityManagement)

## 许可证

本项目为课程设计作品，仅供学习使用。
# Vue 3 + Vite

This template should help get you started developing with Vue 3 in Vite. The template uses Vue 3 `<script setup>` SFCs, check out the [script setup docs](https://v3.vuejs.org/api/sfc-script-setup.html#sfc-script-setup) to learn more.

Learn more about IDE Support for Vue in the [Vue Docs Scaling up Guide](https://vuejs.org/guide/scaling-up/tooling.html#ide-support).
