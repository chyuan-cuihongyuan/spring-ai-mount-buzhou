# Spec 347 — 告警注解随发（effort #347）

> wayfinder map：`.wayfinder/maps/effort-347.md`（T685–T686）。C 会话第 48 轮。

## Problem Statement

告警通知只带机制名与 details：值班人收到通知后要自己查「这个规则对应
哪个 runbook、影响摘要是什么」——规则的自描述信息（运维知识）没有
随通知流动的通道。

## Solution

Alertmanager annotations 思想——规则声明时带注解、随通知载荷直达：

- **AlertRule.annotations**（可选 `Map<String,String>`，null → 空 map；
  3 参兼容构造保留）：约定键如 `runbook-url` / `summary`（键语义归宿主，
  引擎原样透传不解释）。
- **AlertFiring.annotations**：通知载荷携带（5 参兼容构造保留——330
  门与既有测试构造零改动）；恢复通知同样携带。
- **yml**：`buzhou.alert.rules[].annotations.<k>=<v>`（RuleSpec 扩展 +
  兼容构造；装配透传）。
- 345 面板 rules 段同步带 annotations。

## User Stories

1. 作为值班人，我想通知自带 runbook 链接与摘要，所以 收到即可行动
   不用先查文档。
2. 作为运维，我想注解声明在规则上一处维护，所以 触发/恢复/面板三处
   自动一致。
3. 作为使用者，未声明注解时载荷多一个空 map，所以 零风险。

## Implementation Decisions

- record 双兼容构造（AlertRule 3 参 / AlertFiring 5 参）委托规范构造。
- 引擎 notify 构造 AlertFiring 时带上 rule.annotations()。

## Testing Decisions

- 注解随触发与恢复通知到达 listener；yml 绑定（runbook-url/summary）；
  兼容构造（旧 5 参 AlertFiring 不带注解 = 空 map）；面板 rules 段含注解。

## Out of Scope

- 模板插值；通知渲染；注解校验（开放键空间）。

## Further Notes

- record 组件扩展不增公共类型——快照零 diff 预判。
