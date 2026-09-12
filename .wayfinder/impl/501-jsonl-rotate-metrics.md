# 501 — JSONL 轮转事件指标化

**What to build:** RollingJsonlWriter 轮转成功/失败发 buzhou.jsonl.rotated / rotate-failed 指标（tag file）；静态路径同发；getter 保留。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] rotate()/rotateIfNeeded 指标事件
- [x] 收集 metrics 断言用例 + 既有零回归
- [x] spec 648 + README 行
- [x] 全模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
