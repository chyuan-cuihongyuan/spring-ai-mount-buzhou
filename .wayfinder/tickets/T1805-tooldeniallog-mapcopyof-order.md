---
id: T1805
title: ToolDenialLog topDenials 排序被 Map.copyOf 破坏——并列稳定性失效
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-14
---

## Question

R1 全量 verify 显形：ToolDenialLogStabilityTest.tiesSortedByRoleThenTool 在主干确定性失败（与 R1 补测改动无关的既有红）。根因与修复形态？

## Resolution

**用户常设授权 AFK（可推翻）**

根因：`topDenials()`（commit 3cb64152 / G 会话 effort#737 引入）排序逻辑正确（count 降序 + (role->tool) 键字典序 tie-break，落入 LinkedHashMap），但返回前经 `Map.copyOf(out)`——`Map.copyOf` 返回 ImmutableCollections.MapN，**不保迭代序**，把排序结果按哈希表布局重排，稳定性契约（Javadoc「并列按键字典序——稳定」）静默失效。该测试随同轮提交后长期绿是哈希布局巧合；本机 JDK 升级后 MapN 探测序变化，确定性翻红（代码与测试均无改动，`git log` 实证）。

修复（一词）：`Map.copyOf(out)` → `Collections.unmodifiableMap(out)`——保 LinkedHashMap 序且不可变语义不变。教训入档：**有序快照返回禁用 Map.copyOf**（Set/Map 不可变工厂不保序，有序契约必须走 unmodifiable 包装）。
