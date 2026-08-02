# 17 — 开发者控制台（dashboard）

**What to build:** buzhou-observe-dashboard 内嵌 Web 模块：引入即自动装配（复用容器或独立端口可配）；查询 API：会话列表/回放、Span 树、Event 流、注入快照、token/耗时统计；前端单页构建产物打进 jar；Skill 管理页（对接 14 管理 API）；定位开发调试，生产监控走 OTel。

**Blocked by:** 11

**Status:** ready-for-agent

- [ ] 引入模块后打开后台可选会话回放完整 Span/Event 树
- [ ] 按轮次查看注入快照（模型实际所见）
- [ ] token/耗时统计与 Span 属性一致
- [ ] Skill 管理页可上架/下架/编辑 DB Skill
