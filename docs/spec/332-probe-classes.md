# Spec 332 — 健康三探针分层（effort #332）

> wayfinder map：`.wayfinder/maps/effort-332.md`（T655–T656）。C 会话第 33 轮。

## Problem Statement

健康面（impl-41）把所有机制的 DOWN 混在一个聚合层。K8s 部署者面对
「buzhou 健康 DOWN」无法回答该重启（liveness）、该摘流量（readiness）
还是该等待热身（startup）：store 断连触发 liveness 失败会引发重启风暴
——重启治不了外部依赖故障，反而放大故障；内部泄漏不触发 liveness 则
永远没人重启它。探针语义未分层。

## Solution

`BuzhouProbes`（core/health）：把机制归类进三个探针类并给出每类裁决：

- **归类**：yml `buzhou.health.probes.liveness-mechanisms` /
  `startup-mechanisms` 显式点名；**缺省归类 readiness**（保守——外部
  依赖型故障最常见，重启治不了）。机制名引用启动 fail-fast（可用机制
  清单进错误信息——312 同口径）。
- **裁决**：类内任一机制 DOWN → 该类 DOWN（failing 清单列出）；
  UNKNOWN 不连累（未启用 ≠ 失能）；空类 → UP（无要求即满足）。
- **端点** `/actuator/buzhou-probes`：三裁决 + 各类成员与 failing 清单，
  只读，与既有 `/actuator/buzhou` 聚合面并列（聚合面答「哪个机制坏」，
  裁决面答「K8s 该做什么」）。

## User Stories

1. 作为 K8s 部署者，我想让 store 不可达只摘流量不重启，所以 外部依赖
   故障不会引发重启风暴。
2. 作为 K8s 部署者，我想把内部泄漏类机制点名为 liveness，所以 真正
   重启能治的故障会触发重启。
3. 作为 K8s 部署者，我想用 startup 类等待热身完成再放流量，所以 冷启动
   期不被误杀。
4. 作为运维，我想每类裁决带 failing 清单，所以 探针红了不用翻全量
   聚合面就能定位。
5. 作为使用者，我不想配置时端点仍可用（全机制归 readiness），所以
   升级零风险、默认语义保守。
6. 作为贡献者，我想 yml 点名不存在的机制启动即红，所以 拼写错误不
   静默变成永远 UP 的空类。
7. 作为审计者，我想 UNKNOWN 机制不连累任何裁决，所以 未启用机制的
   健康 UNKNOWN 不制造假红。

## Implementation Decisions

- 新公共类型 `BuzhouProbes`（含 `ProbeClass` 枚举与 `Verdict` record）
  + `BuzhouProbesEndpoint`，均落 core/health；装配挂既有
  `BuzhouEndpointConfiguration`（actuator 条件内）。
- 归类冲突（同机制同时进 liveness 与 startup）启动红——归类互斥。
- 端点 payload：`{liveness: {status, failing, mechanisms}, readiness: …,
  startup: …}`；mechanisms 列成员清单（可观测归类本身）。

## Testing Decisions

- `BuzhouProbesTest`：缺省全 readiness / 点名归类 / DOWN 只连累本类 /
  UNKNOWN 不连累 / 空类 UP / 归类冲突红 / 幽灵机制红。
- 端点装配测：有 actuator 才有端点；payload 形状；无配置缺省语义。
- 先例：AlertRuleAssemblyTest（fail-fast 口径）、BuzhouHealthEndpoint
  既有测试。

## Out of Scope

- Spring Boot AvailabilityChangeEvent / actuator health groups 联动
  （宿主侧接线）；
- 裁决历史、flap 窗（312 告警层职责）；
- 按机制的动态重归类（运行时改归类——刷新事件族另议）。

## Further Notes

- 新公共类型随轮 regenerate API 快照。
- 与 312/330 关系：312 管「何时报」、330 管「何时故意不报」、本轮管
  「报了之后 K8s 该做什么」——健康治理链闭合。
