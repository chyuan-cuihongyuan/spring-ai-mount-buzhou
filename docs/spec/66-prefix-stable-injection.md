# Spec 66 — 前缀稳定注入序（effort #26）

> wayfinder map：`.wayfinder26/MAP.md`（T283–T284）。OSS 借鉴：Anthropic prompt
> caching 最佳实践 / OpenAI automatic prefix caching。

## Problem Statement

注入块序为 摘要→事实→清单：摘要与事实每轮可能变化（压缩/新事实），却排在最前——
provider KV-cache 的前缀命中在第一个变化块处即断裂；跨轮最稳定的技能清单反而排尾，
前缀稳定收益为零。

## Solution

opt-in `buzhou.memory.prefix-stable-injection`（默认 false）：块序切换
catalog→summary→facts→recent——最稳定块前置，前缀命中面最大化；默认关块序与内容
零变化。

## User Stories

1. 作为长会话用户，我要稳定块前置，所以 provider 前缀缓存跨轮命中（成本/延迟收益归 provider 计费行为）。
2. 作为红队，我要清单块跨轮字节级一致被钉住，所以结构事实不回退。
3. 作为既有用户，我要默认关零变化，所以升级零风险。

## Implementation Decisions

- setter 注入（构造器涟漪规避）；单一拼装点分支；摘要/事实块内部语义不动。
- 诚实边界：摘要轮间更新仍会断其后前缀——只优化「清单稳定段」的命中面。

## Testing Decisions

- 跨轮首块字节一致断言；默认序回归（无摘要无事实场景两模式等价）。

## Out of Scope

- 块内容变化频率治理；provider cache_control 头；摘要增量形态。

## Further Notes

- 命中增益不可框架内测（provider 计费行为）——结构事实（稳定前置）是可测代理。
