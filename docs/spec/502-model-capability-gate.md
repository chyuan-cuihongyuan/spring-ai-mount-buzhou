# Spec 502 — 模型能力注册表与能力门（effort #502）

> wayfinder map：`.wayfinder/maps/effort-502.md`（T755–T756）。E 会话第 3 轮。

## Problem Statement

路由面（加权 339/WRR 199/时延排序 64/降级链 15）全按流量形态分流，
无按请求需求的能力匹配：vision 请求路由到纯文本模型、带工具请求路由
到不支持 function calling 的模型 → 供应商 400 事后错。LiteLLM router
为每模型声明 capabilities（supports_vision/function_calling）事前匹配。

## Solution

`resilience.capability` 三件（注册表声明 + 事前门）：

- **ModelCapabilities**（record：`vision, tools` 布尔 + `contextWindow`
  int，未知 -1）——每模型一行声明。
- **ModelCapabilityRegistry**（yml `buzhou.resilience.model-capabilities.
  <model>` → capabilities map；Binder 装配，map 空=无 bean）。
- **CapabilityGateAdvisor**（BaseAdvisor，链序 +620——rate-limit(+650)
  内）：请求需求检测——任一 `UserMessage.getMedia()` 非空 = 需 vision；
  `ToolCallingChatOptions.getToolCallbacks()` 非空 = 需 tools。当前模型
  （`buzhou.model-name`，默认 unknown 同口径）**已注册**且缺能力 →
  `BuzhouException(ARGS_VALIDATION_FAILED)` fail-fast；**未注册模型 =
  零门零行为**（NOOP 先例同族——不声明不误拦）。

## User Stories

1. 作为多模型宿主，我想声明每个模型的能力面，so vision/工具请求在路由
   前被拦下（结构化错误）而非供应商 400（事后难排查）。
2. 作为运维，我想未注册模型完全不受影响，so 声明是渐进的（逐模型补齐）。

## Implementation Decisions

- 拒绝词汇 `ARGS_VALIDATION_FAILED`（NON_RETRYABLE——换模型可解，重试
  同模型无益，故不进重试分类；拒绝在 nextCall 前抛 → HookAdvisor.
  onModelError 可兜底——并发舱/限流同语义）。
- 单模型名口径（buzhou.model-name）；加权路由内部各模型能力门留扩散。
- 检测走公开 API（UserMessage.getMedia / ToolCallingChatOptions.
  getToolCallbacks）——零反射零依赖。

## Testing Decisions

- 未注册模型：带 media/带工具请求透传零行为。
- 注册无 vision：media 请求 → BuzhouException(ARGS_VALIDATION_FAILED)
  且文案含能力名与 yml 键；纯文本请求照常。
- 注册有 tools 无声明缺省：带工具请求照常（声明了 tools=true）。
- yml：map 声明出 bean、缺席无 bean。

## Out of Scope

- 能力感知自动路由选择（后续扩散轮）；json-mode 检测；per-request 协商。

## Further Notes

- 新公共类型 `ModelCapabilities`、`ModelCapabilityRegistry`、
  `CapabilityGateAdvisor`、`BuzhouModelCapabilityProperties` 随轮
  regenerate 快照 + api-surface.md 加行。
