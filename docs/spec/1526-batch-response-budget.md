# 1526 — 批级工具结果回喂预算

> 来源：M 会话第 29 轮 = effort #1526（impl 1129）。Anthropic 工具结果 token 预算思想；ToolResultLimiter 单工具维度的批级扩展。

## 背景

单工具限幅（ToolResultLimiter）管不住批维度：N 个工具各自限内但合计巨大的回喂仍会撑爆上下文。

## 目标

- `HarnessToolCallingManager.applyBatchBudget`：批总量超预算 → 按响应长度降序贪心截大者（小结果保留完整），截断件带标记+保留量+重查指引；指标 `buzhou.tools.batch-truncated`；
- `BatchResponseBudgetHolder` 进程级（`buzhou.core.tool-batch-response-budget` > 0 声明即启用，HarnessAssembler 构造期拾取）；0 = 关零行为（默认）。

## 兼容性

opt-in：未配置零行为；配置后仅批总量超限时生效。
