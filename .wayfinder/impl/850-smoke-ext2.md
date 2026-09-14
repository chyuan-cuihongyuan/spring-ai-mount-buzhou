# 850 — 冒烟补全轮

**What to build:** ReadoutContractSmokeTest 增 TodoTool/ToolSlowLog 两独立冒烟。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 两冒烟测试（按各自形状断言非负）
- [x] spec 1098 + README 行

## Done

验证：`mvn -pl buzhou-spring-boot-starter -am test -Dtest='ReadoutContractSmokeTest'` 全绿。commit 见本轮 `test(starter)` 提交。
