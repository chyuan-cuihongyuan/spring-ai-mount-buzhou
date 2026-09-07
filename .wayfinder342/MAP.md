# Wayfinder Map — Buzhou 维护窗口 cordon（effort #342，C 会话第 43 轮）

> C 会话第 43 轮。计划内维护（版本升级/数据迁移）期间不想新会话进来：
> K8s cordon（节点标记不可调度）+ 维护窗是标准解。335 已有 spawn 准入
> 地板——但单写者：维护 cordon 与预算冻结会互相覆盖（cordon 结束
> set LOW 会压掉仍在烧穿的 budget 冻结）。

## Destination

`SpawnAdmissionFloor` 升级多源合成（named source 各自 set，floor =
语义最高者——维护/冻结正交不互踩；default 源兼容 335 既有调用）+
`MaintenanceCordon`（yml 声明窗 buzhou.maintenance.{from,until,reason}
+ 运行时事故按钮 cordon(reason)/uncordon——325「按钮必须预先在场」
纪律 bean 恒在；窗内抬 maintenance 源 HIGH、窗出落回；SmartLifecycle
自管虚拟线程轮询伪时钟可注入）。

## Notes

- 号段：spec 342 / T675–T676 / impl-365。
- 借鉴源：Kubernetes cordon/uncordon（节点不可调度标记）+ 维护窗。
- 纪律：与 335 冻结语义正交——地板取两源最高（都抬 HIGH 同向，谁先
  结束谁落回，另一方仍生效）；拒绝事件沿用 admission-floor 原因。

## Decisions so far

- 地板 get() = 各源语义最高（序数最小）；无源默认 LOW。
- 窗已过期的 yml（until < now）启动即 no-op（诚实：过期窗不追溯）。

## Out of scope

- 多窗列表（单窗+运行时按钮已覆盖——列表等真需求）；在途会话处置
  （cordon 只拒新 spawn 不动在途——drain 走 318 PDB）。

## Tickets

- [x] [T675 地板多源合成](tickets/T675-floor-sources.md)
- [x] [T676 MaintenanceCordon + 装配 + 收口](tickets/T676-cordon.md)
