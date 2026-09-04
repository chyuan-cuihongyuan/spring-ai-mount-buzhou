# Wayfinder Map — Buzhou 健康三探针分层（effort #332，C 会话第 33 轮）

> C 会话第 33 轮。impl-41 立了健康面聚合（/actuator/buzhou 一屏全机制），
> 但所有 DOWN 混在一个层面：store 不可达（摘流量能治）与内部泄漏
> （重启能治）与预热未完（等能治）不可区分——K8s 探针分层的经典问题：
> liveness 失败→重启、readiness 失败→摘流量、startup 失败→等待，
> 用错探针轻则无效重则恶性循环（外部依赖故障触发重启风暴）。

## Destination

`BuzhouProbes`（core/health：机制→探针类归类 + 三类裁决——DOWN 只来自
同类机制 DOWN，UNKNOWN 不连累（312「未启用≠失能」同口径））+
`/actuator/buzhou-probes` 端点（liveness/readiness/startup 三裁决 +
failing 清单）+ yml 面 `buzhou.health.probes.{liveness,startup}-mechanisms`
（缺省全归 readiness——保守默认：buzhou 机制故障默认摘流量不重启）。

## Notes

- 号段：spec 332 / T655–T656 / impl-355。
- 借鉴源：Kubernetes probes（liveness/readiness/startup 三探针语义）。
- 纪律：未配置 = 端点在但三类只含 readiness 全量（与既有健康面同底）；
  机制名引用启动 fail-fast（312 同口径）；空类裁决 UP（无要求即满足）。

## Decisions so far

- 缺省归类 readiness（保守：外部依赖型故障最常见，重启治不了不重启用）。
- liveness/startup 仅 yml 显式点名——内部型机制（泄漏/看门狗）由宿主
  按部署形态决定是否升级为重启信号。
- 端点只读、与 /actuator/buzhou 并列（不合并——聚合面与裁决面职责不同）。

## Out of scope

- Spring Boot AvailabilityChangeEvent 联动（宿主经 health groups 自行接）；
- 探针失败的历史/抖动窗（flap 吸收属 312 告警层职责）。

## Tickets

- [x] [T655 BuzhouProbes 归类+三裁决](tickets/T655-probes.md)
- [x] [T656 端点 + yml 装配 + 收口](tickets/T656-probe-endpoint.md)
