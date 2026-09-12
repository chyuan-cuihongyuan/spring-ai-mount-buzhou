---
id: T981
title: 指标命名规范守卫测试的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

全仓 316 个 buzhou.* 指标名散布各模块——命名漂移（大小写混用、空格、驼峰段）无守卫；Prometheus/micrometer 命名惯例（点分隔小写）靠自觉。守卫怎么落（零新依赖、不误伤配置键/日志文案）？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 16 轮 = effort #715 / spec 715 / impl 518）：`MetricNamingGuardTest`（starter 测试域，边界守卫同款源码扫描范式）——双正则提取**指标名候选**：① 直接调用点 `\.(counter|timer)\(\s*"(buzhou\.[^"]+)"`；② 指标名常量赋值 `String (METRIC|COUNTER|TIMER)[A-Z_]*\s*=\s*"(buzhou\.[^"]+)"`（提取自调用点上下文，天然避开配置键/文案——它们不出现在 counter/timer 调用位）。命名规则：`^buzhou(\.[a-z][a-z0-9-]*)+$`（点分隔、段小写、段内允许数字与连字符——既有 pdb-rejected/scale-up 形态合法化）。违规清单式断言（可读）。借鉴 prometheus/client_java naming conventions 守卫测试。
