# Spec 126 — 提示前缀缓存（effort #90）

> wayfinder map：`.wayfinder90/MAP.md`（T453–T454）。#85 fog 种子⑦「前缀缓存
> 命中率（provider 侧）」收口。借鉴：vLLM / SGLang radix prefix-cache。

## Problem Statement

多轮对话与模板化提示的前缀稳定段（系统提示、few-shot 模板、历史前缀）每次都
全量重算（解析/嵌入/渲染），且没有前缀复用率的观测——提示工程是否在复读无从
判断。

## Solution

`cache/PromptPrefixCache<V>`：以「提示前缀规范形」经 sha256 为键的有界 LRU
（默认 256 条，命中续命，逐出诚实计数）。`get`/`put`/`getOrLoad`（miss 惰性
装载）/`invalidate` 四操作面 + `Stats`（requests/hits/misses/evictions）井读
快照与 `hitRate()`（无请求 0 诚实空值）。键纪律：宿主负责规范形（同前缀必须
逐字节一致）；本缓存不猜测语义相似——那是向量面的事。

## User Stories

1. 作为宿主开发者，我用前缀键缓存结构化解析/嵌入结果，所以同前缀的重复请求
   不再重算，miss 时一次装载后续命中。
2. 作为性能工程师，我读命中率与逐出计数，所以「前缀是否稳定/模板是否复读/
   容量是否过小」三个问题一个指标面回答。

## Testing Decisions

- 红队：同前缀命中续命 + 异前缀 miss 计数；LRU 封顶逐最久未用（续命者存活）
  + 逐出计数；getOrLoad 装载一次后续命中；keyOf 空白 fail-fast + put null
  拒绝；无请求 hitRate=0。

## Out of Scope

- 模型调用侧接线；语义相似命中（向量面）；分布式前缀共享；值 TTL/代际老化。

## Further Notes

- 与 ObservabilityAdvisor 正交组合：前缀指纹键由宿主从消息序列规范形派生。
