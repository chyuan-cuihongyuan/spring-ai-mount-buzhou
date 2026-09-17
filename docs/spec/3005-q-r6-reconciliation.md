# Spec 3005 — Q 会话 R6 对账轮（effort #3005，R6）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5011–T5012，impl 2006）。
> R6k 对账轮第一例（Wave 1 收口）。

## Problem Statement

Wave 1（R2–R5）新增 4 个公共类型（WelfordAccumulator / DisjointSet /
EdfScheduler / NucleusSampler）未入快照——快照门在全仓 verify 必红。

## Solution

R6k 同款四件套：快照 1056→1060（+4 全 Q 系，reactor 全量 regenerate
防单模块扫描集不全写坏快照）+ api-surface.md 四行 + CONTEXT 955→959
+ 全仓 mvn verify 三门绿（覆盖门/快照门/Q 对账门）+ push（含网络
抖动积压补推）。

## Further Notes

- 里程碑：6/150（4%）。Wave 1 含一处收尾修复（NucleusSampler 全 −∞
  NaN 守卫，见 impl 2005）——管道掩码退出码教训已入档。
