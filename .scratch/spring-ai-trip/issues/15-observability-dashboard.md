# 可视化后台设计

Type: grilling
Status: resolved
Blocked by: 13

## Question

可观测后台的形态：独立可部署模块（内嵌 Web 服务 + 前端静态资源打包进 jar？）还是单独应用？查询 API（按会话回放、Span 树拉取、Event 流、token/耗时统计）设计？存储复用哪个 SPI？"有效上下文视图"（把某一轮实际注入模型的内容还原出来）如何实现——这要求注入时刻的快照被记录？前端技术取向（内嵌单页 vs 独立工程）？开源项目里这个模块的定位（开发调试工具 vs 生产监控）？

## Answer

**定案：内嵌 Web 模块 + 注入快照落库 + 单页打进 jar + 开发调试为主定位。**

1. **形态**：`buzhou-observe-dashboard` 可选模块，引入即自动装配内嵌 Web 服务（复用业务 Boot 容器或独立端口，可配），前端静态资源打包进 jar；零部署成本。
2. **有效上下文视图**：每轮注入视图构建完成即存快照（消息序列 + 动态预算明细）进 ObservabilityStore，后台按轮次还原「模型当时实际看到什么」；快照同时是压缩/spill 效果的解释面。
3. **查询 API**：按会话回放、Span 树拉取（session_id 平铺组树，13 已定）、Event 流、注入快照、token/耗时统计；存储复用 `ObservabilityStore` SPI，无新增存储。
4. **前端**：单页应用，构建期前端工程在模块内，发布产物为 jar 内静态资源；版本与后端同演进。
5. **定位**：首要开发调试工具（会话回放、上下文视图、思维链检视）；查询 API 公开稳定；生产监控由 `buzhou-observe-otel` 导出桥对接现有运维栈，文档明确分工。

### 影响面

- ticket 03 模块清单增补：`buzhou-observe-dashboard`（15 → 16）。
- ObservabilityStore 的 Schema 需含注入快照表（13 的「平铺两张表」扩为 Span/Event/快照三张）。
