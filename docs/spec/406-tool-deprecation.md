# Spec 406 — 工具退役通告（effort #406）

> wayfinder map：`.wayfinder/maps/effort-406.md`（T703–T704）。D 会话第 7 轮。

## Problem Statement

工具改名/换实现时没有退役过渡期：旧名直接删 → 模型选工具失败；保留 →
模型继续用旧的，何时能真删无人知晓。缺 K8s API deprecation 的成熟
做法——多版本共存 + 通告随定义下发 + 计量迁移进度。

## Solution

`core.exec.DeprecatedToolCallback`（K8s API deprecation 借鉴）：

- 包装退役工具（装饰器家族同款——定义透传**除描述外**）：
  - **描述前缀**（模型可见 steering，不注入提示词）：
    `[DEPRECATED since {since}{, removal {removal-in}}] {message}`
    `→ 优先使用 {successor}`——模型读定义即知避让；
  - 每次调用（两 call 形态）发事件 `tool.deprecated-called`
    {tool, successor, since} + 计数器 `buzhou.tools.deprecated-calls`
    （tag tool——yml 声明集有界）；
  - **不阻断**：退役≠移除（阻断域归 325 kill switch）；调用照常透传。
- 装配：yml `buzhou.tools.deprecated.<toolName>.{since, removal-in,
  successor, message}` 声明即装配——RuntimeConfig customizer 经
  `wrapToolCallbacks` 匹配名包装（未声明/未命中零变化）；空表无 bean。
- 迁移进度：deprecated-calls 计数衰减 = 迁移完成信号（真删除时机由
  数据说话——K8s 同法）。

## User Stories

1. 作为工具维护者，我想旧工具带退役通告继续可用，所以 模型平滑转向
   新工具而旧流程不炸。
2. 作为工具维护者，我想看到退役工具的调用计数，所以 「何时能真删」
   由 usage 数据回答。
3. 作为宿主，我想 yml 声明即生效，so 退役无需改代码重发。

## Implementation Decisions

- 描述改写是唯一模型可见面；事件/计数是唯一机器可见面。
- successor 可选（无继任者的纯下线场景只带 since/message）。

## Testing Decisions

- 描述前缀形态（全字段/最小字段）；调用透传 + 事件 + 计数；
- 未命中名零包装；yml 装配（map 形态）+ 空/未配置无 bean；E2E 经
  runtime 验证包装真挂上。

## Out of Scope

- 阻断模式；按租户退役；自动迁移改写；面板端点。

## Further Notes

- 新公共类型 `DeprecatedToolCallback` / `BuzhouToolDeprecationProperties`
  随轮 regenerate 快照。
