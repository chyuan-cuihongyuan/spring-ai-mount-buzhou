# impl 1323 — RepairOutcomeStats 悬挂修复动作结果普查（R24 = effort #1723 / spec 1723 / T2647-T2648）

**What**：Action 三闭集（REPLAYED/MARKED_FAILED/SKIPPED_GONE）+census+replayRatio（无样本 −1）+resetForTest。
**Why**：k8s events 自愈动作审计——恢复域健康画像（与 DanglingTurnDetector 互补）。
**Verify**：RepairOutcomeStatsTest 3 断言全绿。 **Status**：done（2026-09-15）
