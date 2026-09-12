---
id: T1013
title: PII 假名化模式装配的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

PiiDetector.pseudonymize（spec 713）只有方法面——GuardModule 装配链不可达；且三缝 hook 各自 new PiiDetector()，模式无法贯穿。

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 32 轮 = effort #731 / spec 731 / impl 534）：PiiDetector 增 formatPreserving 模式构造（true 时 redact 分派 pseudonymize——模式内聚 detector，三缝共用实例即自然一致）；PiiRedactionHook/PiiInputRedactionHook 增 3 参构造（types, customRules, formatPreserving）；Builder.piiPreserveFormat() 直通——顺带把四层三元树收敛为直构造（null 兼容构造保证等价，既有用例守护）。
