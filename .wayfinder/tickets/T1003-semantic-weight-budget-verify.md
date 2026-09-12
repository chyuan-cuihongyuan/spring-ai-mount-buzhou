---
id: T1003
title: 语义缓存权重预算驱逐验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1002]
created: 2026-09-12
---

## Question

权重驱逐正确性如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 2 轮 = effort #701）：四用例——①腾挪序（预算 100 写 60+60 → eldest 被逐、weightEvictions=1、totalWeight=60）；②超预算拒存（预算 50 写 80 → 不存+计数）；③默认 0 零行为（不腾挪，size 仍 maxEntries 封顶）；④readout 一致性（totalWeightChars 随写/逐精确增减）+ yml 装配冒烟。buzhou-resilience 全模块零回归（C 会话排除集）。
