# 02 — 五类归一化错误分类 + ProviderErrorClassifier SPI + 内置 provider 分类器 + 限流 Retry-After

**What to build:** 把错误分类补全到五类（限流/鉴权/内容/网络/未知）+ 默认可重试决策表 + 可配置覆盖；`ProviderErrorClassifier` SPI + 内置 OpenAI/Anthropic/RestClient 系默认分类器；内容拒绝（元数据、不抛异常）识别为「内容」类并上报、不被重试；429 限流重试尊重 `Retry-After` 头。从用户视角：跨 provider 错误口径一致、内容拒绝可观测、429 按 Retry-After 退避。

**Blocked by:** 01

**Status:** done

## 范围

- **五类枚举 + 默认可重试表**：限流/网络 = 重试；鉴权/内容/未知 = 不重试（保守、可预期）；可经 `retryable-categories` 配置覆盖。
- **`ProviderErrorClassifier` SPI**：把「异常 + 响应元数据 → 类别」做成可扩展点；内置 OpenAI / Anthropic / RestClient 系默认分类器（按 provider 穷举其异常形态）。若 provider 覆盖面过大，可按 provider 分批提交，但 OpenAI（最常见）须在本票落地以保证可演示。
- **内容拒绝（静默通道）**：provider 内容过滤导致的元数据标记（如 `finishReason=CONTENT_FILTER`，**不抛异常**）→ 归 CONTENT 类；`content-refusal-detected` 事件；本层只分类 + 保证 afterModel/onModelError 可观测，**治理策略归内容安全机制（不在本票）**。
- **`error-classified` 事件**（带五类标签）进 observability。
- **Retry-After**：429 限流的 `Retry-After` 头解析 + 钳制到 max-backoff，喂给重试退避（补 01 的纯指数退避）。
- **`retryable-categories` 配置项**接入 `buzhou.resilience.*`。

## 验收

- [ ] OpenAI/Anthropic/RestClient 系异常被正确归入五类（**分类器纯函数表测试**，按 provider 穷举——本组次缝合点）
- [ ] 429 / 网络按默认表重试；鉴权（401/403）/ 未知默认不重试、快速失败
- [ ] 内容拒绝被识别为 CONTENT、不重试、`content-refusal-detected` 事件上报、afterModel 可见
- [ ] `error-classified` 事件带正确类别标签
- [ ] `retryable-categories` 覆盖默认表生效（如把未知配为重试）
- [ ] 429 的 `Retry-After` 被尊重且钳制到 max-backoff
- [ ] e2e（内容拒绝透传 / 鉴权快速失败 / 429+Retry-After）+ 分类器表测试通过

## 备注

- 管辖 Spec：`.scratch/model-resilience/spec.md`「归一化错误分类」决策表。
- 若内置 provider 分类器体量超单上下文窗口，可按 provider 拆为后续小票，但 OpenAI 须随本票交付（否则常见 provider 仍落 UNKNOWN）。
- 内容拒绝治理不在本票：归 12 号票内容安全机制；本层只负责分类 + 可观测。
