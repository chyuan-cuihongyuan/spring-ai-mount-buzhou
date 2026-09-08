# Spec 415 — 会话黏性路由提示（effort #415）

> wayfinder map：`.wayfinder/maps/effort-415.md`（T721–T722）。D 会话第 16 轮。

## Problem Statement

多实例黏性路由只有口头要求与 WARN 提示：LB 该按什么键哈希、实例间是否
一致、路由是否均匀——无事实源无工具面；会话面板每行有会话无亲和键。

## Solution

`core.session.SessionAffinity`（Ketama 确定性键思想）：

- **`key(appId, sessionId)`**：sha256("appId|sessionId") 前 8 hex——
  稳定跨实例跨语言（不掺实例名——掺了就不黏；不用 String.hashCode——
  跨实现不一）。LB 的哈希锚。
- **`bucket(appId, sessionId, buckets)`**：sha256 首 8 字节无符号取模
  （0..buckets-1）——实例/桶位分配；纯函数无状态（跨实例零协调天然
  一致）。
- 面板接线（346 会话面板）：每行加 `affinityKey` + `affinityBucket`
  （buckets = yml `buzhou.sessions.affinity-buckets`，默认 16——仅展示
  位，不影响任何机制）。
- 文档化 LB 配方（spec/map 内——nginx `hash $arg_... consistent` /
  网关同 sha256 取模）。

## User Stories

1. 作为运维，我想拿到每会话的稳定亲和键，so LB 哈希规则有事实源。
2. 作为运维，我想面板可见桶位分布，so 路由是否均匀一眼可辨。
3. 作为宿主，我想纯函数无状态，so 实例间零协调天然一致。

## Implementation Decisions

- 纯函数（静态工具）——无 bean 无状态。
- 展示位默认 16 桶——不改任何机制（零行为变化）。

## Testing Decisions

- 键确定性（同输入同键、跨调用稳定）；不同会话分散（碰撞率抽查）；
- bucket 界内 + 确定性；面板行携带两字段（yml 配 buckets）。

## Out of Scope

- 进程内 LB；一致性哈希 ring；再均衡提示；实例注册。

## Further Notes

- 新公共类型 `SessionAffinity` 随轮 regenerate 快照。
