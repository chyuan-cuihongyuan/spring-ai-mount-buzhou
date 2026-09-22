# Spec 1908 — 雪花 ID 分解（effort #1908，R109）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3017–T3018，impl 1509）。借鉴：
> Twitter Snowflake（万星级生态惯例）64 位 ID 布局——1 符号位 +
> 41 位毫秒时间戳 + 10 位机器号 + 12 位序列：ID 本身携带生成时刻
> 与来源，分解即可排障（哪个实例、什么时间、同毫秒第几个）。

## Problem Statement

分布式 ID 只当黑盒字符串：排查「这条消息哪来的、什么时候生成的」
要查库反推——时间有序 ID 的自描述信息（时间戳/机器号/序列）被
浪费，缺编解码判定面。

## Solution

`SnowflakeIdDecompose`（core/concurrent，静态纯函数 + 嵌套
Decomposed）：

- `decompose(id, epochMillis)`：按位拆解 → 时间戳（epoch+偏移）、
  机器号（10 位）、序列（12 位）；
- `compose(timestampMillis, workerId, sequence, epochMillis)`：逆
  组装（roundtrip 自洽）；越界 fail-fast（worker > 1023 / seq >
  4095 / 时间早于 epoch）。

## User Stories

1. 作为排障者，ID 分解出「实例 7 在 epoch+1690000000000ms 的第
   42 个」——来源与时刻直读。
2. 作为生成器作者，compose/decompose roundtrip 自洽——编码有据。
3. 作为验证者，越界即 fail-fast——机器号 1024、序列 4096 拒收。

## Implementation Decisions

- 纯位运算零状态；布局常量 1+41+10+12（移位 22、掩码 0x3FF/0xFFF）；
  epoch 由调用方声明（Snowflake 惯例自定义纪元）。

## Testing Decisions

- roundtrip 三例（零序列/满序列 4095/跨机器）；分解字段精确断言；
  越界三型（worker 1024/seq 4096/时间早于 epoch）fail-fast。

## Out of Scope

- 不做 ID 生成与时钟回拨处置（归生成器）；不做字符串形态。

## Further Notes

- 与 确定性哈希（DeterministicHash）互补：那是无序指纹，这是
  时间有序可分解 ID。
