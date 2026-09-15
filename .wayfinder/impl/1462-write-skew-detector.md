# impl 1462 — WriteSkewDetector 写偏斜检测（R62 = effort #1861 / spec 1861 / T2923-T2924）

**What**：`WriteSkewDetector`（core/transaction 静态纯函数）——skewRisk
（读交写不交组合判定）+ scan 全对扫描风险对清单；空白 id/null 读写集
fail-fast。

**Why**：快照隔离 write skew 异象（值班医生反例）思想——写冲突检测拦
不住「读同一不变量支撑、各写各行」的组合破坏；风险对清单直接映射
冲突桌（读集加写锁或升级可串行化）。

**Verify**：`WriteSkewDetectorTest` 5 用例全绿。

**Status**：done（2026-09-16）
