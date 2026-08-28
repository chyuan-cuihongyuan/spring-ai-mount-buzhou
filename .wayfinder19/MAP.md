# Wayfinder Map — Buzhou 技能目录语义排序（effort #19）

> effort #19（已闭合 2026-08-29），延续 #5–#18；收口后累计 166 轮 / impl 1–205。
> 本 effort 主线：**技能目录语义排序注入**——fog 毕业生：目录注入预算（默认 64）按
> 注册序硬截断，技能多于预算时「最相关的技能可能恰好被截掉」。借鉴 Claude Code
> skills 按需加载（目录只放清单、正文按需取）+ LiteLLM semantic routing（embedding
> 相似度选路）思想：注入前按「当前用户问法」与「技能名+描述」的 cosine 相似度排序，
> 预算内保最相关；无嵌入模型 / 关闭时行为零变化。

## Destination

`buzhou.skills.semantic-ranking.enabled=true` + EmbeddingModel bean 时：目录清单按当前
轮用户问法语义排序后应用预算截断（预算内保最相关技能）；技能向量缓存（name+description
哈希失效）；嵌入失败/无问法回退注册序（旁路计数可观测）；默认关闭零行为变化；
新键 2 个登记绑定矩阵。

## Notes

- 领域/测试哲学/10K★ 政策/AFK 授权：沿用 effort #6–#18 MAP Notes。
- 外部事实源：Claude Code skills（清单注入 + 按需 load，目录预算纪律）；LiteLLM
  semantic routing（embedding 相似度决定路由目标——同款相似度判定思想）。
- 本地勘察（2026-08-29）：`SkillCatalogRendererImpl.renderCatalog(sessionId)` 不见当前
  问法；`InjectionViewProcessor.process(sessionId, stored, turn)` 的 stored 尾部 USER
  消息即问法来源；`DefaultSkillRegistry.listForPage` 注册序截断 + 溢出计数；skills
  模块无 spring-ai 依赖（需加 optional spring-ai-model 引 EmbeddingModel 接口）。
- 诚实边界：排序质量归嵌入模型（框架只保证排序稳定 + 预算语义 + 失败回退）；
  嵌入成本 = 每轮一次问法向量 + 缓存命中的技能向量零成本。
- 过程教训沿用：新键必须登记绑定矩阵（T214 防线）；examples 依赖改动后全量 install。

## Decisions so far

- **queryHint 走接口默认重载**：`renderCatalog(sessionId, queryHint)` default 委托旧签名
  （第三方渲染器零破坏）；memory 侧取 stored 尾部 USER 文本传入（无则 null）。
- **排序器 = skills 模块新类 SemanticSkillRanker**（optional EmbeddingModel 构造注入）：
  cosine 降序 + 原序稳定并列；技能向量 ConcurrentHashMap 缓存（key=name，text 变更失效）。
- **预算语义不变**：截断数量与溢出提示口径不动——只改变「哪 N 个进入预算」。
- **enabled 而无 bean = fail-fast 带修法**（与语义缓存同口径）；嵌入调用失败 → 回退
  原序 + bypass 计数（降级不阻断注入）。

## Not yet specified

- RunawayHook / TokenBudgetHook 计数写路径同型原子化（fog 沿用）。
- 观测 OLAP 导出（fog 沿用）。
- 技能语义去重/合并（同名近义技能识别——量级证据后议）。

## Out of scope

- 沿用 effort #7–#18 Out of scope 全部条目。
- 技能正文检索/加载的语义化（load_skill 语义匹配——正文面另议）。
- 跨会话排序反馈学习。

## Tickets

初始 5 张（T264–T268，按轮逐张闭合）：

- [x] [T264 renderCatalog(sessionId, queryHint) 重载 + memory 问法传入](tickets/T264-hint-plumb.md)（impl-204；core default 委托 + memory 尾部 USER 提取）
- [x] [T265 SemanticSkillRanker（cosine + 向量缓存 + 回退）+ pom optional 依赖](tickets/T265-ranker.md)（impl-204；spring-ai-model optional）
- [x] [T266 装配 + 新键 semantic-ranking.enabled（fail-fast 无 bean）](tickets/T266-ranking-wiring.md)（impl-204；autoconfig ObjectProvider 注入）
- [x] [T267 红队 + 单测（相关性保预算/失败回退/无问法原序/禁用零变化）+ perf 哨兵](tickets/T267-ranking-redteam.md)（impl-205；skills 6 例 + memory hint 1 例 + 矩阵 enabled=true 全路径）
- [x] [T268 文档面 + 绑定矩阵登记 + 里程碑 verify + 收口](tickets/T268-effort19-closing.md)（impl-205；metadata + ENV_READ 登记；快照 +1；累计 166 轮）
