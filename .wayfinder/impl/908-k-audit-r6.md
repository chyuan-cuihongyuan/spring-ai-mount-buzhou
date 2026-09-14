# 908 — K 会话周期对账轮 R6

**What to build:** 全仓 `mvn verify`（隔离 worktree）+ K 线工件链五项对账清单（spec/README/票/impl/map）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 工件对账：spec 1200–1205 + README 行 OK；票 T1801–T1818 closed（T1819 open 排下轮）；impl 903–908 done
- [x] 全仓 `mvn verify`（隔离 worktree）：15/16 绿；starter 两显形红各单列票——T1818 快照过期（已修+复验绿）/ T1819 指标捕获污染（下轮修）；examples 连带跳过一次
- [x] spec 1205 + README 行

## Done

对账五项全 OK；两显形红各自单列票处置（一修一排程）；多会话共享工作区风险（worktree 被删 / detached HEAD / 他线卷入提交）入 map。commit 见本轮 `chore(wayfinder)` 提交。
