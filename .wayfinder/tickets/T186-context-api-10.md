---
Type: task
Status: closed
blocked-by: T170-ttft-tpot-metrics.md, T171-stream-cancel-cumcap.md, T172-mdc-correlation.md, T173-turn-feedback.md, T174-feedback-export.md, T175-weighted-canary.md, T176-shadow-fork.md, T177-model-pool-quota.md, T178-error-codes.md, T179-backoff-jitter.md, T180-unsubscribed-stream.md
---
## Question

术语节（TTFT/TPOT、反馈、金丝雀、shadow、池配额等）+ effort#10 公共面（含错误码收口的破坏性变更入档）。

## Resolution

impl-154 落地：CONTEXT.md 新增「运营可观测与流量治理（effort #10）」术语节 12 条；api-surface.md
新增 effort#10 公共面节（observability/core/resilience 三模块）；破坏性变更 3 处入档——错误码
挂码迁移（spill IO/技能管理/todo/SHA 的 catch 面跟进）、Fallback 与 ResilienceProperties
canonical 构造组件数增加（兼容构造保留）。T186 关闭。
