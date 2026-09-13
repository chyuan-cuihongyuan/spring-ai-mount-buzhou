# 711 — 消息序列连续性审计

> 来源：G 会话第 12 轮 = effort #711（341 StoreFsck 的消息层对偶）/ [T1022](../../.wayfinder/tickets/T1022-turn-seq-audit.md) / [T1023](../../.wayfinder/tickets/T1023-turn-seq-audit-verify.md) / impl 611。

## Problem

会话消息以（turnSeq, seqInTurn）二元组跨 memory/redis/jdbc 三 store 落库——store 级故障（部分写丢失、主从复制缝隙、误删）在读取侧表现为「上下文突然缺一段」，但没人能区分「模型没生成」与「存储丢了」。StoreFsck（341）覆盖摘要/状态/租约/观测四类，**消息序列连续性**不在其列。序列断裂是存储正确性的第一证据（Kafka offset 审计思想——≈30K star）。

## Solution

- `TurnSequenceAudit`（core.cleanup，纯函数静态原语）：
  - `audit(List<Marker>)` → `List<Finding>`——Marker(turnSeq, seqInTurn) 由调用方从消息投影（解耦 store SPI）；
  - 期望形态：按（turn 升序、turn 内 seq 从 0 连续）排列，turn 序本身从首见 turn 连续递增；
  - 三类发现：`GAP`（turn 内 seq 断号 / turn 序缺号）、`DUPLICATE`（同序对出现两次）、`OUT_OF_ORDER`（更小序对在更大序对之后到达）；
  - 单遍扫描 O(n)；空表 = 零发现；null 表 fail-fast。
- 纯原语不接 store：宿主对任何 store dump（或读出结果）投影即可跑；housekeeper 定时接线归后续轮（538「先原语后接线」节奏）。

## User Stories

1. 排障：用户报「上下文缺一段」——dump 该会话消息投影后跑审计，GAP@turn7 直接把「存储丢数据」从猜测变证据。
2. 巡检扩展：三 store 实现的 CI 对照测试用同一组 marker 断言零发现——实现间序列语义回归可互相锁定。

## Implementation Decisions

- 单遍状态机：维护 (lastTurn, lastSeq, seen set)——seen set 用 long 编码（turn<<32|seq）防重复的内存开销可控（消息量级万内）。
- 起始约定（0,0）连续——跨 store 语义差异由调用方投影归一（诚实边界）。
- 不修复不删除（只读证据面——fsck 族纪律）。

## Testing Decisions

- 健康：3 turn × 递增 seq → 零发现。
- GAP：turn1 缺 seq1；turn 缺号（0,2 无 1）。
- DUPLICATE：同 (turn,seq) 两次。
- OUT_OF_ORDER：(2,0) 在 (1,0) 前出现。
- 空表零发现、null fail-fast。

## Out of Scope

- store SPI 集成与定时巡检（后续轮）。
- 修复/补齐（只读纪律）。
- 事件级序列（本面只管消息层）。

## Further Notes

与 705（Redis 键布局）/707（spill 配对）同会话主线：**fsck 族从「资源层」推进到「数据层」**。
