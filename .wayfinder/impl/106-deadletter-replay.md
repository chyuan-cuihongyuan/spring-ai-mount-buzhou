# impl-106 — webhook 死信重放

**What to build:** 死信一键迁回 outbox 重投（attempts 清零、容量满部分重放）。

**Blocked by:** None

**Status:** done

- [x] WebhookOutbox.requeueDead（损坏死信丢弃；容量满停）
- [x] WebhookEventForwarder.replayDeadLetters() + nudge + INFO 日志
- [x] 测试：耗尽死信→恢复→重放→终见事件——core 7/7 绿；spec 37 §B

## Done

commit：见 git log（impl-106）。
