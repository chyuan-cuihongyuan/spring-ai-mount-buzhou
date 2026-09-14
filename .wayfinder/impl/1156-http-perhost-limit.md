# 1156 — http_request per-host 并发上限

**What to build:** PerHostConcurrencyGuard + HttpRequestTool 接线 + 第七拒绝桶 + yml 装配。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] PerHostConcurrencyGuard（CAS 计数 tryEnter/exit/inFlight）
- [x] HttpRequestTool：guard 重载构造 + finally 释放 + hostLimitRejects 桶
- [x] ToolsModule：http-max-per-host yml + 装配传 guard
- [x] 测试五断言全绿（tools 112 用例）

## Done

验证：`mvn -pl buzhou-tools test` 全绿。
