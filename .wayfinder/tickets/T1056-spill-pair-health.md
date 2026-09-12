---
id: T1056
title: spill 配对健康面接线的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

707 配对审计无健康面——接线吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 29 轮 = effort #728 / spec 728 / impl 628）：`SpillPairHealth` implements BuzhouHealth——mechanism=spill-pair；禁用 UNKNOWN/启用恒 UP（残缺是数据）；details 五项统计；装配随 BuzhouSpillHealthAutoConfiguration。details 全树扫描（低频可接受——诚实边界）。
