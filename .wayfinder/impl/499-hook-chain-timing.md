# 499 — hook 链 per-hook 耗时观测

**What to build:** HookChain 八回调面 nanoTime 包裹计时 + per-hook Timing 累计（count/total/max）+ stats() 快照 + 慢 hook WARN 原子去重。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] HookChain 计时内嵌（Timing LongAdder + volatile max）
- [x] stats() 不可变快照
- [x] 慢 hook WARN 原子去重（100ms 阈值常量）
- [x] 慢/快混合 + block 路径 + 累计用例 + 零回归
- [x] spec 646 + README 行
- [x] 全模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
