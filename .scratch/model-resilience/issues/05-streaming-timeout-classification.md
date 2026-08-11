# 05 — 流式调用：deadline + 分类 + onModelError（不中途重试）

**What to build:** 把 deadline、错误分类、onModelError 应用到 `.stream()` 路径；明确 **M1 不对流式已发出的 token 做中途重试**（边界不可行）——流式失败即传播（经分类 + onModelError 兜底）。从用户视角：流式场景同样受超时 / 分类保护，失败时有兜底或事件。

**Blocked by:** 02, 03, 04

**Status:** done

## 范围

- **`ResilienceAdvisor.adviseStream`**：deadline 兜底 + 错误分类 + 终态失败触发 onModelError（复用 02 分类、03 超时、04 切面）。
- **不做中途重试**：已发出的 token 不回收，流式失败即传播；文档与测试固定该 M1 边界（M2 不在此扩）。

## 验收

- [ ] 流式慢模型（latch 阻塞）+ 短 deadline 触发 `timeout-fired` 并终止流
- [ ] 流式错误被分类、onModelError 可兜底
- [ ] 流式失败**不发生中途重试**（断言：单次失败即传播，无重试事件）
- [ ] e2e（流式超时 / 分类 / 兜底）通过

## 备注

- 管辖 Spec：`.scratch/model-resilience/spec.md`「Streaming」范围假设（M1 流式不中途重试，已与 owner 确认）。
- 中途重试出图：属 M2 及以后考量，本票明确不做。
