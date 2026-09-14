# 1058 — Dashboard 查询页守卫与游标解析修复

**What to build:** MAX_PAGE_SIZE=200 常量 + normalizePageSize/parseCursor 共享辅助接入两路径 + 五测。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] DashboardQueryService 钳制+可读游标（共享辅助，filtered 改同源）
- [x] ListSessionsPageGuardTest（钳制/归一/IAE/翻页回归/降级不变）
- [x] spec 1405 + README 行（无新公共类型——常量入既有类，快照面不变）

## Done

验证：`mvn -pl buzhou-observe-dashboard -am test -Dtest='ListSessionsPageGuardTest,DashboardQueryServiceTest,FilteredSessionListingTest'` 13/13 绿。
