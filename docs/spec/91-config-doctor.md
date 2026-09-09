# Spec 91 — 配置体检 doctor（effort #52）

> wayfinder map：`.wayfinder/maps/effort-52.md`（T347–T348）。借鉴：Spring Shell doctor /
> Spring Boot diagnostics。

## Problem Statement

配置类问题第一来源是「拼错键静默失效」（`semantic-driftt` 写进去框架不认识也不
报错——能力没开还以为开了）与「值域越界」（布尔键写 yes-please）。这些只在运行
时症状暴露，定位成本高——#52 插曲（jar 与源码配置面漂移导致矩阵假失败）即实例。

## Solution

`ConfigDoctor`（core/config）：classpath 各模块 additional metadata json 聚合
buzhou.* 键宇宙；`examine(Map<String,String>)` 对照实际配置面：
- 未知键 → WARN + 编辑距离 ≤2 最近邻建议（「是否想写 buzhou.memory.semantic-drift？」）；
- 已知键值与声明类型不符 → ERROR（Boolean 严格 true/false 白名单——绕开
  `Boolean.parseBoolean` 任意输入 false 的陷阱；数值可解析性）；
- 合法 → INFO 计数（不刷屏）。
报告：findings 有界 64 条 + 三级计数 + 单行 `summary()`。`examine(Environment)`
聚合入口（EnumerablePropertySource 枚举，只取 buzhou.*）。autoconfig
`buzhou.config-doctor.enabled`（默认关）：就绪事件体检一次，发现走日志。
只读不写——报告不改行为。

## User Stories

1. 作为宿主，我要拼错键被点名并给近邻建议，所以能力失效在启动期可见。
2. 作为运维，我要值域越界即报 ERROR，所以配置错误不进运行时排障流程。
3. 作为安全负责人，我要报告不含值内容，所以体检面无敏感泄漏。

## Implementation Decisions

- 键宇宙 = classpath metadata json 聚合（与 ConfigBindingsMatrixTest 同法——
  单一事实源，jar 里就有）。
- metadata 不可读 = 键宇宙为空（体检退化为全 WARN——诚实降级不静默装好）。
- 就绪事件（非启动早期）——全配置源已合并后体检。

## Testing Decisions

- 近邻建议（多打字符/尾错位）；无近邻也 WARN；Boolean/Double 不可解析 ERROR +
  合法键 INFO；干净配置 OK 摘要；100 坏键截 64；Environment 只取 buzhou.* 且
  近邻指向真实 classpath 键；levenshtein 基础面。

## Out of Scope

- 跨键矛盾组合检测（语义规则表）；健康端点暴露；报告导出。

## Further Notes

- 摘要行是启动日志的天然候补（opt-in 下默认 INFO 级一行——无坏键零噪音）。
