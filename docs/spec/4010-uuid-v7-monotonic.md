# Spec 4010 — UUIDv7 时间有序生成器（effort #4010，R11）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6021–T6022，impl 2111）。
> 借鉴：RFC 9562 UUIDv7；v4 随机 UUID 的 B 树随机写病。

## Problem Statement

v4 均匀随机的字典序无时间结构——索引插入点全随机（B 树随机写、
范围扫不可用）——**字典序即时间序**的零协调 ID 件缺失。

## Solution

`UuidV7Monotonic`（core/concurrent，时钟/随机源可注入）：

- 48 位 Unix 毫秒置最高位 + ver 7 + rand_a 12 位**单调计数器**
 （新毫秒随机重置、同毫秒 +1）+ variant 10 + rand_b 62 位随机；
- 计数器溢出向时间戳**借位**（unix_ts_ms+1 伪时序推进——有序性
  不断，RFC 允许）；
- timestampOf/counterOf 回读（version≠7 拒判）；synchronized 单调面。

## User Stories

1. 作为存储作者，主键插入点有序（叶子局部性）——B 树写放大降。
2. 作为分页作者，同毫秒批量生成不撞序（计数器单调）。

## Testing Decisions

- 百次生成 ver=7/variant=2/时间戳回读；固定钟 200 个严格递增且
  全唯一；5 千生成（>4096）借位后仍严格有序、伪时序幅度 ≤2ms；
  时钟推进回读 + 计数器界内；畸形五型 fail-fast（null 源×2/
  null id/v4 拒判）。

## Out of Scope

- 不做 UUIDv6/v8；不做跨实例全局单调（归协调器/Snowflake 位）；
  不做格式化面（Crockford 轮已覆盖人面转录）。

## Further Notes

- 与 SnowflakeIdDecompose（协调位布局）成对：v7 零协调以时间戳换
  排序性。Wave 2（编码与数据形态族）收口。
- 里程碑：11/50。
