---
id: T1124
title: 断路器 crash-loop 检测验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1123]
created: 2026-09-13
---

## Question

转闩/闩锁不解/恢复清除/再闩计数如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 12 轮 = effort #811）：CircuitCrashLoopDetectorTest 6 例——3 次 OPEN 转闩+跨窗不解+恢复清零+loopsDetected=1/窗口滑出 opensInWindow=1/恢复后 2 次 OPEN 再闩 loopsDetected=2/模型独立+封顶 truncated/null+典序/fail-fast（minOpens≥2）。教训：首跑测试 minOpens=1 违「反复」语义被构造器正确拒绝。
