# Spec 1804 — 前缀块命中读面（effort #1804，R5）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2809–T2810，impl 1405）。借鉴：
> vLLM block-level prefix cache / SGLang radix tree——公共前缀按块复用，块命中
> 率量化 KV/嵌入计算节省；尾块（partial block）不入缓存。

## Problem Statement

语义/响应缓存只答「整请求命中了没」，答不了「前缀复用的底子有多厚」：
Agent 会话天然共享长前缀（系统提示 + 滚动历史），整请求命中率贴地的同时
前缀块命中率可能很高——这两个读数混在一起，前缀预热/缓存投资的决策就没
有依据。

## Solution

`PrefixBlockHitStats`（buzhou-resilience/cache，静态纯函数）：

- `analyze(blockSize, prompts)` → `BlockReport(blockSize, requests, totalBlocks,
  reusedBlocks)` + `blockHitRatio()`/`blockMissRatio()`（零入账 -1 哨兵）；
- 语义对齐 vLLM：等长块切分；尾块不足整块不入账；同请求内重复块只记首见
  （跨请求再见才计复用——radix 树一次插入语义）；
- `DEFAULT_BLOCK_SIZE=64` 常量默认，调用方可另声明口径。

## User Stories

1. 作为缓存策略维护者，整请求命中率 5% 但块命中率 60% → 前缀底子厚，
   该投 radix 索引/预热而不是放弃缓存。
2. 作为容量规划者，块命中率贴地 → 每次请求都是新面孔，前缀投资无回报，
   该省下这部分基建。
3. 作为框架宿主，块口径（字符/token）自声明，纯读面零侵入零跨调用状态。

## Implementation Decisions

- 纯函数：索引全部调用局部（无静态可变状态——测试含纯函数性回归钉死）。
- 契约 fail-fast：blockSize < 1 拒绝；null 按空表、null 元素按空文本。

## Testing Decisions

- 共享前缀跨请求命中；尾块丢弃；单请求全 miss + 请求内去重；空表/null/
  全短文本哨兵；blockSize 畸形 fail-fast；同参重复调用结果一致（无状态
  残留）。首跑 1 红暴露测试数据与请求内去重语义的交互（同内容块数据修正）。

## Out of Scope

- 不实现真正的 radix 树缓存（归后续轮）；不接语义缓存 advisor 热路径。

## Further Notes

- 与 ResponseCacheKeys 正交：那是整请求键派生，这是块级复用底子读数。
