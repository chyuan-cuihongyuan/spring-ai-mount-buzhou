# 1004 — 维护窗历史读面

> 来源：J 会话第 5 轮 = effort #1004（[T1459](../../.wayfinder/tickets/T1459-maintenance-history-shape.md) / [T1460](../../.wayfinder/tickets/T1460-maintenance-history-verify.md) / impl 757）。借鉴：Kubernetes [cordon/uncordon 事件史](https://kubernetes.io/docs/reference/kubectl/generated/kubectl_cordon/)（节点维护生命周期在事件流可回溯）。

## Problem Statement

MaintenanceGate（spec 205，K8s cordon 借鉴）只有 volatile 当前窗——begin/end 生命周期**过窗即逝**：「上次维护是何时、为何、关了多久、期间拒了多少 Turn」不可回溯；Hook 的 micrometer 计数（buzhou.maintenance.blocked）是进程全局累计，不按窗分账。维护后复盘（维护是否达成目的、误伤多少流量）无据。

## 目标

- `MaintenanceGate` 增量（core.session，实例态——gate 本是进程级 bean）：
  - 嵌套 record `HistoryEntry(Window window, Instant beganAt, Instant endedAt, long refusals)`（仅含**已闭**窗）；
  - 闭窗有界环（容量 16，新→旧），`history()` 只读快照（不可变）；
  - `noteRefused()`：窗内拒绝计数（LongAdder；窗外调用无害无痕）；
  - `begin`/`end` 语义对外不变（begin 仍要求 reason；end 仍幂等——仅新增闭窗入史副作用）。
- `MaintenanceGateHook.beforeTurn` 在既有 counter 旁同点补 `gate.noteRefused()`（一行，包内可见）。

## 兼容性

纯增量读面：isActive/window/refusalMessage 语义逐位不变；begin/end 对外契约不变；无新配置项。

## Out of Scope

- 历史持久化（cordon 事件有 etcd 存储；本仓止步进程内有界环）。
- 多实例维护窗同步（spec 205 已注记留档，本轮不扩）。
