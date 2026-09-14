# 1118 — 降级存储契约对齐 + 机制计数（M 系 R17）

**What to build:** redis DegradingObservabilityStore 补 degrade 指标 + README 十大机制。

**Blocked by:** T2281 / T2282（同轮 shape+verify）。

**Status:** done

- [x] redis 版 runDegradable 补 buzhou.store.write.failures{policy=degrade}
- [x] README 九大→十大（三处措辞 + 表加第 10 行模型韧性层）
- [x] RedisWriteFailurePolicyTest 2 用例零回归

## Done

验证：定向测试绿。commit 见本轮 fix 提交。
