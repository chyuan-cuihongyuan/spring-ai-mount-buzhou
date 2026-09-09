# Wayfinder Map — Buzhou 会话黏性路由提示（effort #415，D 会话第 16 轮）

> D 会话第 16 轮。勘察（2026-09-08）：多实例部署的黏性路由在 runbook
> 里是**口头要求**（"推荐部署：粘性路由 + 租约独占"）；resilience
> 装配的 WARN 也提示——但**路由事实源无工具面**：LB 该按什么键哈希、
> 实例间是否一致、路由是否均匀，全靠运维手搓。346 面板每行有会话
> 无亲和键。

## Destination

`core.session.SessionAffinity`（Ketama/一致性哈希ring 的最小面借镜——
确定性键先行）：`key(appId, sessionId)` = sha256("appId|sessionId")
前 8 hex（稳定跨实例——路由器/LB 的哈希锚）；`bucket(appId, sessionId,
buckets)` = 无符号取模（0..buckets-1——实例/桶位分配）；会话面板
（346）每行加 affinityKey+affinityBucket（buckets 由 yml
`buzhou.sessions.affinity-buckets`（默认 16）供面板展示位）；文档化
LB 配方（nginx hash $arg_affinity consistent /网关按同一 sha256 取模）。

## Notes

- 号段：spec 415 / T721–T722 / impl-388。
- 借鉴源：Ketama（memcached 一致性哈希——libketama 生态标准）；
  Buzhou 侧最小面 = 确定性键 + 桶位（ring 本体在 LB——不在进程内）。
- 纪律：纯函数无状态（跨实例零协调天然一致）；键不掺实例名（掺了就
  不黏）；sha256 而非 hashCode（JDK string hash 跨语言实现不一）。

## Out of scope

- 进程内 LB/请求转发（Buzhou 不做流量入口）；一致性哈希 ring 本体；
  会话迁移再均衡提示；自动实例注册。

## Tickets

- [x] [T721 SessionAffinity 纯函数](../tickets/T721-session-affinity.md)
- [x] [T722 面板接线 + yml](../tickets/T722-affinity-dashboard.md)
