# Wayfinder Map — Buzhou superstep 原子批（effort #86，新 50 轮会话第 1 轮）

> effort #86，新会话（#86–#135 共 50 轮）首轮。承接 #85 收口 fog 台账种子①
> 「事务性并行批（LangGraph superstep）」。

## Destination

并行工具批的 superstep 原子性（opt-in）：任一调用入参校验未过/工具缺失时，
整批不派发——成功同伴不再先斩后奏。默认关=既有 per-tool 行为零变化。

## Notes

- 借鉴 LangGraph superstep（批为原子步）+ Temporal Activity 前检思想；
  既有 BatchFeedbackPolicy.FAILED_ONLY 管的是「执行后回喂」，本能力管「执行前派发」——正交。
- 每轮完整 loop：wayfinder map → spec → tickets → implement。
- **并行会话分工（2026-08-30 登记）**：本主题由两会话并行认领——A 会话持有
  `concurrent/SuperstepBatch` 通用原语族（spec 122 §A/§B，含 ErrorCode.SUPERSTEP_FAILED，
  spec 文件 122-superstep-batch.md）；B 会话（本行作者）持有 harness 集成侧
  （HarnessToolCallingManager 原子前检 + BATCH_ABORTED，spec 文件
  122-atomic-superstep-batch.md）。互不改动对方文件；提交各管各的路径。
  后续轮次按「先建 .wayfinder<N>/MAP.md 者得 N」自然互斥认领。

## Decisions so far

- 原子中止的同伴结局记 BATCH_ABORTED（新 outcome 维度，事件日志可查「未执行因同伴」）。
- 同一 spec 编号 122 双文件并存（A/B 侧重不同）——收口轮合并归一。

## Not yet specified

- 批内部分派后的事务回滚（副作用不回滚——诚实边界，不谎称事务）。

## Out of scope

- 沿用 #7–#85。

## Tickets

- [x] [T443 superstep 原子批前检 + BATCH_ABORTED 结局](../tickets/T443-atomic-superstep.md)（impl-271）
- [x] [T444 原子批回归测试（默认关/开/缺失工具/事件日志）](../tickets/T444-atomic-superstep-tests.md)（impl-271）
