# Spec 168 — 目录渲染缓存（effort #128）

> wayfinder map：`.wayfinder/maps/effort-128.md`（T522–T523）。PromptPrefixCache
> （spec 126）的首个内置消费方。借鉴：vLLM radix prefix-cache 内容寻址。

## Problem Statement

技能目录每轮注入都全量重渲染（StringBuilder 循环 + 遥测计数）——目录在多数
部署里稳定不动，重复渲染是纯浪费的热点路径。

## Solution

`SkillCatalogRendererImpl` 内嵌 `PromptPrefixCache<String>`：渲染是目录内容的
纯函数，以「name|description 每行 + #overflow」规范形 sha256 为键内容寻址——
同目录二连击起命中复用（同一实例返回）；上架/解绑/改文案即换键自然失效
（无需失效逻辑——键变即 miss）；跨会话共享安全（内容相同输出相同）。
`renderCacheStats()` 暴露四计数（命中率 = 目录稳定性信号——频繁 miss 提示
目录在抖）。输出逐字节零变化（既有渲染断言回归全绿）。

## User Stories

1. 作为宿主，稳定目录的每轮注入走缓存命中，所以渲染 CPU 归零、行为零变化。
2. 作为运维，renderCacheStats 的 miss 率提示目录稳定性——频繁失效是目录
   在抖的信号。

## Testing Decisions

- 红队：同目录二连击（1 miss + 1 hit + 同实例复用）；改描述换键 miss；
  未知会话短路在缓存前（零请求）。skills 模块全量回归（含既有渲染输出断言）。

## Out of Scope

- 嵌入缓存（第二消费方——fog）；模型响应缓存（语义重不做）；容量配置。

## Further Notes

- spec 126 的「不猜语义相似」纪律在此兑现：内容寻址、逐字节规范形。
