# Spec 1831 — 对冲延迟策略（effort #1831，R32）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2863–T2864，impl 1432）。借鉴：
> Google《The Tail at Scale》hedged requests——等到 P95 仍未返回才发对冲，
> 只在尾部等待付双倍钱，中位数零对冲成本。

## Problem Statement

HedgedChatModel 执行对冲但延迟阈值无策略面：立即双发是双倍负载，固定延迟
又是魔法数——「等多久才值得发对冲」应从延迟分布推导（P95），样本不足时
保守退守。

## Solution

`HedgeDelayPolicy`（buzhou-resilience，静态纯函数）：

- `hedgeThresholdMillis(samples, percentile, floorMillis, minSamples)`：
  样本充足取最近秩分位（nearest-rank，确定性无插值），不足退守地板值
 （无数据不冒进）；默认 P95/地板 10ms/最小样本 20 常量；
- `decide(elapsedMillis, thresholdMillis)` → `WAIT / SEND_HEDGE`（边界含
 ——到点就发不再观望）。

## User Stories

1. 作为延迟治理者，P95=380ms → 380ms 内绝不双发，之后才对冲——尾部
   5% 才付双倍钱。
2. 作为容量守护者，样本不足自动退守地板——冷启动不冒进双倍负载。
3. 作为框架宿主，延迟样本口径自声明，纯裁决零执行。

## Implementation Decisions

- 纯裁决不执行（对冲动作归宿主）；样本校验先于退守（负样本任何路径
  都拦——初版退守路径漏检自查修正）。
- fail-fast：空样本、分位越界、负地板/样本/裁决入参。

## Testing Decisions

- 最近秩 P95（40 样本 rank 38 → 380）；退守+乱序容忍；边界含上；畸形
  五型 fail-fast。

## Out of Scope

- 不执行对冲；不做分位随窗口自适应滚动（归窗口族读面）。

## Further Notes

- 与 HedgedChatModel 配对：那是执行器，这是「何时对冲」的策略面。
