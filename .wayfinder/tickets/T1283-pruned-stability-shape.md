---
id: T1283
title: pruned×稳定性×gate 联动补验的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 17 轮：spec 901 的 pruned 状态流入 spec 908 稳定性分析 / spec 914 gate 历史时语义是否自洽？联动验证是否有缺口？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 17 轮 = effort #916 / spec 916 / impl 669）：预判缺陷成立——`analyzeK` 的红绿映射只认 fail/error 为红，**pruned（未执行）会被当绿**，污染一致性判定（一真一假的项可能被两个 pruned 淹没成「稳定绿」）。修复（按 G r39 先例）：`analyzeK` 对齐时 pruned 状态视为「该 run 无有效样本」→ 置 null → 既有单侧漂移排除路径自然消化（有效样本 <2 全排除时该项不 compared）；两 run 版 `analyze` 同语义加固（含 pruned 的项不入比对分母）。gate 历史无 pruned 交互（GateDecision 不含项级状态——零变化）。e2e 断言：pruned 混入不产生「假稳定」。
