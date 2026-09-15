# 934 — 金丝雀路径 e2e 直测（R32）

**What to build:** CanaryPathEndToEndTest（3 用例：canary 路由成功/canary 失败链序回退/canary-selected 事件 payload）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] CanaryPathEndToEndTest（3 用例）
- [x] spec 1231 + README 行
- [x] 验证：定向绿 + resilience 全量绿

## Done

验证：定向 3 用例全绿；resilience 全量绿（442+ 用例）。commit 见本轮 `test(resilience)` 提交。
