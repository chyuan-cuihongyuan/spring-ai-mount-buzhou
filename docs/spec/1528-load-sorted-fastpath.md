# 1528 — InMemoryMessageStore.load 已序免排序快路径

> 来源：M 会话第 32 轮 = effort #1528（impl 1131）。

## 背景

load 每次调用全量排序 O(n log n)——它是每轮模型调用的热路径（历史注入），正常追加天然有序，长会话退化点。

## 目标

O(n) isSorted 检查通过 → List.copyOf 快照（免排序）；乱序回退原全排序。快照语义保持。

## 兼容性

等值优化零语义变化（契约测试零回归）。
