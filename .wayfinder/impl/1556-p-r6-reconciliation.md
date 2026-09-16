# impl 1556 — P 会话 R6 对账轮（spec 2005 / T3111–T3112 / R6）

纵切片：快照补登（regenerateSnapshot reactor 形态：998→1006，+8 = P 系
4 + O 系代补 4）+ api-surface.md 八行 + CONTEXT 897→905 + 全仓 mvn
verify 三门全绿 + P 对账门核账 + push 重试（网络恢复后补推积压）。

- 命令：`mvn -pl buzhou-spring-boot-starter -am test -Dtest='ApiSurfaceSnapshotTest#regenerateSnapshot' -Dbuzhou.api-snapshot.regenerate=true -Dsurefire.failIfNoSpecifiedTests=false`
- verify：16 模块 BUILD SUCCESS（见轮内执行记录）。
