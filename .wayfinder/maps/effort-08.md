# Wayfinder Map — Buzhou 生产级纵深（effort #8）

> effort #8，延续 #1–#7（#5：22 轮；#6：9 轮；#7：20 轮）。主线：**能力补全与社区/对抗面**——
> skill 检索、死信重放、保留策略、跨 store 迁移、新能力攻击面的红队覆盖、健康端点、社区文件。
> 到达 = 20 轮自迭代落地、全仓 verify 绿、MAP 闭合。

## Notes

- 领域/测试哲学/10K★ 政策/AFK 授权：沿用 effort #7 MAP Notes。
- 图前勘察（2026-08-15）：LoadSkillTool 为 ToolCallback 直实现（skill_search 同款）；
  effort #7 收口 fog：skill_search / 索引 CLOSED 保留 / 压缩梯子事件化等。
- 新面识别：新能力（多模态/结果限幅/摄取）无红队对抗覆盖；.github 社区文件（ISSUE/PR 模板）缺失；
  health details 未含 outbox/索引新维度。

## Decisions so far

- [T132 skill_search 检索工具](../tickets/T132-skill-search.md) — query 子串检索不截断全集（listAllFor 新 default + 覆写）；上限 20 + load_skill 指引；绑定可见性沿用；spec 37 §A。
- [T133 死信重放 API](../tickets/T133-deadletter-replay.md) — replayDeadLetters() 一键迁回 outbox（attempts 清零/容量满部分重放/损坏丢弃）；spec 37 §B。
- [T134 索引 CLOSED 行保留](../tickets/T134-index-retention.md) — purgeOlderThan（三实现覆写，ACTIVE 永不扫）+ 观察者 1/64 惰性清扫 + closed-retention 可配；spec 37 §C。
- [T135 压缩梯子事件化](../tickets/T135-ladder-compaction-events.md) — CompactionListener(sessionId,result,evictRatio)；梯子每级折入都发事件（级可区分）；spec 38 §A。
- [T136 跨 store 迁移器](../tickets/T136-store-migrator.md) — SessionMigrator.migrate 复用 export/import 管线（重映射/keepIds/指标）；轻量工具定位；spec 38 §B。
- [T137 黄金轨迹 C](../tickets/T137-golden-c.md) — GoldenTrajectoryEffort8Test 四轨迹（半开三段子序列/检索不截断/死信重放/清扫三保护）；spec 38 §C。
- [T138 红队新攻击面](../tickets/T138-redteam-new-surface.md) — 多模态注入/工具结果注入确定性对抗用例（观察档；promptfoo 词汇不可表达）；spec 39 §A。
- [T139 观测背压审计](../tickets/T139-observability-backpressure-audit.md) — 满队=阻塞背压而非丢弃（测试钉住+零丢失）；javadoc/runbook 告警两注记；spec 39 §B。
- [T140 health 新维度](../tickets/T140-health-new-dimensions.md) — WebhookOutboxHealth（水位四键）+ SessionIndexHealth（wired/采样）条件装配；恒 UP 语义（旁路/优化面不 DOWN）；spec 39 §C。
- [T141 社区文件](../tickets/T141-community-files.md) — bug_report 结构化（模块/store/证据/披露提示）+ config.yml（security 私密引导）；既有三模板勘察纠偏后不动。
- [T142 javadoc @since 审计](../tickets/T142-javadoc-since-audit.md) — 脚本审计 24 类型全缺 → 批量注入 @since 1.0.0；编译绿。
- [T143 黄金轨迹 D](../tickets/T143-golden-d.md) — G17 迁移双型（H2→内存跨形态）+ G18 outbox 水位；examples 增 store-jdbc/h2 test 依赖；spec 38 §D。
- [T144 perf 哨兵增补](../tickets/T144-perf-sentinels-2.md) — skill_search/死信重放存储面/迁移往返三哨兵（baseline 落档）；perf 桥补 requeueDead/SESSION_ID。
- [T145 examples 演示第二批](../tickets/T145-examples-demo-2.md) — 检索两步发现/死信运维重放/迁移演练三用例（ToolResponse 断言口径注记）。
- [T146 runbook 第四轮](../tickets/T146-runbook-4.md) — 死信一键化/保留期调优/迁移步骤/health 两面解读。
- [T147 CONTEXT/api-surface 增补](../tickets/T147-context-api-addendum.md) — 术语节 6 条 + effort#8 公共面（含 closed-retention 属性绑定补遗：Core 第 3 参 + 兼容构造）。
- [T148 配置元数据增补](../tickets/T148-config-metadata-2.md) — buzhou.index.closed-retention 入档 + 绑定验证用例。

