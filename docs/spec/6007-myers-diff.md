# Spec 6007 — Myers O(ND) Diff（effort #6007，T8）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6215–T6216，impl 2208）。
> 借鉴：Myers O(ND) 差分论文思想（git diff 同源）。

## Problem Statement

序列差异的病：全量替换（差异无结构不可审）、朴素全量 DP
O(N·M) 时空（大文件不可行）——**最短编辑脚本面**缺失。

## Solution

`MyersDiff`（core/metrics，行级序列）：

- 贪心 O(ND)：D 为编辑距离——外层步数 d、对角线 k 的
  V 数组 + 轨迹快照回溯——脚本长度 = 最优（N+M−2·LCS）；
- Edit(EQUAL/INSERT/DELETE, text) 三态脚本；tie-break 定构
 （k=−d 先降后删）——同输入同脚本；
- fail-fast：null 序列。

## User Stories

1. 作为审计作者，两版配置差异以最短脚本呈现——最小审查面。
2. 作为回放作者，脚本可逆向应用（a→b）——差异可执行。

## Testing Decisions

- 混合操作脚本应用于 a 恒得 b + EQUAL 段双侧取值一致
 （脚本可执行性）；200 对随机小序列（字母表 3、长 ≤8）
 脚本长度 = N+M−2·LCS（经典 DP oracle 最优性）；空侧/
 全等边界；fail-fast。

## Out of Scope

- 不做语义清理（shift-boundary 启发式）；不做三方合并
 （ThreeWayMerge 的面）；不做流式增量。

## Further Notes

- 与 ThreeWayMerge（spec 4042）同族不同面：双侧 diff 底座
  vs 三方归一冲突显形；与 LcsBits 不同面：只算距离 vs
  产出可执行脚本。
- 里程碑：T8/50（16%）。
