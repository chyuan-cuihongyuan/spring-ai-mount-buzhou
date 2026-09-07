# Spec 304 — 事务批补偿（effort #304）

> wayfinder map：`.wayfinder/maps/effort-304.md`（T599–T600）。借鉴：saga 模式
> （Seata 长事务补偿族；fog 152「事务批回滚/补偿」项）。

## Problem Statement

跨步多写操作没有回退面：归档 = 写归档条目 → 级联删活数据，第二步部分失败
（某 store 删除抛错）时活数据删了一半而方法仍返回成功——状态真相互斥且
不可恢复。

## Solution

**`CompensatingBatch`（core.transaction）**：

- 步 `Step.of(name, action, compensation)`——compensation 接收本步结果。
- `run(uow, steps)`：逐步在 `UnitOfWork` 事务内执行；任一步失败 → 已成步
  <b>倒序补偿</b>（每步补偿在自身事务内）后原异常上抛。
- <b>补偿失败即停止回退</b>：后续（更早步）补偿不执行——它们假设后者已
  成功；log ERROR + `buzhou.saga.compensation-failed{step}` 计数，原异常
  仍上抛（人工介入语义，证据留档）。
- 观测：`buzhou.saga.compensated{step}` 每步补偿成功计数。

**archive() 接线**（归档条目 = undo log）：

- step1 写归档条目（补偿 = 撤归档键）；step2 级联删活数据（部分失败 →
  上抛；补偿 = 从条目写回 消息/摘要/状态 三槽）。
- 端态：step2 失败且补偿全成 → 活数据复原 + 归档键撤（净回原状）；
  补偿也失败 → 归档键保留（唯一完整副本，人工介入）。

## User Stories

1. 作为运维，归档中途故障后会话回到原状（或归档键保留可人工重试）——
   不再有「删了一半还报成功」。
2. 作为开发者，多步写操作声明每步补偿即得回退面——手写倒序 try/catch
   消失。
3. 作为运维，saga 计数器即补偿发生面（健康信号）。

## Implementation Decisions

- archive() 部分失败从「吞掉返回 true」改「上抛」（诚实化；主代码调用方
  仅 purgeExpired 不受影响）。
- restore() 不接线（归档键最后删——失败可幂等重放）。

## Testing Decisions

- `CompensatingBatchTest`：全成不补偿 / 中途失败倒序补偿原异常上抛 /
  补偿失败停止回退 / 每步在事务内执行。
- `SessionArchiverCompensationTest`：级联部分失败（obs 槽抛）→ 活数据
  复原 + 归档键撤；补偿再失败（summary 槽抛）→ 归档键保留 + 原异常上抛。

## Out of Scope

- 分布式事务；并行步；补偿断点操作台。

## Further Notes

- 持久化族：UnitOfWork（13）/ fsck（T108）/ 归档（102/103）/ **批补偿（本轮）**。
