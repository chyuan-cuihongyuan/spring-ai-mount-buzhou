---
Type: task
Status: closed
---
## Question

熔断半开多探测（T104 fog）：单探测成功即 CLOSE 在抖动 provider（成功率 50%）下会频繁跳闸循环。half-open 成功阈值（连续 N 探测成功才 CLOSE）是否做？

## Resolution

AFK 自决：做，可配保守。Circuit 增 `halfOpenSuccessThreshold`（默认 1 = 既有单探测行为零变化；>1 时半开放行至多 N 个探测，连续成功 N 次→CLOSED，任一失败→OPEN 重计退避）。探测并发占位沿用既有 probeInFlight 语义扩展为「在飞探测计数 ≤ threshold」。事件 payload 增 halfOpenProgress。产 spec 35 §A + impl-93。

### 闭合细化（实现期定稿）

- 占位闸门不变量：在飞探测数 + 已成功数 ≥ 阈值（每成功永久占一槽，非简单在飞计数）。
- 事件 payload 不增 halfOpenProgress（状态迁移事件已可见；冗余字段无净收益）。
