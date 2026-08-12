# 05 — 装配 + 文档收口: autoconfig 配置绑定 + docs/spec 同步

**What to build:** 装配缝合点收口与文档交付。`ApplicationContextRunner` 测试（复用 `BuzhouCoreAutoConfigurationTest` / `BuzhouResilienceAutoConfigurationTest` 形态）：`buzhou.backpressure.*` 与 `buzhou.resilience.rate-limit.*` 全量配置绑定断言、缺省 null=不限断言、脊柱参数改由配置驱动（经 runtime 行为断言而非反射读字段）。机制详设文档落盘并同步 docs/spec（「改机制先改 Spec」）：三维挂点、过载两档契约、配置项全表、事件类型全表；「与动态预算的区分」章节（动态预算管单会话上下文窗口怎么分，本机制管跨会话速率与并发，正交不重叠）；「每实例配额折算」章节（单进程内存语义，总配额/实例数由配置表达，多实例部署如实文档化）；TPM 诚实边界写明（事后记账+下次预检=平均速率保护，不防单尖峰）；safe-by-default 审计收口（阈值默认 null=不限，显式配置才生效）；M2 预留位写明（11 预算闸门共享计数形态、21 按租户分桶、AIMD 留项、与 08 收敛失控防护家族统一形态）；路线图 M1 行勾选 07 项。

**Blocked by:** 02、03、04（全部机制切片收口后）

**Status:** ready-for-agent

- [ ] `ApplicationContextRunner` 测试：两前缀全量配置绑定 + 缺省 null=不限 + 配置驱动脊柱行为断言
- [ ] 机制详设文档落盘，与实现一致（三维挂点 / 两档契约 / 配置项全表 / 事件类型全表）
- [ ] docs/spec 同步（09 模块工程 + resilience 机制章节按需更新）
- [ ] 「与动态预算的区分」「每实例配额折算」「TPM 平均速率边界」三章节写明
- [ ] 路线图 M1 行勾选 07 项
