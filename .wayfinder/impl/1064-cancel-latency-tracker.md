# 1064 — 取消延迟追踪读面

**What to build:** CancelLatencyTracker implements SessionObserver（单会话实例构造期绑定——TurnErrorSampler 同型装配；在途标记+未决取消键+环 64+trackedTotal 累计+P50/P95）+ 六测。

**Blocked by:** 全仓 verify（R10 里程碑作业）——已完成后编码。

**Status:** done

- [x] CancelLatencyTracker（core/session，构造期绑 sessionId；无轮取消不入账）
- [x] CancelLatencyTrackerTest（哨兵/轮间取消忽略/取消→终结入环/环挤旧 tracked 累计/reset）
- [x] spec 1411 + README 行 + api-surface.md L 段 + 快照再生（评审修正：SessionObserver 回调不携 sessionId——初版 SessionKeys 静态猜测不存在，改构造期绑定；tracked 语义修为累计+环仅作分位窗）

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='CancelLatencyTrackerTest'` 6/6 绿；starter 双门绿（快照 +8 公共类型）。
