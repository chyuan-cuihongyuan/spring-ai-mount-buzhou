---
id: T1568
title: 崩循环探测器类级水位读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1567
created: 2026-09-15
---

## Question

J 会话第 56 轮：CrashLoopWatchStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（CrashLoopWatchStatsTest）：正常记录 → opensRecorded 增；构造 33 模型压满封顶 → opensTruncated=1；窗口内连发达 minOpens → loopsDetected=1；recovery 后再次成环 → loopsDetected=2（重复周期计数）；null/空白模型不落任何桶；resetForTest 归零。定向 `mvn -pl buzhou-resilience -am test -Dtest='CrashLoopWatchStatsTest'` 绿 + 既有 CircuitCrashLoopDetector 回归绿。
