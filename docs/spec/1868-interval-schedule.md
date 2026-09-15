# Spec 1868 — 区间合并与空闲缝隙（effort #1868，R69）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2937–T2938，impl 1469）。借鉴：
> 日历调度惯例（Calendar busy/free）——忙时段合并（9-10 与 10-11 就是
> 9-11），窗口内找空闲档期。

## Problem Statement

turn 时间线上的占用段（工具执行/模型调用/持锁区间）散落重叠：「总忙时
 多少」要人肉拼段、「哪里还能排新任务」要人眼找缝——区间合并与缝隙
 查找是排程的前置基建，缺位则每个排程处手搓。

## Solution

`IntervalSchedule`（core/exec，静态纯函数）：

- `Interval(start, end)` 契约（start ≤ end，零长合法——瞬时占用）；
- `merge(intervals)`：排序扫描——重叠/相邻（next.start ≤ cur.end）/嵌套
  归一取 max end；乱序容忍；
- `gaps(intervals, from, to)`：窗口内空闲缝（首前/区间间/尾后；区间
  越界裁剪到窗口内）。

## User Stories

1. 作为排程作者，占用段合并后总忙时直读、窗口缝即候选档期——新任务
   排哪不再人肉拼。
2. 作为审计者，合并确定性（排序扫描）——忙闲图可回放比对。
3. 作为框架宿主，区间口径（毫秒/轮号）自声明，纯计算不排程。

## Implementation Decisions

- 纯计算；相邻即合并（端点相接无缝——数学上同段）；窗口早退（cursor
  过 to 即返）。

## Testing Decisions

- 合并四形态（重叠/相邻/嵌套/独立+乱序）；缝隙三段全报；全覆盖零缝/
  空占用全窗/越界裁剪；畸形三型 fail-fast。

## Out of Scope

- 不做带权区间调度（WCIS 归未来静脉）；不执行排程。

## Further Notes

- 与 CriticalPathLength 互补：那是 DAG 时长下界，这是时间轴占用面。
