# 496 — JSONL 轮转 yml 装配扩散

**What to build:** health.timeline 与 shadow 两处 yml 键（export-max-bytes/history、detail-max-bytes/history）→ 属性组 +2 槽 → 装配透传 RollingJsonlWriter 三参构造。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] BuzhouHealthTimelineProperties +2 槽（4 参便捷构造保留）
- [x] ResilienceProperties.Shadow +2 槽（4/5 参便捷构造保留 + canonical @ConstructorBinding）
- [x] 两处装配透传
- [x] 两侧绑定用例（缺省默认/显式 0 关）+ 零回归
- [x] spec 643 + README 行
- [x] 全模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` + `-pl buzhou-resilience -am test` 绿。commit 见本轮 `feat(core/resilience)` 提交。
