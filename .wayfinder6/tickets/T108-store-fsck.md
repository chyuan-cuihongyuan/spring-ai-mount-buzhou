---
Type: task
Status: closed
---
## Question

Store 一致性校验工具 fsck（新缺口，运维面）：跨五 store（message/summary/state/fact/observability）+ spill 无对账工具；生产长期运行后可能积累孤儿（summary 无对应 session、state 键残留、spill 证据无消息引用、观测记录膨胀）。需要决策：fsck 形态（静态工具类 vs runtime API vs 独立 main 类）、检测项清单与判据、修复策略（只报 vs 报+可选清除）、输出格式（结构化报告对象 + 文本渲染）。产出 spec 29 + impl 切片。

## Resolution

AFK 自决（授权同 effort #5，可推翻）：

1. **形态：core 公共 API `StoreFsck.run(BuzhouStores)` 静态入口 + `StoreIntegrityReport` 结果对象**——不做独立 main（部署形态多样，嵌入应用/运维脚本调用更通用）。
2. **检测项（v1 六项）**：① summary 有记录但 message store 空（孤儿摘要）；② state 键前缀合法但 session 无消息（残留状态）；③ fact TTL 过期未清；④ spill 证据无任何消息引用（孤儿证据，需 T105 引用表）；⑤ observability 记录 session 无消息（悬挂观测，只报不清——审计保留价值）；⑥ store 间 sessionId 集合差集对账。
3. **修复策略：只报 + 可选清除**——`run(stores)` 只读报告；`repair(stores, report, predicates)` 按检测项选择清除（默认全部 false 不动）；观测记录永不自动清。
4. **输出**：`StoreIntegrityReport`（per-check：findings 计数 + 样例列表上限 20 + 严重级 info/warn/error）+ `renderText()` 人读渲染（runbook 引用）。

### 闭合细化（实现期定稿）

- v1 检测项定稿为四项（孤儿摘要/残留 state/泄漏租约/悬挂观测）：spill 由其自有机制治理（spec 26 账本+sweep+TTL）；facts 属 memory 内部存储（fog）——原决议六项收缩。
- 会话全集 = 观测 listSessionSummaries 数字游标分页 + extras 补充（全集完整性依赖观测留痕，诚实声明）；合成会话 __buzhou.webhook__ 天然豁免。
- 一会话可合法命中多项（如 仅租约+观测 → dangling-lease + dangling-observability）。
- spec 29 落档；runbook 排查树引用。
