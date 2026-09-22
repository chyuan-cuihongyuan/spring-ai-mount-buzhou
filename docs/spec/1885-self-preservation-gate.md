# Spec 1885 — 自保模式门（effort #1885，R86）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2971–T2972，impl 1486）。借鉴：
> Netflix Eureka（12K+ 星）self-preservation 语义——续约率跌破阈值
> 时「宁保数据不逐实例」：停止过期剔除（网络分区时逐掉全部实例的
> 自杀防护），恢复后自动退出。

## Problem Statement

注册中心/租约表的心跳过期剔除拍脑袋：分区期间所有心跳同时断，
朴素剔除会把健康实例全部清场（重启风暴）；「大面积失联 = 自己
瞎了而不是全死了」的自保语义没有独立判定面。

## Solution

`SelfPreservationGate`（core/concurrent，轻量持态门）：

- `onRenewal()`：续约计数（滑动计数器口径）；
- `renewalRatio()`：当前续约数/期望续约数读数；
- `shouldExpire()`：续约率 < 阈值 → false（自保停逐），≥ 阈值 →
  true（正常逐）——剔除前必查；
- `selfPreserving()`：自保态读数（可观测面）。

## User Stories

1. 作为注册中心作者，20 实例 15% 心跳在途 → ratio 0.15 < 0.15
   阈值边界 → 停逐——分区不清场。
2. 作为恢复观察者，心跳回升 ratio ≥ 阈值 → 自动退出自保继续逐
   过期——恢复路径对称。
3. 作为评审者，阈值边界（恰等于）按正常逐处理——保守在「宁停
   不误」，恢复不含糊。

## Implementation Decisions

- 持态小门（期望续约数 = 实例数×每分钟心跳，构造注入；当前续约
  计数调用方按窗口喂入）；实例数 ≥ 1、阈值 ∈ (0,1) fail-fast。

## Testing Decisions

- 自保触发（比率低于阈值停逐）/恢复（达标续逐）两态；边界恰等
  正常逐；可观测读数两例；畸形三型 fail-fast。

## Out of Scope

- 不做真实心跳收发与租约存储（归租约面）；不做通知广播。

## Further Notes

- 与 PhiAccrualFailureDetector（单实例失联怀疑度）互补：那是逐点
  判死，这是全局熔断剔除的配额语义。
