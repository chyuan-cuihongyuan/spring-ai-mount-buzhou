# 698 — write_file noclobber 防误覆盖模式

**What to build:** WriteFileTool noclobber opt-in（存在拒绝零副作用）+ 三态测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] setNoclobber + 存在性守门（写盘前判定零副作用）
- [x] NoclobberTest（新建/存在拒绝/默认关覆盖）
- [x] spec 951 + README 行（欠账累计 926–951）

## Done

验证：`mvn -pl buzhou-tools -am test -Dtest=NoclobberTest` 全绿。commit 见本轮 `feat(tools)` 提交。
