# 666 — 导出域三件套联动 e2e

**What to build:** ExportDomainE2ETest 五场景编排闭环（协商×审计×规范化指纹×diff）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 五场景闭环测试
- [x] spec 913 + README 行（欠账累计 906–913 八行）

## Done

验证：`mvn -pl buzhou-core test -Dtest=ExportDomainE2ETest` 全绿（五场景一次通过，无实现缺陷）。commit 见本轮 `test(core)` 提交。
