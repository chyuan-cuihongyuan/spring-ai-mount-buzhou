# 715 — 指标命名规范守卫测试

> 来源：G 会话第 16 轮 = effort #715（借鉴 prometheus/client_java naming conventions 守卫测试）/ [T981](../../.wayfinder/tickets/T981-metric-naming-guard-shape.md) / [T982](../../.wayfinder/tickets/T982-metric-naming-guard-verify.md) / impl 518。

## 背景

全仓 316 个 `buzhou.*` 指标名散布各模块——命名漂移（大小写混用、空格、驼峰段）无物理守卫。Prometheus/micrometer 惯例：点分隔小写段；基数守卫（spec 111）管 tag 有界，命名规范本身无测试。

## 目标

- `MetricNamingGuardTest`（starter 测试域，边界守卫同款源码扫描范式，零新依赖）：
  - 双正则提取指标名候选：① 直接调用点 `.counter("…"` / `.timer("…`；② 指标名常量赋值 `String METRIC*/COUNTER*/TIMER* = "buzhou…"`（调用点上下文天然排除配置键/日志文案——它们不出现在 counter/timer 位）。
  - 命名规则：`^buzhou(\.[a-z][a-z0-9-]*)+$`——点分隔、段小写、段内数字与连字符合法（既有 `buzhou.archive.pdb-rejected`/`buzhou.bulkhead.scaling.scale-up` 形态兼容）。
  - 全仓扫描违规清单式断言（可读、可定位）。
- 规则与提取器各自单测（守卫非恒绿自证）。

## 非目标

不扫 tag 键/值（有界性归基数守卫 spec 111 口径）；不扫运行时拼接的动态指标名（字面量口径诚实划界）；不做 micrometer 注册表级断言（装配面归既有指标测试）。

## 测试

规则判定单测（合法/非法各列）；提取器单测（调用点+常量+干扰项合成片段）；全仓扫描零违规。

## 兼容性

纯测试域增量；生产源码零变化。
