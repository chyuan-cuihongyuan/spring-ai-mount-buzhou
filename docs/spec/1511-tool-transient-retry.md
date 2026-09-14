# 1511 — 幂等工具瞬断重试自动装配通道（F1 功能缺口落地）

> 来源：M 会话第 13 轮 = effort #1511（impl 1114）。spec 05「运行期瞬断重试」承诺（design-incompleteness F1）的装配收口——动工查重发现既有 RetryingToolCallback（spec 133/302）装饰器，本通道补声明式装配 + 瞬断白名单档。

## 背景

spec 05:96-102 承诺工具瞬断重试（默认 0 次、退避 1s/2s/4s 上限 3、IO 白名单、单调用内部重试）——已有装饰器（spec 133，重试一切异常 + 幂等契约归宿主手动声明）但无自动装配通道、无瞬断白名单。

## 目标

- `IdempotentToolRetryHolder`（Holder 模式）：`buzhou.core.tool-transient-retry.enabled=true` 声明即启用——@BuzhouTool.idempotent=true 或 `idempotent-overrides` 白名单的工具在 HarnessAssembler 装配期自动包既有 RetryingToolCallback（Hooked 内层：hook 只见逻辑调用一次；重试不占额外许可）；
- 既有 `RetryPolicy` 加 `transientOnly` 档（默认 false 零行为变化；通道传 true）：`isTransient` 白名单（IOException/TimeoutException + 类名启发 Connect/Reset/5xx 族 + cause 链三层防包装漏判）——非瞬断零重试原样上抛；
- 退避/上限透传 RetryPolicy（max-attempts 默认 3、initial-backoff 1s、max-backoff 4s）。

## 兼容性

默认关（Holder null）装配不包零变化；既有手动包装路径与 spec 133 语义不变（transientOnly 默认 false）。
