# Wayfinder Map — Buzhou 模型能力注册表与能力门（effort #502，E 会话第 3 轮）

> E 会话第 3 轮。勘察：路由面已有加权（339）/平滑 WRR（199）/时延感知
> 备选排序（64）/降级链（15）——全部按**流量形态**分流，无按**请求需求**
> 的能力匹配：vision 请求路由到纯文本模型、带工具请求路由到不支持
> function calling 的模型 → 供应商 400（事后错）而非路由前拦（事前门）。
> LiteLLM router 的 model capabilities（supports_vision/function_calling）
> 思想。

## Destination

`resilience.capability.ModelCapabilities`（record：vision/tools/contextWindow
——布尔能力+窗口大小，未知 -1）+ `ModelCapabilityRegistry`（yml
`buzhou.resilience.model-capabilities.<model>` map，Binder 装配）+
`CapabilityGateAdvisor`（BaseAdvisor，+620——rate-limit(+650) 内：
请求需求检测——任一 UserMessage.getMedia() 非空=需 vision、
ToolCallingChatOptions.toolCallbacks 非空=需 tools；当前模型注册且缺
能力 → BuzhouException(ARGS_VALIDATION_FAILED) fail-fast 事前拦（不进
重试分类→HookAdvisor.onModelError 可兜底——并发舱同语义）。未注册
模型=零门零行为（NOOP 先例同族）。诚实边界：单模型名口径
（buzhou.model-name）——加权路由内部各模型能力门留扩散轮。

## Notes

- 号段：spec 502 / T755–T756 / impl-405。
- 借鉴源：LiteLLM Router model capabilities（supports_vision /
  supports_function_calling）。
- 拒绝词汇复用 ARGS_VALIDATION_FAILED（NON_RETRYABLE——换模型可解，
  重试同模型无益故不进重试）。

## Out of scope

- 能力感知的自动路由选择（注册表+门先行，路由扩散留后续轮）；
  json-mode/response-format 检测（Spring AI 2 无跨供应商可携信号）；
  per-request 混合能力协商。

## Tickets

- [x] [T755 ModelCapabilityRegistry yml 面](../tickets/T755-capability-registry.md)
- [x] [T756 CapabilityGateAdvisor 事前门](../tickets/T756-capability-gate-advisor.md)
