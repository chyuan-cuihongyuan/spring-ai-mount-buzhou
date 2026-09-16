# Spec 1873 — 调度松弛量（effort #1873，R74）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2947–T2948，impl 1474）。借鉴：
> 项目管理 CPM 松弛量（float/slack）——任务最晚可延迟多久不拖总工期；
> 关键任务 float=0、非关键有缓冲可让路。

## Problem Statement

关键路径（1867）给了总下界与关键终点，但没给**逐任务缓冲量**：非关键
 任务能延多久不拖总长（削峰让路的依据）、关键任务是不是真的一毫秒都
 动不得——float 面缺位则并行削峰凭感觉。

## Solution

`ScheduleFloat`（core/exec，静态纯函数，与 CriticalPathLength 配对）：

- `floats(tasks, dependencies)` → 逐任务 `TaskFloat(id, floatMillis,
  earliestStartMillis)`；
- 语义：正向 ES/EF DP + 反向到汇最长距 DP；LS = T − toSink − dur；
  float = LS − ES（关键任务 0）；
- 契约：端点在任务集、无环（备忘递归 visiting 集检测）、无重复
 （fail-fast）；null 任一按空。

## User Stories

1. 作为编排作者，c/d 各有 85ms 缓冲——削峰时延后它们，a/b 关键链独占
   资源。
2. 作为容量作者，float 分布（多少任务零浮动）= 编排刚性度读数。
3. 作为框架宿主，任务口径自声明，纯计算不排程。

## Implementation Decisions

- 备忘递归双向 DP（任务图小——诚实边界同 CPM 轮）；visiting 集环检测。

## Testing Decisions

- 关键链零浮动+旁支缓冲（c/d 各 85——首跑红为心算期望误，R54 病理
  第五次实证，按实现口径推倒重算）；串行全关键；空/环/端点缺失/重复
  fail-fast。

## Out of Scope

- 不做资源约束调度（RCPSP 归未来静脉）；不执行排程。

## Further Notes

- 与 CriticalPathLength 配对成 CPM 双面：下界+终点 / 逐任务缓冲。
