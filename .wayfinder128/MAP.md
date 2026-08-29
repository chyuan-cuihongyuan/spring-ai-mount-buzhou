# Wayfinder Map — Buzhou 目录渲染缓存（effort #128，A 会话第 23 轮）

> A 侧票号 T501+ / spec 偶数段沿用。spec 126 fog「模型调用侧接线」的更安全
> 变体：PromptPrefixCache 首个内置消费方落在目录渲染（纯函数面）。

## Destination

目录渲染按内容寻址缓存：name+description+overflow 规范形 sha256 为键——
目录每轮注入是热点，同目录命中复用；上架/改文案即换键自然失效；命中率 =
目录稳定性信号。

## Notes

- 渲染是纯函数（输出零变化——回归 85→88 全绿含既有渲染断言）；跨会话共享
  安全（内容相同输出相同）；renderCacheStats() 观测面。

## Decisions so far

- [渲染缓存](tickets/T522-render-cache.md) — getOrLoad 惰性装载 +
  render_uncached 拆分。

## Not yet specified

- 嵌入缓存（语义排序向量复用——PromptPrefixCache 第二消费方）。

## Out of scope

- 模型响应缓存（语义重——不做）；缓存容量配置。

## Tickets

- [x] [T522 目录渲染缓存](tickets/T522-render-cache.md)（impl-295）
- [x] [T523 收口提交](tickets/T523-render-cache-close.md)（impl-295）
