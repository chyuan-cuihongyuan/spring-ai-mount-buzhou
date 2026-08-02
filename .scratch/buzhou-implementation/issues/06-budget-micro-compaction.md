# 06 — 动态预算与微压缩

**What to build:** 注入视图单一管线上线：字符启发式估算器+TokenEstimator SPI、先扣后算动态预算（0.90 阈值可配、摘要计入、Schema 缓存）；微压缩以完结轮次为原子单位（neverCompress/maxAgeTurns/minSizeChars/protectRecentTurns 策略并入四层配置）、占位符带 evidence-id、统一证据回查工具可用。

**Blocked by:** 03

**Status:** ready-for-agent

- [ ] 预算计算对固定开销变化（系统提示/工具数/长输入）有参数化测试
- [ ] 20 轮工具会话微压缩后 token 释放量可测，占位符含 evidence-id
- [ ] 未完结轮次的工具结果绝不被压缩有测试
- [ ] 证据回查工具按 evidence-id 取回原文（含范围读取共享能力）
