# impl 1409 — EvictionThresholdGate 驱逐阈值门（R9 = effort #1808 / spec 1808 / T2817-T2818）

**What**：`EvictionThresholdGate`（buzhou-spill 静态纯函数）——Thresholds
(soft, hard) 契约构造 + decide 三态裁决（BELOW/GRACE_PENDING/EVICT_NOW，
硬阈不受宽限豁免、软阈看 millisAboveSoft≥grace）+ census 多信号三态普查
（DecisionCensus + evictRatio -1 哨兵）。

**Why**：K8s eviction manager 思想——单一阈值在临界点抖动驱逐；软阈+宽限
给宿主自救窗（自发回收），硬阈保命立即逐；两级裁决让自动驱逐既不灵敏
抖动也不迟钝盘满。

**Verify**：`EvictionThresholdGateTest` 4 用例全绿。

**Status**：done（2026-09-16）
