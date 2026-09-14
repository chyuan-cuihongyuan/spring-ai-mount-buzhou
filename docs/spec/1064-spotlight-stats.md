# 1064 — 读侧 Spotlighting 包裹判定读面

> 来源：J 会话第 64 轮 = effort #1064（[T1583](../../.wayfinder/tickets/T1583-spotlight-stats-shape.md) / [T1584](../../.wayfinder/tickets/T1584-spotlight-stats-verify.md) / impl 816）。借鉴：OWASP LLM01 间接注入防御的 spotlighting 采用率（包裹覆盖率是注入面收敛度的第一信号，MSRC delimiting + datamarking）。J 系 guard/inject 域首轴。

## Problem Statement

`SpotlightHook.afterTool()`（wayfinder T18：外部数据回灌 prompt 前随机分隔符 + 交织标记包裹）的四条路径——实际包裹、已包裹幂等跳过、拦截告示跳过、error/null 跳过——全部零计数：**聚光灯防护覆盖率不可见**。宿主无法回答「外部工具输出多大比例被包裹、幂等跳过是否高发（readback 切片行为正常）」；注入防御面覆盖存在缺口时无从对账。

## 目标

- `SpotlightHook` 增量（guard/inject，静态面）：五 `AtomicLong`。
  - `invocations`：afterTool 入口计数（总桶）；
  - `wrapped`（实际包裹）/ `alreadyWrappedSkips`（已包裹幂等跳过）/ `noticeSkips`（拦截告示跳过）/ `errorSkips`（error 或 null 结果跳过）四个结局桶。
- 嵌套 `record SpotlightStats(long invocations, long wrapped, long alreadyWrappedSkips, long noticeSkips, long errorSkips)` + `stats()` + `resetForTest()`。
- 守恒恒等式：**invocations = wrapped + alreadyWrappedSkips + noticeSkips + errorSkips**（每入口恰落一桶）。

## 兼容性

纯增量读面：afterTool 返回值、包裹格式（Spotlighting 单一事实源）与改写时机逐位不变；静态面理由同 R46–R63 先例；无新配置项。

## Out of Scope

- 按 content 来源分桶（内容敏感面——红线纪律）。
- 标记字符密度统计（wrap 参数配置面）。
