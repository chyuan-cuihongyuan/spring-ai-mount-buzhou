# 525 — 工具泳道排队时延观测

**What to build:** LaneLimitingToolCallback acquire 段计时——WaitStats（waited/totalWaitNanos/maxWaitNanos/timeouts）+ waitStats() getter + buzhou.lane.wait/lane.timeout 指标。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] acquire 计时 + WaitStats + 指标（lane tag 有界）
- [x] 阻塞/超时/直通/零回归四组用例
- [x] spec 722 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
