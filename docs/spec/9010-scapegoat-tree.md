# Spec 9010 — Scapegoat Tree 替罪羊树（effort #9010，W11）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9021–W9022，impl 2363）。
> 借鉴：Scapegoat tree（Galperin-Rivest 1993 SODA——重建式自平衡第三条路）

## Problem Statement

旋转式平衡的病：不变量繁多、元数据维护与旋转
路径复杂——**推平重建**：深度超 log_{1/α}n 找最高
α 失衡祖先整子树重建为完美平衡。

## Solution

ScapegoatTree（core/concurrent，实例类）：insert/
remove/contains/size/inOrder/height（同包测试面）；
α=0.75；删除掉半高水线全树重建；确定。

## Testing Decisions

500 顺序插入 α 界（24 vs 朴素 500 链）；
4000 随机操作 vs TreeSet 圣像；删后平衡；确定性；
逐删到空。

## Out of Scope

不做映射（集语义）；不做范围查询；不做并发
（concurrent 包定位为结构族——并发语义归上游）。

## Further Notes

与 SplayTree（6001）/Treap（6002）同域不同面。
开发期勘误入档：替罪羊取最高失衡祖先（链场景治本）
+编译失败静默跑旧 class 的流程教训。Wave 2 收束件。
