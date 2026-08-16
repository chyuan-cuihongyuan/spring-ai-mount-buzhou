# 154 — CONTEXT/api-surface 增补（effort#10）

**Parent:** spec 46–51 面 / [T186](../tickets/T186-context-api-10.md)

**Status:** done

- [x] CONTEXT.md 新术语节「运营可观测与流量治理（effort #10）」12 条（TTFT/TPOT、流取消三路分类、
  慢滴流累计上限、MDC 关联、rateTurn、反馈导出、加权金丝雀、shadow 探测、池配额、错误码收口、
  退避 jitter、未订阅流惰性化）
- [x] api-surface.md 新增「effort #10 新增公共面」节：observability（TTFT/TPOT timer）、core
  （rateTurn/FeedbackExporter/STREAM_FIRST_TOKEN/取消计数/stream-total-timeout/ErrorCode 3 新码）、
  resilience（Fallback 2 新组件/Shadow 组/ShadowTrafficController/ModelRateLimiter）
- [x] 破坏性变更入档 3 处：错误码挂码迁移（调用方 catch 面跟进）、Fallback/ResilienceProperties
  canonical 构造组件数增加（兼容构造保留）
