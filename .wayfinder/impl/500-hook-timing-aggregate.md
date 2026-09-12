# 500 — hook 计时进程级聚合与健康读面

**What to build:** HookTimingAggregator（Holder 模式进程级聚合）+ HookChain 镜像累计 + HookTimingHealth（BuzhouHealth 读面）+ 装配默认开启。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] HookTimingAggregator + Holder
- [x] HookChain 镜像累计（aggregator 关零变化）
- [x] HookTimingHealth + BuzhouCoreAutoConfiguration 装配
- [x] 跨链合并/缺省隔离/装配 details 用例 + 零回归
- [x] spec 647 + README 行
- [x] 全模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
