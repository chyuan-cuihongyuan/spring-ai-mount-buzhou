# 730 — 限流头跨供应商归一解析

> 来源：G 会话第 31 轮 = effort #730（719 扩散）/ [T1060](../../.wayfinder/tickets/T1060-rl-header-normalization.md) / [T1061](../../.wayfinder/tickets/T1061-rl-header-normalization-verify.md) / impl 630。

## Problem

719 parse 只认 OpenAI 头名（x-ratelimit-*）——Anthropic（anthropic-ratelimit-requests/tokens-*）多模型混布场景宿主得写两套解析。

## Solution

`parseFlexible(HttpHeaders)`：先 OpenAI 头名，全部缺项才回退 Anthropic 名——**不混合来源**（两家头并存时 OpenAI 优先，防跨供应商口径拼接）。Anthropic 维度：requests remaining/limit、tokens remaining/limit、tokens reset。其余语义（utilization/pressure/fail-safe）与 719 完全一致。

## Out of Scope
其他供应商（Google/Azure 头名后续按需）；OpenAI 的 reset 头在 Anthropic 回退分支不解析（Anthropic 无对应头）。
