# effort #846 — 死信重投成功率读数

- 会话：H 会话 800 系第 47 轮 ｜ spec [846](../../../docs/spec/846-deadletter-redelivery-stats.md) ｜ 票 [T1193](../tickets/T1193-deadletter-redelivery-stats.md)/[T1194](../tickets/T1194-deadletter-redelivery-stats-verify.md) ｜ impl599
- 借鉴：sidekiq retry set 扩散（847 死信台账姊妹面）

## 勘察（排重）

- WebhookDeadLetter：死信记录（eventId/attempts/createdAt）——重投结果统计缺位。
- WebhookOutboxAudit：outbox 审计（不同层）。
- grep -i `redeliver|重投`：无命中。

## 决定

`DeadLetterRedeliveryStats`（core.webhook，原子记账）：record(success)——attempts/successes+成功率（空尝试 0）+连续失败 streak（成功清零）。喂点=重投路径装配侧；重投语义归调用方。

## 测试

成功率 0.5+streak 清零/连续失败 streak=3+率 0/空真——3 例全绿。

## 诚实边界

不执行重投（语义归调用方）；全局计数非 per-event（分账由调用方建实例）；进程内存有界。
