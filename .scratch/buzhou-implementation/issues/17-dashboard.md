# 17 — 开发者控制台（dashboard）

**What to build:** buzhou-observe-dashboard 内嵌 Web 模块：引入即自动装配（复用容器或独立端口可配）；查询 API：会话列表/回放、Span 树、Event 流、注入快照、token/耗时统计；前端单页构建产物打进 jar；Skill 管理页（对接 14 管理 API）；定位开发调试，生产监控走 OTel。

**Blocked by:** 11

**Status:** done（实现 768c7eb；复审修复同提交：内存 summarize 空值防护 + eventsOfSpan 排序与 JDBC 对齐（契约测试补顺序断言）、nextCursor 末页空页、前端编辑误清正文/白名单、非法 JSON 400、SpanKind 常量化 + SpanRecord.activityAt 收敛重复兜底；spec 03 增补推演 #11–#14 + dashboard.port 配置表对齐。「复用业务 Boot 容器」MVC 挂载与 AutoConfiguration 归 ticket 20）

- [x] 引入模块后打开后台可选会话回放完整 Span/Event 树（DashboardHttpServerTest 端到端：replay 按轮归组 + spans?view=tree 服务端组树）
- [x] 按轮次查看注入快照（模型实际所见）（/sessions/{sid}/turns/{n}/snapshot；404 语义有测试）
- [x] token/耗时统计与 Span 属性一致（DashboardQueryServiceTest.statsConsistentWithSpanAttributes 手算对拍）
- [x] Skill 管理页可上架/下架/编辑 DB Skill（DashboardSkillAdminAdapterTest 走真实 SkillAdminApi 全链路）
