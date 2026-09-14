---
id: T1331
title: pass@k×防抖门组合补验的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 52 轮：spec 902 pass@k 无偏估计与 spec 943 防抖门的组合语义（「pass@k 达标但单次 passRate 不达」场景）是否有验证缺口？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 52 轮 = effort #953 / spec 953 / impl 694 续）：组合补验成立（薄测试轮）。`PassAtKGateComboTest`：① 构造「单次 passRate 0.4 不达 0.5 门，但 pass@2 无偏 0.64 达标」的数据——两口径结论相反的组合语义实证（902 的概率口径 vs 943/80 的频率口径互补不矛盾）；② enforceStable k 次历史喂 SummaryVersionAudit 式对账（跨组件纯函数复用一致性）。
