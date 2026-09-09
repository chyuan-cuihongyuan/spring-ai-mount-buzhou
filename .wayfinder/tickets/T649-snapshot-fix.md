---
Type: task
Status: closed
---
## Question

ApiSurfaceSnapshotTest 跨平台修复（normalizedClasspath/splitClasspath 助手
三处共用、relativize 反斜杠归一）+ regenerateSnapshot 全量再生（+38 型）。

## Resolution

done（2026-09-02）：impl-352；比对测试 Windows reactor 真跑（Skipped: 0）
且绿；diff 恰 38 行新增人工核对入档。
