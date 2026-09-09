# Wayfinder Map — Buzhou 会话优雅排水（effort #106，B 会话第 18 轮）

> B 会话第 18 轮。主题池「会话 drain」：维护窗口要下线会话——现在只能硬关
> （在飞 Turn 被斩）。借鉴 K8s pod drain：先拒新（Terminating）、等旧完
> （in-flight 排空）、再下线。

## Destination

SessionDrainCoordinator（core/session）：beginDrain 拒新 + enter 记在飞
（AutoCloseable Lease）+ awaitDrained 等排空（带预算）+ ErrorCode.SESSION_DRAINING。
维护序列化：drain → 排空 → 归档/关闭。

## Notes

- 号段：B=奇数 spec（本轮 155）。
- 与 SpawnGate.signalDrainStarted（runtime 级）正交：那是整个 runtime 停机，
  这是单会话维护下线。

## Decisions so far

- 排空等待有预算（超时返回 false 不死等——调用方决定升级硬关）。

## Not yet specified

- 与归档器（spec 97）串联的维护流水线；autoconfig 接线。

## Out of scope

- 沿用 #7–#105；跨实例会话迁移。

## Tickets

- [x] [T513 SessionDrainCoordinator（拒新/记在飞/等排空）](../tickets/T513-drain.md)（impl-290）
- [x] [T514 排水回归（拒新/排空/超时/未排不拒）](../tickets/T514-drain-tests.md)（impl-290）
