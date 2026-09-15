# impl 1310 — EventGapDetector 会话事件时间间隙检测（R11 = effort #1710 / spec 1710 / T2621-T2622）

**What**：静态纯函数 analyze(timestamps, threshold)→GapReport(gapCount/largestGap)；严格大于；<2 哨兵。
**Why**：事件流时间维断流显形（Flink event-time gap 思想），序维之外补时维。
**Verify**：EventGapDetectorTest 4 断言全绿。 **Status**：done（2026-09-15）
