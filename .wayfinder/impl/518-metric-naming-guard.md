# 518 — 指标命名规范守卫测试

**What to build:** MetricNamingGuardTest——源码级指标名双正则提取（调用点 + METRIC 常量）+ 命名规则断言（点分隔小写段）+ 规则/提取器单测。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 命名规则（^buzhou(\.[a-z][a-z0-9-]*)+$）+ 提取器（调用点/常量双正则）
- [x] 规则判定 + 提取器合成片段单测（非恒绿自证）
- [x] 全仓扫描零违规
- [x] spec 715 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-spring-boot-starter -am test` 绿。commit 见本轮 `test(starter)` 提交。
