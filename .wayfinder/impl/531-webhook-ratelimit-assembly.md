# 531 — webhook 投递限速 yml 装配

**What to build:** webhookEventForwarder bean 读 rate-limit-per-second/burst → setRateLimiter 直通；缺省零变化。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] env 读参 + limiter 构造 + burst 缺省
- [x] burst 缺省口径 + limiter 语义回归用例
- [x] spec 728 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
