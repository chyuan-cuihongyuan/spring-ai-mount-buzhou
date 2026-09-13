# 830 — 审计树形健康读数

> 来源：H 会话第 31 轮 = effort #830 / [T1161](../../.wayfinder/tickets/T1161-audit-tree-health.md) / [T1162](../../.wayfinder/tickets/T1162-audit-tree-health-verify.md) / impl 583。
> 借鉴：Certificate Transparency 树语义（CT 思想扩散）。

## Problem

审计 Merkle 树的形状（深度/满树程度）决定证明路径长度与批量验证成本——「树是不是矮胖健康」无量化面。

## Solution

`AuditTreeHealthReadout`（guard.audit，纯函数）：analyze(leafCount) → 深度（⌈log2 n⌉）/nextPow2/补位叶/满树判定；0=空树；负数归 0。

## 兼容性

纯新增静态工具（叶数由调用方采集——零侵入）。

## 诚实边界

纯形状不校验内容；补位叶是数学概念（实现填充语义归树本身）。
