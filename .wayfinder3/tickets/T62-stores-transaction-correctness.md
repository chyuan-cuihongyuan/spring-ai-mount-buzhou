---
id: T62
title: stores · 事务边界接线与并发正确性
type: grilling
status: closed
assignee: ""
blocked-by: [T55]
created: 2026-08-14
---

## Question

事务正确性如何修复？需裁决：① JdbcUnitOfWork 实际接线面（tool_call_log/state/run_registry 的多表写同事务）；② summary MAX(version)+1 读改写竞态修复（UPSERT/乐观锁）；③ 单条脏 JSON 炸整个会话 load 的隔离（跳过+WARN+计数？拒绝？）；④ 运行期写失败的降级语义（可配：fail-turn vs 降级只读继续 vs 入队重试）；⑤ Redis UoW 连接池化；⑥ 熔断器时间恢复/半开语义（连续 3 次失败永久熔断的修复）。

## Resolution

**ratify（用户常设授权，可推翻）→ spec 13 §stores-7**：UoW 接线（tool_call_log/state/run_registry 多表写与先删后插同事务）；摘要版本原子 UPSERT（方言分轨 ON CONFLICT/ON DUPLICATE）；load 逐条隔离脏数据（跳过+WARN+BuzhouDataCorruptionException 计数）；写失败策略 FAIL_TURN（默认，既有语义）| DEGRADE（降级内存+ERROR+指标）；Redis UoW 连接池化（上限可配）；熔断 failureWindow PT10M 半开试探、成功清零、随会话清理。
