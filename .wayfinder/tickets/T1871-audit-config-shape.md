---
id: T1871
title: R29 选题——GuardAuditConfig fromGuardMap 解析全分支补测（80 missed 集中区）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 29 轮：批次 5 候选中 GuardAuditConfig（branch 80 missed，fromGuardMap 子 Map 解析集中区）如何定向补测？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 29 轮 = effort #1228 / spec 1228 / impl 931）：

1. **补测面（8 用例）**：null guardMap 默认回退；非 Map audit 值回退；全字段往返（trim+lower/容量/min-verify/key-dir/keys）；blank store 与非法容量回退；负 min-verify-version 回退零；blank key-dir 忽略；非法 KeyFile 过滤（version≤0/路径缺失/blank）；public-key-path 可选缺省 null。
2. **形态**：纯函数 Map 驱动（无 Mockito）——config 域解析测试先例（ConfigMaps）。
3. **边界**：不改主代码；Jcs（84 missed）等批次 6 候选留后续。
