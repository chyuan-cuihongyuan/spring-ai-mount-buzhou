# Spec 97 — 会话归档冷层（effort #58）

> wayfinder map：`.wayfinder/maps/effort-58.md`（T363–T364）。#35 fog 项收口。

## Problem Statement

SessionCleaner（spec 13/29）是硬删除级联：retention 到期即清五槽——误清不可逆，
审计/回溯需求（客服争议、事故复盘）没有冷存档安全网。

## Solution

`SessionArchiver`（cleanup 包）：`archive(sessionId)` 把消息/摘要/state 三槽快照
为单归档 JSON，落合成会话 `__buzhou.archive__`（键 `archive.<sessionId>`；fsck
天然豁免），随后经 SessionCleaner 级联删除。`restore(sessionId)` 原键原值回放三槽
并删归档键；`archived()` 清单（字典序）。

安全边界：
- 快照编码失败 fail-fast（BuzhouException/DATA_CORRUPTION）——原会话保留不删
  （宁可不清不冒丢失风险）；
- 空会话（无消息且无摘要）不归档返回 false（诚实不动）；
- Instant 经 databind SimpleModule 编解码（ISO-8601 人可读；无 jsr310 依赖）。

## User Stories

1. 作为合规负责人，我要删除前有冷存档，所以审计回溯不随 retention 消失。
2. 作为运维，我要 restore 原键原值回放，所以归档会话可完整复活。
3. 作为宿主，我要编码失败不删原会话，所以归档引入零数据丢失风险。

## Implementation Decisions

- 归档落 state store 合成会话（五 store 面不扩——与 webhook/eval outbox 同先例）。
- 与 spec 28 导出/导入分工：那是活迁移（Id 重映射），这是冷存档（零转换）。

## Testing Decisions

- 归档后三槽清空 + 归档键在册；restore 回放三槽原值 + 归档键删除；
- 空会话/未知归档双 false；清单字典序 + 会话间隔离。

## Out of Scope

- 归档 TTL/容量治理；gzip；autoconfig retention 策略接线（fog 记账）。

## Further Notes

- 复合面：`new SessionArchiver(stores, cleaner).archive(sid)` 一行替代裸 delete。
