# Wayfinder Map — Buzhou 工具退役通告（effort #406，D 会话第 7 轮）

> D 会话第 7 轮。勘察（2026-09-08）：core.exec 有金丝雀（324）/紧急停用
> （325）/干跑（323）——但**退役过渡期通告缺失**：工具改名/换实现时旧名
> 直接删会让模型选工具失败，保留则模型继续用旧的。K8s API deprecation
> 的成熟做法（多版本共存 + 通告随定义下发 + 计量迁移进度）无对应物。

## Destination

`core.exec.DeprecatedToolCallback`（K8s API deprecation 借鉴——通告随
定义、退役≠移除）：包装退役工具——描述前缀 `[DEPRECATED since X,
removal Y] 建议 successor Z`（模型可见的 steering）；每次调用发
`tool.deprecated-called` 事件 + 计数（迁移进度面）；不阻断（阻断域归
325 kill switch）。装配：yml `buzhou.tools.deprecated.<name>={since,
removal-in,successor,message}` 声明即 wrapToolCallbacks 全工具集匹配
包装（RuntimeConfig customizer——与 402 同独立 bean 路子）。

## Notes

- 号段：spec 406 / T703–T704 / impl-379。
- 借鉴源：K8s API deprecation（通告随 OpenAPI 定义下发、GVK 多版本
  共存、metrics 看 usage 衰减决定真删除时机）。
- 纪律：描述改写是唯一模型可见面（不注入提示词）；事件含 successor
  供宿主迁移看板。

## Out of scope

- 阻断模式（325 域）；按调用方分租户退役；自动迁移改写（模型自选替代
  ——不建议宿主硬改写）；面板端点（计数走指标即可）。

## Tickets

- [x] [T703 DeprecatedToolCallback](../tickets/T703-deprecated-tool-callback.md)
- [x] [T704 yml 装配 + E2E](../tickets/T704-deprecation-assembly.md)
