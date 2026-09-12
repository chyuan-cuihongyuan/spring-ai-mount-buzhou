# 551 — G 会话收口预检轮（台账核查）

**What to build:** spec/票/impl 三台账对账 + 守门测试复跑 + spec 745 缺位填补。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] spec 700–748 连续核查（745 缺位本轮填补）
- [x] 票 96 张全闭环对账
- [x] impl 535–550 连续对账
- [x] SpecCoverage + 快照比对复跑绿
- [x] spec 745 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-spring-boot-starter -am test` 绿。commit 见本轮 `docs(core)` 提交。
