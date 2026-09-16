# Spec 2062 — 回绕序号比较（effort #2062，R63）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3225–T3226，impl 1613）。
> 借鉴：TCP sequence number 回绕比较——有符号差序。

## Problem Statement

long 序号（投递序号 / 事件纪元 / 日志偏移）长期运行回绕（MAX→MIN）
后，朴素 a < b 在回绕点反转——新序号被判旧（MIN < MAX 为 true 的
病），乱序检测/去重窗/序号围栏在回绕点集体失灵且极难复现。

## Solution

`SequenceOrder`（core/recovery，纯函数零状态）：

- `compare(a, b)` 三态：有符号差 b−a（溢出回绕即语义——差符号在
  回绕点仍正确）；**|diff| ≥ 安全半环（2⁶²）即 INCOMPARABLE**
 （相距太远真序未知——诚实不臆答）；等值 INCOMPARABLE（同序不分
  先后）；
- `isBefore/isAfter` 便捷；`forwardDistance(a, b)` 距离折算到
  [−半环,+半环] 邻域（回绕邻域 MAX→MIN 距离 1；不可分域
  Long.MIN_VALUE 哨兵）。

## User Stories

1. 作为投递序号作者，回绕点前后判序不反转——世纪级运行不埋雷。
2. 作为去重窗作者，forwardDistance 折算距离——「多远之前」回绕安
   全。

## Testing Decisions

- 普通序；回绕点（朴素病证 MIN<MAX=true vs 回绕安全 isBefore(MIN,
  MAX)=false）+ 邻域小步；半环界（内 BEFORE 外 INCOMPARABLE）；等
  值；距离折算（回绕距离 1/哨兵）；反向回绕。

## Out of Scope

- 不做 64 位无符号全域比较（半环诚实口径）；不接投递围栏（#108 迁
  移归后续轮）。

## Further Notes

- 投递序列号围栏（#108）的比较可迁移本件（收敛方向）。
