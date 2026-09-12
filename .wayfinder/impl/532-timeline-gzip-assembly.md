# 532 — 健康时间线 JSONL 压缩线装配

**What to build:** BuzhouHealthTimelineProperties.exportCompressFrom（缺省 0，1 拒）+ HealthTimelineJsonl 4 参构造 + bean 透传。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] properties 组件 + effective 方法（1 拒）
- [x] HealthTimelineJsonl 重载 + bean 透传
- [x] gz 可解/file.1 明文/缺省零回归用例
- [x] spec 729 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
