# 538 — 金丝雀过滤×慢启动×热重载联动补验

**What to build:** 编排用例——filter→构造→热调升配 ramp→tick 到位（测试域轮）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 编排用例
- [x] spec 735 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-resilience -am test` 绿。commit 见本轮 `test(resilience)` 提交。
