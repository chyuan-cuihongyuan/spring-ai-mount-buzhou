# Spec 2006 — 最后写入胜利寄存器（effort #2006，R7）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3113–T3114，impl 1557）。
> 借鉴：Dynamo/CRDT LWW——多写者无协调收敛，(timestamp, writerId)
> 字典序平局确定性仲裁。

## Problem Statement

多写者并发更新同一值（共享事实 / 配置热更新 / 会话元数据），无全局
锁协调时谁赢？本地时钟不可比（偏移），随机仲裁不可回放，迟到旧写
悄悄覆盖新值无人知晓。

## Solution

`LastWriteWinsRegister<T>`（core/concurrent，synchronized 小临界区）：

- `put(value, ts, writerId)`：(ts, writer) 字典序 > 当前采纳 / < 拒
  （迟到旧写 superseded 计数）/ ts 平局 writer 字典序仲裁（Dynamo
  惯例——确定性无随机）/ 完全相等幂等 / 同 writer 同 ts 异值不可判
  定 first-wins + conflict 计数；
- `merge(Timestamped)` 跨实例同语义（LWW 收敛保证：双向 merge 后
  终值一致）；
- 读数：current()（含版本口径）/ conflictCount（平局对账面）/
  supersededCount（乱序到达对账面）；
- 契约：value/writerId 非空、ts ≥ 0 fail-fast。

## User Stories

1. 作为共享事实作者，多实例并发写无需协调——merge 后各端收敛同值。
2. 作为对账者，superseded 计数 = 乱序网络频率；conflict 计数 = 时钟
   平局频率——时钟质量诊断有据。

## Implementation Decisions

- 平局仲裁 writerId 字典序（非 hash——可读可回放）；不引向量时钟
   （重 CRDT 语义留白）。

## Testing Decisions

- 单调采纳；迟到拒+计数；平局字典序仲裁+冲突计数；幂等零计数；同
  writer 同 ts 异值 first-wins；merge 双向收敛；空寄存器 null；畸形
  五型 fail-fast。

## Out of Scope

- 不做多值集（OR-Set）；不接具体存储（SPI 桥接归后续轮）。

## Further Notes

- 与向量时钟偏序（O 系 VectorClock）互补：LWW 轻量定序，向量时钟
  因果可溯。
