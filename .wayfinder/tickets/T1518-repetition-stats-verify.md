---
id: T1518
title: 打转检测触发聚合读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1517
created: 2026-09-14
---

## Question

J 会话第 33 轮：触发聚合读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（RepetitionStatsTest，ModelCallContext shim 复制 + 相同/相异输出喂入）：window=3 相同输出三条 → 第 3 条 fire（fires=1、maxRunSeen=3、blocks=0——observe-only）；unstick=true window=2 → 第 2 条 block 且 blocks=1；相异输出不 fire；maxRunSeen 峰值保持。定向 `mvn -pl buzhou-core test -Dtest='RepetitionStatsTest,RepetitionDetectorHookTest'` 绿（后者若存在）。
