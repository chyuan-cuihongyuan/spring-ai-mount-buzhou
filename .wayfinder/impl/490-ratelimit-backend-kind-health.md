# 490 — 限流后端形态进健康面

**What to build:** ResilienceStats.rateLimitBackend（configure 写入 + details 直读）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] stats 字段/setter + configure 接线
- [x] 1 用例三态 + 全模块零回归
- [x] spec 637 + README 行

## Done

验证：`mvn -pl buzhou-resilience -am test` 绿。commit 见本轮 `feat(resilience)` 提交。
