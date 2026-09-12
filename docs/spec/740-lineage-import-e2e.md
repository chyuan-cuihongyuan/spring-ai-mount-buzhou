# 740 — 谱系游走导入场景深链补验

> 来源：G 会话第 41 轮 = effort #741（spec 711 补验）/ [T1031](../../.wayfinder/tickets/T1031-lineage-import-e2e-shape.md) / [T1032](../../.wayfinder/tickets/T1032-lineage-import-e2e-verify.md) / impl 543。

## 背景

ForkLineageWalker 环/深度单态已验——「导入注入超深链+尾部环」复合场景与默认深度 64 的组合行为未闭环。

## 目标（测试域补验轮）

- 100 节点链（>默认 64）+ 尾部环：默认深度 walk → depthCapped 截断不 OOM；
- 显式深度 200 → 走到环处 loopDetected；
- 键统一 SessionForkKeys（漂移即断纪律回归）。

## 兼容性

纯测试域增量。
