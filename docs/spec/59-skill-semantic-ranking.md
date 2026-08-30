# Spec 59 — 技能目录语义排序注入（effort #19）

> wayfinder map：`.wayfinder19/MAP.md`（T264–T268）。OSS 借鉴：Claude Code skills
> 按需加载纪律 + LiteLLM semantic routing 的 embedding 相似度选路思想。

## Problem Statement

技能目录注入有预算上限（catalog-max-entries，默认 64）以防系统提示词膨胀：超出预算的
技能不进入清单。当前截断按注册序——当某 agent 绑定的技能数量超过预算时，「与当前
用户问法最相关的技能」可能恰好排在注册序尾部被截掉，模型于是不知道该加载它，只能靠
运维手工调整绑定关系。目录的相关性应该按轮动态判定，而不是静态注册顺序。

## Solution

注入前对候选技能做语义排序：当前轮用户问法与「技能名 + 描述」各算 embedding，cosine
相似度降序排列后应用既有预算截断——预算内保最相关技能；溢出提示口径不变。技能向量
按 (name, text) 缓存，文本不变零重复嵌入；问法向量每轮一次。可选启用
（semantic-ranking.enabled，默认关）；启用而无 EmbeddingModel bean → 启动 fail-fast
带修法；嵌入调用失败 → 回退注册序 + bypass 计数（注入链路不因排序降级而断）。

## User Stories

1. 作为绑定技能超过目录预算的 agent 作者，我希望预算截断保住与当前问法最相关的技能，所以模型能发现并加载它们。
2. 作为运维者，我希望技能向量有缓存且文本变更即失效，所以常驻技能零重复嵌入成本。
3. 作为运维者，我希望每轮只嵌一次问法向量，所以排序成本与技能数线性但无额外嵌入调用。
4. 作为用户，我希望禁用时行为与现状完全一致，所以升级零风险。
5. 作为配置错误场景的用户，我希望 enabled=true 而无 EmbeddingModel 时启动失败并带修法提示，所以不会静默失效。
6. 作为稳定性要求者，我希望嵌入失败回退注册序并计数，所以排序降级不断注入链路。
7. 作为红队，我希望「无关技能排前挤掉相关技能」的回归被钉住，所以排序语义不回退。
8. 作为红队，我希望无问法（空轮）时保持原序，所以排序不引入随机行为。
9. 作为 API 治理者，我希望新键登记绑定矩阵，所以配置防线不静默漂移。

## Implementation Decisions

- `SkillCatalogRenderer#renderCatalog(sessionId, queryHint)` default 方法委托旧签名
  （二进制兼容）；memory 的 InjectionViewProcessor 取 stored 尾部 USER 消息文本传入。
- skills 模块加 optional `spring-ai-model` 依赖（仅 EmbeddingModel 接口）。
- `SemanticSkillRanker`：embed(query) 与 embed(name+description) 的 cosine 降序、并列
  保原序（稳定排序）；技能向量 ConcurrentHashMap 缓存（key=name，value=text 变更失效）；
  任一嵌入异常 → 整体回退原序 + 计数。
- 渲染器接入点：listForPage 结果先经 ranker（enabled 且 hint 非空）再截断。
- 新键：`buzhou.skills.semantic-ranking.enabled`（默认 false；fail-fast 无 bean 带修法）。

## Testing Decisions

- 好测试钉外部行为：预算内保最相关（stub EmbeddingModel 手工向量）、失败回退原序、
  无问法原序、禁用零变化、无 bean fail-fast；不测内部缓存结构。
- perf 哨兵：64 技能排序（缓存命中）耗时上界。
- 先例：语义缓存（spec 55）的 stub EmbeddingModel 与 fail-fast 口径沿用。

## Out of Scope

- 技能正文加载的语义匹配（load_skill 面另议）；跨会话反馈学习；向量持久化。

## Further Notes

- 排序判别力归嵌入模型（框架保证排序稳定性/预算语义/失败回退）——与语义缓存同一
  诚实边界口径。
