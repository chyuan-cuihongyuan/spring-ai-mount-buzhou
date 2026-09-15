# impl 1318 — HookCancelStats 钩子取消面统计（R19 = effort #1718 / spec 1718 / T2637-T2638）

**What**：observed/cancelledSkipped/completed 三计数+cancelRatio（无样本 −1）+resetForTest。
**Why**：OTel exporter 取消路径遥测——钩子缺失归因（取消 vs 未注册）。
**Verify**：HookCancelStatsTest 3 断言全绿。 **Status**：done（2026-09-15）
