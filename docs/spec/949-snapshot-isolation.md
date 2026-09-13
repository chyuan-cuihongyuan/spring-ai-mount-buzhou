# 949 — 快照数据集隔离性深验

> 来源：I 会话第 48 轮 = effort #949（[T1333](../../.wayfinder/tickets/T1333-snapshot-isolation-shape.md) / [T1334](../../.wayfinder/tickets/T1334-snapshot-isolation-verify.md) / impl 697）。薄加固轮（G 深验模式）。

## 背景

`snapshotDataset`（spec 187）三断言未固化：① 快照是内容拷贝非活视图（源变靶不变）；② 独立生命周期（删源靶活）；③ nextItemId 继承（拷贝项 id 最大值续起，新 addItem 不碰撞）。

## 目标

`SnapshotIsolationDeepTest` 三场景 + 既有语义零变化回归（快照目标已存在 fail-fast 口径保留）。

## 兼容性

纯测试轮；零生产代码变更预期。