## Not yet specified

- LLM 响应缓存（语义边界未清，长期 fog）。
- dashboard 前端工程化（#4 边界）。
- outbox Redis 服务端 SCAN 下推（当前键集合侧过滤已够用，量级不抵复杂度）。

## Out of scope

- 沿用 effort #7 Out of scope 全部条目。

## Tickets

19 张 T132–T150 **全部闭合**（2026-08-15）；impl 切片 105–121 全部落地并合入 main。
**Frontier**：∅——effort #8 到达目的地（fog 候选见收口记录）。

<details><summary>初始票清单（历史）</summary>

初始 19 张（T132–T150，均含 AFK 决议，按轮逐张闭合）：

- [T132 skill_search 检索工具](../tickets/T132-skill-search.md)
- [T133 死信重放 API](../tickets/T133-deadletter-replay.md)
- [T134 索引 CLOSED 行保留](../tickets/T134-index-retention.md)
- [T135 压缩梯子事件化](../tickets/T135-ladder-compaction-events.md)
- [T136 跨 store 迁移器](../tickets/T136-store-migrator.md)
- [T137 黄金轨迹 C](../tickets/T137-golden-c.md)（blocked-by T132/133/134）
- [T138 红队新攻击面](../tickets/T138-redteam-new-surface.md)
- [T139 观测背压审计](../tickets/T139-observability-backpressure-audit.md)
- [T140 health 新维度](../tickets/T140-health-new-dimensions.md)
- [T141 社区文件](../tickets/T141-community-files.md)
- [T142 javadoc @since 审计](../tickets/T142-javadoc-since-audit.md)
- [T143 黄金轨迹 D](../tickets/T143-golden-d.md)（blocked-by T136）
- [T144 perf 哨兵增补](../tickets/T144-perf-sentinels-2.md)
- [T145 examples 演示](../tickets/T145-examples-demo-2.md)（blocked-by T132/133/136）
- [T146 runbook 增补](../tickets/T146-runbook-4.md)（blocked-by T133/134/136）
- [T147 CONTEXT/api-surface 增补](../tickets/T147-context-api-addendum.md)
- [T148 配置元数据增补](../tickets/T148-config-metadata-2.md)（blocked-by T134）
- [T149 里程碑 verify](../tickets/T149-milestone-verify.md)（blocked-by T132–T148）
- [T150 收口](../tickets/T150-effort8-closing.md)（blocked-by T149）

</details>

## 收口记录（2026-08-15）

- **20 轮完整自迭代**（图表轮 + 18 实现轮 + 里程碑轮 + 收口轮）：能力补全（skill_search T132 /
  死信重放 T133 / 索引保留 T134 / 梯子事件 T135 / 迁移器 T136）、防线加密（黄金 C·D T137/143 /
  红队新面 T138 / 观测审计 T139 / health T140）、社区与文档（T141–142 / 145–148）、
  里程碑 T149 与收口 T150。
- **里程碑终验**：全仓 `mvn clean verify` exit=0——18 模块全 SUCCESS；
  **1140 测试 0 失败 0 错误**（50 skipped = docker/真实 LLM 门控）。
- **文档面**：spec 新增 37–39 三篇；CONTEXT「能力补全与对抗面」术语节；api-surface effort#8 面；
  runbook 第四轮；redteam README/config 观察档注记；perf baseline 第二批三哨兵；
  @since 1.0.0 补 24 类型；社区文件两补（bug_report 结构化 + config.yml 私密披露引导）。
- **实现期纠偏**（诚实记录）：图表勘察两误判被实现期纠正——社区模板已存在（T141 缩窄）；
  examples 缺 store-jdbc 依赖（T143 补 test 依赖）。
- **累计口径**：effort #5（22）+ #6（9）+ #7（20）+ #8（20）= **71 轮完整流程自迭代**；
  票号 T1–T150、impl 切片 1–121 全局连续。
- **fog 候选**（后续 effort）：skill 语义排序（评分需查询语义）；outbox Redis 服务端 SCAN 下推；
  观测管线容量可配上限告警联动；红队新面转 nightly 重放定门；观测 OLAP/多实例分布式（长期）。
