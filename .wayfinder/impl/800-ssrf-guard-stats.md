# 800 — SSRF 守卫判定分布读面

**What to build:** SsrfGuard 静态六计数（checks/allowlisted/dnsAllowed/emptyHostRejects/dnsRejects/blockedRejects）+ 嵌套 SsrfGuardStats + stats()/resetForTest() + 放行两桶/拒绝三桶/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 六计数落点（入口/放行清单直通/DNS 校验通过/空主机/DNS 失败/拦截段命中）
- [x] SsrfGuardStats 嵌套 record + totalAllowed/totalRejects 派生 + stats() + resetForTest()
- [x] SsrfGuardStatsTest（放行直通/公网 IP 放行/空主机/DNS 拒绝/内网拦截/守恒/reset 六测）
- [x] spec 1048 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-tools -am test -Dtest='SsrfGuardStatsTest'` 全绿 + 既有 SsrfGuard/HttpRequestTool 回归绿。commit 见本轮 `feat(tools)` 提交。
