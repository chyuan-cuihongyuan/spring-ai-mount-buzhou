---
id: T2307
title: InMemoryMessageStore.load 已序免排序快路径的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 32 轮：内存消息存储 load 的每调全量排序（O(n log n)）热路径退化如何修？

## Resolution

**用户常设授权 AFK（可推翻）**

load 是每轮模型调用的热路径（历史注入），正常轮次推进下追加天然有序（turnSeq/seqInTurn 单调）——O(n) isSorted 检查通过则 List.copyOf 快照返回（免 O(n log n) 排序，长会话退化点消除）；乱序（time-travel fork/恢复序）回退原全排序（语义零变化）。快照语义保持（copyOf，不暴露活视图）。
