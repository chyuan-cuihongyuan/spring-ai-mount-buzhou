# Wayfinder Map — Buzhou 告警注解随发（effort #347，C 会话第 48 轮）

> C 会话第 48 轮。312 告警触发只带机制名——值班人收到通知还要去查「这
> 个规则该看哪个 runbook、影响什么」。Alertmanager annotations 的常识：
> 规则声明时带 runbook-url/summary 等注解，随通知载荷直达消费端。

## Destination

AlertRule 增可选 annotations（Map——runbook-url/summary 等；3 参兼容
构造保留）→ AlertFiring 载荷增 annotations（5 参兼容构造保留——330 门
等既有构造零改动）→ yml buzhou.alert.rules[].annotations.<k>=<v> 绑定
透传。通知通道（宿主 listener）从此拿到自描述告警。

## Notes

- 号段：spec 347 / T685–T686 / impl-370。
- 借鉴源：Prometheus Alertmanager annotations（runbook-url/summary）。
- 纪律：注解原样透传不解释（键约定归宿主）；未声明空 map 零变化。

## Decisions so far

- 兼容构造双保留（AlertRule 3 参/AlertFiring 5 参）——既有调用方零破坏。

## Out of scope

- 注解模板变量插值（{{ $labels }}——buzhou 无标签体系）；富通知渲染
  （归宿主通道）。

## Tickets

- [x] [T685 注解随发（Rule/Firing/透传）](tickets/T685-alert-annotations.md)
- [x] [T686 yml 绑定 + 收口](tickets/T686-annotations-yml.md)
