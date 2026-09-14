# 1162 — WebhookOutbox 锁迁移

**What to build:** spec 1606 中危 #1 落地：四方法 monitor→ReentrantLock wrapper。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] append/appendRetry/orphanIndexCount/requeueDead → wrapper + *Locked（方法体零动）
- [x] webhook 包 105 用例零回归

## Done

验证：`mvn -pl buzhou-core test -Dtest=io.github.chyuan_cuihongyuan.buzhou.core.webhook.**`。
