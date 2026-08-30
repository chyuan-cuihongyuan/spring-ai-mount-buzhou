# Wayfinder Map — Buzhou skill_search 语义面（effort #33）

> effort #33，延续 #5–#32（累计 179 轮 / T1–T296 / impl 1–218）。
> 主线：**检索语义面**——#19 ranker 只用于目录注入；skill_search 命中集仍是注册序、
> 零命中只有旧文案。复用同一 ranker：命中按相似度排序 + 零命中语义近邻提示。

## Destination

ranker 非 null 时：命中集按 query cosine 排序（相关在前）；零子串命中给语义最近
3 条「语义最近」提示；无 ranker 行为与历史一致；零新键（复用 semantic-ranking.enabled）。

## Notes

- 诚实边界同 spec 59/73（判别力归嵌入模型；近邻无阈值——排序质量不承诺）。

## Decisions so far

- 检索与目录注入共享同一 ranker 实例（向量缓存互通）。

## Not yet specified

- 检索阈值化（相似度下限）；混合排序（子串命中加权）。

## Out of scope

- 沿用 #7–#32；新配置键；正文检索。

## Tickets

- [x] [T297 检索命中语义排序 + 零命中近邻提示](tickets/T297-search-semantic.md)（impl-219）
- [x] [T298 红队 + 文档 + verify + 收口](tickets/T298-search-close.md)
