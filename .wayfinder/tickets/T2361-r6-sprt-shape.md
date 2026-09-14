---
id: T2361
title: R6 A/B 评估 SPRT 序贯提前终止的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2360
created: 2026-09-15
---

## Question

N 会话第 6 轮：评估成本控制——预算硬截断（spec 520）还是证据驱动提前终止？

## Resolution

选 **SPRT 证据驱动提前终止**（Wald 序贯概率比检验）。预算截断截到哪算哪无方向
结论；SPRT 在 LLR 越界时停且带 α/β 显著性保证——压倒性优势第 5 项即停。实现要点：
① 方向分离（MLE 双侧极端不符号修正则 B 全盛会算成 A 优——数学错误测试抓出）；
② skipped 与 error 桶诚实分离；③ 平局不进符号检验分母。
