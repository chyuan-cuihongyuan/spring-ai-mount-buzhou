---
Type: task
Status: closed
blocked-by: T170-ttft-tpot-metrics.md, T173-turn-feedback.md, T175-weighted-canary.md, T177-model-pool-quota.md
---
## Question

TTFT 劣化排查 / 池配额调优与 remaining 告警 / 反馈运营（负反馈回流评估）/ 金丝雀与 shadow 运维步骤（含 shadow 预算池管控）。

## Resolution

runbook 第六轮落地：§2 症状树 +4 行（流累计超限/取消占比拆解/rateTurn 拒绝/候选限流跳过）；
§7 告警 +5 项（ttft/tpot/stream.cancelled{deadline}/shadow error/skipped-budget）；新增 §8
「流量治理与反馈运营」——金丝雀配置与漂移变更语义、shadow 配置/预算/信任前提、反馈运营与
MDC logback pattern 示例。T185 关闭。
