# Spec 3040 — 时间戳锁仲裁器（effort #3040，R41）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5081–T5082，impl 2040）。
> 借鉴：Rosenkrantz 1978 wait-die / wound-wait（死锁预防）。

## Problem Statement

多事务/多 agent 资源互锁（A 等 B、B 等 A）事后检测（Tarjan 指认
环）已晚——重启代价高；需要**事前不发生**的预防口径。

## Solution

`TimestampLockArbiter`（core/concurrent，纯仲裁无阻塞——等待/重启
动作归调用方）：

- 年长者特权（(timestamp, txnId) 字典序小者老——同钟并列可全序）：
  - **wait-die 老等少死**：老者请求被少者持有的锁 → WAIT；少者
    请求被老者持有的锁 → REQUESTER_ABORTS（自杀重启）；
  - **wound-wait 老伤少等**：老者请求 → GRANTED 且持有者被伤
    （woundedTxn 指认、锁易主）；少者 → WAIT；
- 两模式等待图恒无环（死锁预防而非检测）；空闲即授；同持有者
  幂等；release（非持有者无副作用）；holds/holderOf 读数。

## User Stories

1. 作为并发作者，资源互锁事前免疫——重启方被明确指认。
2. 作为排障作者，woundedTxn 直接指认受害者——免事后追环。

## Testing Decisions

- 空闲即授+持有读数；wait-die 双向（老 WAIT 持有者不动/少
  ABORTS 伤自身）；wound-wait 双向（老 GRANTED 伤持有者+被伤者
  重启后再请求只能等/少 WAIT 无伤）；释放非持有者无副作用+再授；
  同 txn 幂等；同钟并列 txnId 决（3 WAIT/20 ABORTS）；null mode/
  resource fail-fast。

## Out of Scope

- 不做真阻塞/条件变量（等待动作归调用方——本件纯裁决）；不做
  多资源一次性获取（原子性归调用方排序）；不做锁队列公平性。

## Further Notes

- 与 TarjanSccFinder 成对：本件事前预防（不发生环）、彼件事后
  指认（环成员定位）——防线互补。
- 里程碑：41/150。
