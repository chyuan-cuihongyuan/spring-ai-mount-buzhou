# Spec 185 — per-tool 会话配额（effort #211）

> wayfinder map：`.wayfinder/maps/effort-211.md`（T557–T558）。借鉴：云厂商 per-API
> quota——每接口独立额度，单点爆用不拖垮总额度。

## Problem Statement

模型会陷入「单工具循环」（反复调某个贵/慢工具几十次）：日配额（spec 16）限
的是会话总量，要等全量耗尽才拦——代价已经发生；且总量拦的是「所有工具
一起」，无法表达「这个工具最多 10 次」这类细粒度预算。

## Solution

`ToolQuotaHook`（guard/hook，order 250——角色 260 之前）：

- **配置**：`Map<工具名, 上限>` + 可选 `"*"` 通配默认（未列名工具适用）。
- **计数**：会话态键 `buzhou.tool-quota.<tool>`——beforeTool 递增（放行即计，
  被拒不计）；超限 `HookResult.block`（可读理由：本会话该工具已达上限 N 次）。
- **隔离与重置**：会话态生命周期 = 天然 per-session、会话结束自然清零。
- 计数 `buzhou.tool-quota.blocked`（tool tag 有界——配额表本身有界）。

## User Stories

1. 作为宿主，贵工具标 10 次/会话——模型循环爆用被即拦，总量预算留给别的工具。
2. 作为运维，blocked 按 tool 分标签——哪个工具被爆一眼可见（模型行为侧写）。
3. 作为宿主，不配置零限制零变化。

## Implementation Decisions

- 通配 "*" 只作未列名工具的默认（列名优先）；上限 ≥1 校验。
- 会话态 Integer 计数（无 TTL——会话生命周期即窗）。

## Testing Decimals

- 达上限 block 文案；未达放行且计数递增；列名优先于通配；无通配未列名零限制；
  会话隔离（两会话互不影响）；被拒不重复计。

## Out of Scope

- 手动重置 API；跨会话共享；token 加权。

## Further Notes

- 配额三层：会话总量（16）/ 虚拟 key（124，A）/ per-tool 会话内（本轮）。
