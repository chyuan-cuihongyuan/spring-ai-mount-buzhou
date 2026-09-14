# 1615 · 泄漏疑似聚合接线（spec 839 孤类救活）

> 来源：N 会话 R16（effort #1615 / T2381–T2382 / impl 1168）。spec 1611 普查修复
> 第五弹：LeakSuspectAggregator（spec 839）建成即孤——LeakDetectorHolder 三处
> `new ResourceLeakDetector()` 均未挂 listener。

## Solution

- `LeakSuspectHolder.compositeWith(hostListener)`：宿主 listener 与进程级聚合器
  复合（双收；宿主 null = 仅聚合器）。装配处（BuzhouCoreAutoConfiguration 的
  detector 构造）一行替换——检测器行为零变更。
- `LeakSuspectHolder.report()` 静态读出聚合排行。

## Testing Decisions

- `LeakSuspectHolderTest`：复合双收（宿主计数 + 聚合 count/maxAge 排行）、
  null 宿主仅聚合器。回归 LeakSuspectAggregatorTest 4 用例。

## Out of Scope

- 其余 core 读数孤类（EstimatorCalibrationAudit/IdleSessionMonitor 等——每项独立轮）。
