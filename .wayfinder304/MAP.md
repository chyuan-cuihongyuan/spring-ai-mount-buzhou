# Wayfinder Map — Buzhou 事务批补偿（effort #304，C 会话第 5 轮）

> C 会话第 5 轮。`UnitOfWork` 只管单 store 事务；跨步多写操作（归档 = 写归档
> → 级联删活数据）部分失败时无回退——活数据删了一半、归档在、真相互斥
> （fog 152「事务批回滚/补偿」项）。

## Destination

`CompensatingBatch`（core.transaction，saga 思想）：顺序步 + 逐步补偿；任一步
失败 → 已成步<b>倒序补偿</b>后原异常上抛；<b>补偿失败即停止回退</b>（后续补偿
不跑——它们假设后者已成功；人工介入，证据留档）。接线 `SessionArchiver.archive`
（归档条目天然是 undo log：step2 失败即从条目写回活数据）。

## Notes

- 借鉴：saga 模式（Seata/长事务补偿族）；「补偿失败即停」= saga 经典语义
  （compensation failure → manual intervention）。
- 号段：本轮 spec 304 / T599–T600 / impl-327。

## Decisions so far

- archive() 行为变更（诚实化）：级联清理部分失败原先吞掉返回 true——现在
  上抛（部分删除 + 声称成功才是 bug）；主代码调用方仅 purgeExpired（不受影响）。
- 补偿顺序 = 步序倒序；补偿自身失败：log ERROR + 计数 + 停止回退 + 原异常
  上抛（归档场景：归档键保留——数据安全优先于状态整洁）。
- restore() 不动（归档键最后删——失败天然可重试幂等重放，无补偿必要）。

## Not yet specified

- 补偿断点恢复台（人工介入的操作面——观测族后续）。

## Out of scope

- 分布式事务（XA/2PC）；并行步（saga 顺序语义）。

## Tickets

- [x] [T599 CompensatingBatch 原语（倒序补偿/补偿失败即停/观测计数）](tickets/T599-saga-batch.md)（impl-327）
- [x] [T600 archive 接线 + 部分失败补偿回归](tickets/T600-archive-saga.md)（impl-327）
