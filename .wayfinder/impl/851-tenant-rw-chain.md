# 851 — 租户沙箱×读写链路组合测试轮

**What to build:** TenantRwChainTest——租户内写→读对称 + 越界拒 + 守恒。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] TenantRwChainTest（租户写读/越界拒/对称恒等/归零四测）
- [x] spec 1099 + README 行

## Done

验证：`mvn -pl buzhou-tools -am test -Dtest='TenantRwChainTest'` 全绿。commit 见本轮 `test(tools)` 提交。
