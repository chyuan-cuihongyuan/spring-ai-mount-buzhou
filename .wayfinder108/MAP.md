# Wayfinder Map — Buzhou 投递序列号围栏（effort #108，B 会话第 20 轮）

> B 会话第 20 轮。主题池「序列号围栏」：at-least-once 下接收方只能靠幂等键
> 去重，<b>丢包不可见</b>（缺了哪条没人知道）。借鉴 Kafka producer sequence
> numbers + consumer gap detection——单调序号让缺口显形。

## Destination

forwarder 信封加进程内单调 seq（每 forwarder 一个计数器，重启复位=新纪元）+
SequenceFence（接收方围栏：CONTINUE/GAP/DUPLICATE/RESET 四裁决——缺口与
重启显形）。零配置：无 seq 的旧信封 CONTINUE 放行（兼容）。

## Notes

- 号段：B=奇数 spec（本轮 159）。
- seq 是<b>投递序</b>（进程内），不是 outbox 持久 seq——重启复位即 RESET 纪元，
  fence 据此识别「计数器换了」，诚实边界入档。

## Decisions so far

- fence per 订阅流（subscriptionId 维度各自追踪）。

## Not yet specified

- 跨重启持久 seq（纪元内绝对序）；多实例全局序。

## Out of scope

- 沿用 #7–#107；exactly-once 承诺（at-least-once + 幂等键是既有立场）。

## Tickets

- [x] [T517 信封 seq + SequenceFence 四裁决](tickets/T517-seq-fence.md)（impl-292）
- [x] [T518 围栏回归（单调/缺口/重复/纪元复位/兼容）](tickets/T518-seq-fence-tests.md)（impl-292）
