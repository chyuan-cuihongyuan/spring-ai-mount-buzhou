# Spec 1870 — 固定间隔下次触发（effort #1870，R71）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2941–T2942，impl 1471）。借鉴：
> crontab/systemd timer 固定间隔网格语义——触发点钉在 epoch+k×interval
> 网格上，停机不重排（错过就错过，补账不重贴）。

## Problem Statement

周期任务（清理/对账/心跳）的下次触发随手写 `now + interval`：停机重启
后网格漂移（本该 10:00 的任务变成 10:07 起步）、错过的触发是补是跳
 无账可查——网格对齐与补账数没有统一计算面。

## Solution

`NextFireSchedule`（core/exec，静态纯函数）：

- `nextFireMillis(interval, epochStart, now)`：≥ now 的最近网格点
 （含上——恰在格点即「现在到期」；now ≤ epoch 即首触发 epoch）；
- `missedFires(interval, epochStart, lastAcked, now)`：(lastAcked, now]
  内网格点数——上次确认后漏了几次，补账显式。

## User Stories

1. 作为定时任务作者，epoch 钉网格——重启不漂移；now=250 间隔 100 →
   下次 300 而非 350。
2. 作为运维者，missedFires=3 → 停机期间漏三拍，补三拍或跳过有数可依。
3. 作为框架宿主，间隔口径（毫秒）自声明，纯计算不触发。

## Implementation Decisions

- 纯计算；ceil 除法对齐；局部方法误用（Java 不支持）实现期自查修正。

## Testing Decisions

- 网格含上四例（250→300/200→200/201→300/epoch 前后）；补账四例
 （3/0/首格起 3/未到 epoch 0）；畸形三型 fail-fast。

## Out of Scope

- 不做 cron 表达式解析（五段式归未来静脉）；不执行触发。

## Further Notes

- 与 ChaosBudgetGate 的 Window 互补：那是允许窗，这是触发网格。
