---
Type: task
Status: open
blocked-by:
---
## Question

性能基准怎么做？现状：零 JMH/计时回归。决策点：轻量计时 harness（不引 JMH——需独立模块+ shade，成本高；用简单 warmup+分位数计时测试，标记 @Tag(perf) 默认 CI 跳过 nightly 跑？）、基准场景（微压缩吞吐、spill 回读延迟、100 轮会话端到端 P50/P95）、阈值（观测不卡门，报告落档 docs/perf/）、防回归解读规则。产出 spec 21 增量 + impl 68。
