# 734 — PDB×空闲压缩联动补验

> 来源：G 会话第 35 轮 = effort #734（spec 704/706 联动补验）/ [T1019](../../.wayfinder/tickets/T1019-pdb-idle-e2e-shape.md) / [T1020](../../.wayfinder/tickets/T1020-pdb-idle-e2e-verify.md) / impl 537。

## 背景

IdleSessionMonitor（spec 179，空闲清单=归档候选）与 SessionAvailabilityFloor（spec 704，归档闸）分属两轮——「候选→闸」的联动语义未闭环。

## 目标（测试域补验轮）

- sweep 产出的空闲候选在水位不足时被 floor 逐个拒绝（archived 不增长）；
- 水位恢复（会话重新活跃/供应商计数上升）后候选可归档；
- 拒绝路径 pdb-rejected 计数与候选数一致。

## 兼容性

纯测试域增量。
