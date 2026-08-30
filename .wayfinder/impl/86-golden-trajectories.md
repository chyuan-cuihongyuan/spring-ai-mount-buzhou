# impl-86 — 黄金轨迹回归评估

**What to build:** 六大机制的「脚本化输入 → 事件序列断言」黄金集 + EventSequenceAssert
断言器（testsupport 同发布），机制行为回归 CI 常挡。

**Blocked by:** T104（熔断自适应后语义定格）——已闭合

**Status:** done

- [x] `EventSequenceAssert`（containsInOrder/neverAfter/followedBy/count/payload + attach/attachGlobal）
- [x] `GoldenTrajectoryTest` 六条轨迹：降级链/预算闸/日配额/熔断恢复/REASK/fork
- [x] 实现期机制语义回写：熔断样本按逻辑调用计；forked 发往分支/全局通道
- [x] examples 75/75 绿（黄金集进常规测试面）

## Done

commit：见 git log（impl-86）。
