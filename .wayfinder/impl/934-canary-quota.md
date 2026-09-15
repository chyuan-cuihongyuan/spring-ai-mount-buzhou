# 934 — 金丝雀候选限流放行与配额窗口语义（R34）

**What to build:** CanaryQuotaExhaustedTest（放行态双轮直达 + 耗尽态窗口语义实证）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] CanaryQuotaExhaustedTest 放行态/耗尽态两用例
- [x] spec 1233 + README 行
- [x] 验证：定向绿 + resilience 全量绿

## Done

验证：定向 2 用例全绿；resilience 全量绿。commit 见本轮 `test(resilience)` 提交。
