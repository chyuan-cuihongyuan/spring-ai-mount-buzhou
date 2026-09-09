# Wayfinder Map — Buzhou 序号围栏跨重启持久纪元（effort #303，C 会话第 4 轮）

> C 会话第 4 轮。spec 159 的 `SequenceFence` 把「seq 变小」<b>猜测</b>为发送方
> 重启新纪元（RESET）——重启是猜出来的不是声明出来的；且发送侧 `deliverySeq`
> 纯内存，重启复位后接收方无法区分「重启」与「重放/截断」（fog 227 第 7 项）。

## Destination

Kafka producer epoch 思想落地：发送方每次启动<b>持久递增纪元</b>（存 outbox
合成会话 `meta.epoch`），信封显式携带 `epoch` 字段；`SequenceFence` 纪元感知
——RESET 只在纪元跃升（显式）或同纪元 seq 倒退（异常防御）时给出，旧纪元
迟到投递判 STALE（安全丢弃）；旧发送方无 epoch 走原 seq 推断路径（兼容）。

## Notes

- 借鉴：Kafka producer epoch（idempotent producer 的 generation 语义）。
- 号段：本轮 spec 303 / T597–T598 / impl-326。

## Decisions so far

- 纪元取 `max(持久值+1, 启动墙钟毫秒)` 后回写持久——防快启同毫秒撞号，
  存储写失败降级墙钟值（实际不撞）。
- 判定矩阵：纪元升 → RESET（显式新纪元）；纪元同 → 既有 CONTINUE/GAP/
  DUPLICATE + 倒退 RESET（防御性重基线）；纪元降 → STALE（旧纪元迟到，
  基线不动）；无纪元 → 原 seq 推断路径逐位不变。
- 发送侧 envelope seq 保持每纪元从 1 起（纪元化后无需跨纪元续号）。

## Not yet specified

- 接收方 fence 基线自身持久化（接收侧部署关切——epoch 机制已消除猜测，
  基线持久归接收方自决）。

## Out of scope

- outbox 内部排序 seq（进程内语义已入档 spec 24，不动）。

## Tickets

- [x] [T597 发送侧持久纪元 + 信封 epoch 字段](../tickets/T597-sender-epoch.md)（impl-326）
- [x] [T598 SequenceFence 纪元感知判定矩阵回归](../tickets/T598-fence-epoch.md)（impl-326）
