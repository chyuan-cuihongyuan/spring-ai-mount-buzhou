# Wayfinder Map — Buzhou 配置热重载（effort #110，B 会话第 22 轮）

> B 会话第 22 轮。主题池「配置热重载」：限幅器/预算/超时等运行参数改一次要
> 重启。借鉴 Caddy config reload（原子换引用 + 变更通知）与 Spring Cloud
> refresh 语义。

## Destination

ReloadableConfig<T>（core/config）：volatile 引用原子替换 + 版本号 +
订阅/退订 + 条件 CAS 更新（updateIf）。宿主持它包装任意配置 record——
watcher/接口触发 replace 即热生效。

## Notes

- 号段：B=奇数 spec（本轮 163）。
- 只提供「换与通知」原语；watcher（文件/配置中心轮询）归宿主——分层诚实。

## Decisions so far

- 同值替换也计版本（重放安全——观察方按版本幂等）。

## Not yet specified

- 与既有 properties/autoconfig 键的绑定桥；文件 watcher。

## Out of scope

- 沿用 #7–#109；配置中心客户端；diff 计算。

## Tickets

- [x] [T523 ReloadableConfig 原语（换/版本/订阅/CAS）](../tickets/T523-reloadable.md)（impl-294）
- [x] [T524 热重载回归（替换/通知/退订/条件换）](../tickets/T524-reloadable-tests.md)（impl-294）
