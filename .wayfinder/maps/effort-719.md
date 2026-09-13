# effort #719 — 供应商限流头前瞻读数

- 会话：G 会话 700 系第 20 轮 ｜ spec [719](../../../docs/spec/719-provider-ratelimit-signals.md) ｜ 票 [T1038](../tickets/T1038-provider-rl-signals.md)/[T1039](../tickets/T1039-provider-rl-signals-verify.md) ｜ impl619
- 借鉴：OpenAI API x-ratelimit-* 头（openai-python ≈25K star）——429 之前的余量前瞻

## 勘察（排重）

- DefaultErrorClassifier 只在 **429 后**解析 Retry-After（事后）；x-ratelimit-remaining-* 前瞻头全库零解析。
- 本地 ModelRateLimiter 是自配额（不知供应商实时余量）。

## 决定

`ProviderRateLimitSignals`（resilience 纯静态）：parse(HttpHeaders)→Signals(remaining/limit requests+tokens，Long 可空)+resetDuration（reset 头解析）+requestUtilization()/tokenUtilization()（1−余/限，缺限缺余→NaN）+pressureLevel()（NONE/MEDIUM/HIGH：util≥0.8/≥0.95）；全字段 null-safe 畸形值 fail-safe 跳过；无任何头 → empty。纯解析原语（消费端自接响应头——advisor 拦截接线留后续）。

## 测试

全量头解析+利用率精确/压力三级/缺头 empty/畸形值 fail-safe。

## 诚实边界

头名按 OpenAI/Anthropic 通行约定（不同供应商名异——宿主可先归一）；前瞻不等于自动降速（消费者决定动作）；单响应快照口径（无历史平滑）。
