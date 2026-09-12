# 521 — webhook 投递限速

**What to build:** WebhookRateLimiter 令牌桶 + WebhookEventForwarder 可选接线（attemptOnce 前取令牌，defer 留 outbox 原状 + 计数，整批 defer 提前结束），默认关零变化。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] WebhookRateLimiter（capacity/refill + 时钟注入 + deferredCount）
- [x] forwarder 可选接线（defer 路径 + 整批 defer 早退）
- [x] 节流/不碰重试状态机/回填/零回归用例
- [x] spec 718 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
