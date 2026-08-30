# Wayfinder Map — Buzhou 生产级纵深（effort #7）

> effort #7，延续 #1–#6（#5：22 轮 T81–T102；#6：9 轮 T103–T111）。本 effort 主线：
> **新九能力的工程闭合与防线加密**——契约矩阵、生命周期联动、事件化观测、黄金轨迹全覆盖、
> 装配/配置/文档/发布面收口。到达 = 20+ 轮自迭代落地、全仓 verify 绿、防线与文档齐备、MAP 闭合。

## Notes

- 领域/测试哲学/10K★ 政策/AFK 授权：沿用 effort #6 MAP Notes（Spring AI 2.0.0 单 Agent harness；examples 端到端主接缝；语义借鉴零新依赖）。
- 图前勘察（2026-08-15）：dashboard listSessions 仍走观测留痕（未消费 T109 索引）；additional-spring-configuration-metadata 三模块已有但缺 effort #6 新键；memory 微压缩无 SessionEvent 面；skills 目录线性全量注入无预算。
- 过程教训沿用：下游模块单跑一律 `-am`；`MimeType.valueOf`；断言以端上计数为准。

## Decisions so far

- [T112 index 契约测试矩阵](../tickets/T112-index-contract-matrix.md) — AbstractSessionIndexContractTest 五契约随 test-jar 发布，内存/JDBC/Redis 三实现接入（后两者附重启持久用例）；spec 33 §A。
- [T113 索引 DELETED 联动 + fsck 索引源](../tickets/T113-index-delete-fsck.md) — delete() 级联置 DELETED（审计留存；默认列表排除 DELETED，三实现+契约统一）；fsck 全集索引优先/观测回退；spec 33 §B。
- [T114 outbox 前缀扫描](../tickets/T114-outbox-prefix-scan.md) — SessionStateStore.scanByPrefix SPI（JDBC LIKE 下推/Redis 键集合过滤）；WebhookOutbox 四路径改走前缀扫描消全量读放大；spec 33 §C。
- [T115 memory 压缩事件化](../tickets/T115-compaction-events.md) — ivp.setCompactionListener（空折入零通知、lenient）+ MemoryModule 观测双写 memory.compacted（compactedCount/reclaimedChars）；spec 34 §A。
- [T116 黄金轨迹扩充 A](../tickets/T116-golden-effort6-a.md) — GoldenTrajectoryEffort6Test 三轨迹（evidence 引用生命周期/outbox 跨重启补投/压缩事件观测）；spec 34 §B。
- [T117 黄金轨迹扩充 B](../tickets/T117-golden-effort6-b.md) — GoldenTrajectoryEffort6BTest 三轨迹（导出导入往返续用/结果限幅模型侧标记/索引全生命周期含 DELETED）；spec 34 §C。
- [T118 熔断半开多探测](../tickets/T118-halfopen-probes.md) — halfOpenSuccessThreshold（默认 1 零变化；>1 连续 N 成功才恢复）；槽位不变量（在飞+已成功≥阈值）；失败立即回 OPEN；spec 35 §A。
- [T119 skills 目录注入预算](../tickets/T119-skills-catalog-budget.md) — listForPage（entries+total）+ 渲染器溢出提示（另有 N 个未列出 + catalog-max-entries 指引）；评分/skill_search 记 fog；spec 35 §B。
- [T120 MediaIntake 字节摄取](../tickets/T120-media-bytes-intake.md) — 字节→spill→MediaRef 闭环（Latin-1 二进制无损往返；spill 语义全沿用）；spec 35 §C。
- [T121 导出 facts 扩展槽](../tickets/T121-export-facts.md) — SessionExport.extensions 第 9 槽 + SessionExportExtension 接口 + memory FactsExporter（fact.* scanByPrefix 无损段）；导入回放最终一致；spec 36 §A。
- [T122 dashboard 消费索引](../tickets/T122-dashboard-index.md) — listSessionsFiltered（索引优先过滤/分页，观测回退 fromIndex=false 降级可感）+ Builder.sessionIndex 注入；spec 36 §B。
- [T123 配置元数据补全](../tickets/T123-config-metadata.md) — outbox/result-limit/backoff-cap/half-open-threshold/catalog-* 全键入档 + queue-capacity 废弃标记；skills 新建元数据；spec 21 增补。
- [T124 新能力装配面断言](../tickets/T124-starter-assembly-tests.md) — ApplicationContextRunner 四断言（默认零装配/webhook 触发/限幅属性/索引 bean 共存）；limitFor 转公共查询面；spec 21 增补。
- [T125 新能力 perf 哨兵](../tickets/T125-perf-sentinels.md) — outbox 批扫/索引过滤/导出往返三哨兵（首轮实测落档 baseline.md）；WebhookOutboxPerfAccess test-jar 桥。
- [T126 新能力演示](../tickets/T126-examples-effort6-demo.md) — Effort6CapabilitiesDemoTest 五用例（摄取闭环/备份恢复/fsck/dashboard 过滤/facts 迁移）；口径注记（摘要注入需 memory 模块）。
- [T127 runbook 新能力条目](../tickets/T127-runbook-effort6.md) — 排查两症状（死信/索引降级）+ 调优六键 + 备份恢复步骤 + V3 注记 + result-truncated 告警。
- [T128 release SBOM 附着](../tickets/T128-release-sbom.md) — deploy 后 -Psupply-chain 生成 CycloneDX BOM（json+xml）附着 GitHub Release；RELEASING 检查单同步。

## Not yet specified

- LLM 响应缓存（语义边界未清，长期 fog）。
- dashboard 前端工程化 / 观测 OLAP（沿用 #4 边界）。
- 多实例分布式语义（沿用 #2–#5 边界）。

## Out of scope

- 沿用 effort #6 Out of scope 全部条目。

## Tickets

20 张 T112–T131 **全部闭合**（2026-08-15）；impl 切片 87–104 全部落地并合入 main。
**Frontier**：∅——effort #7 到达目的地（fog 毕业候选见收口记录）。

<details><summary>初始票清单（历史）</summary>

初始 20 张（T112–T131，均含 AFK 决议，按轮逐张闭合）：

- [T112 index 契约测试矩阵](../tickets/T112-index-contract-matrix.md)
- [T113 索引 DELETED 联动 + fsck 索引源](../tickets/T113-index-delete-fsck.md)（依赖 T109✓）
- [T114 outbox 前缀扫描 SPI](../tickets/T114-outbox-prefix-scan.md)
- [T115 memory 压缩事件化](../tickets/T115-compaction-events.md)
- [T116 黄金轨迹扩充 A](../tickets/T116-golden-effort6-a.md)（blocked-by T115）
- [T117 黄金轨迹扩充 B](../tickets/T117-golden-effort6-b.md)（blocked-by T113）
- [T118 熔断半开多探测](../tickets/T118-halfopen-probes.md)
- [T119 skills 目录注入预算](../tickets/T119-skills-catalog-budget.md)
- [T120 MediaIntake 字节摄取](../tickets/T120-media-bytes-intake.md)
- [T121 导出 facts 扩展槽](../tickets/T121-export-facts.md)
- [T122 dashboard 消费索引](../tickets/T122-dashboard-index.md)
- [T123 配置元数据补全](../tickets/T123-config-metadata.md)（依赖 T118/T119 闭合后终稿）
- [T124 starter 装配测试扩展](../tickets/T124-starter-assembly-tests.md)
- [T125 新能力 perf 哨兵](../tickets/T125-perf-sentinels.md)
- [T126 examples 新能力演示](../tickets/T126-examples-effort6-demo.md)（blocked-by T120）
- [T127 runbook 新能力条目](../tickets/T127-runbook-effort6.md)
- [T128 release SBOM 附着](../tickets/T128-release-sbom.md)
- [T129 文档快速上手面](../tickets/T129-docs-polish.md)
- [T130 里程碑全仓 verify](../tickets/T130-milestone-verify.md)（blocked-by T112–T129）
- [T131 effort#7 收口](../tickets/T131-effort7-closing.md)（blocked-by T130）

</details>

## 收口记录（2026-08-15）

- **20 轮完整自迭代**（图表轮 + 18 实现轮 + 里程碑轮 + 收口轮；wayfinder 解票 → to-spec →
  to-tickets → implement → 验证 → commit）：索引防线（契约矩阵 T112 / DELETED 联动+fsck 索引源
  T113 / scanByPrefix T114）、观测与回归（压缩事件化 T115 / 黄金轨迹 A·B T116–117）、
  韧性（半开多探测 T118）、输入与供给（目录预算 T119 / MediaIntake T120）、可移植
  （导出扩展槽 T121）、查询面（dashboard 索引 T122）、工程面（配置元数据 T123 / 装配断言
  T124 / perf 哨兵 T125 / 演示 T126 / runbook T127 / SBOM 附着 T128 / 文档 T129）、
  里程碑 T130 与收口 T131。
- **里程碑终验**：全仓 `mvn clean verify` exit=0——18 模块全 SUCCESS；
  **1117 测试 0 失败 0 错误**（49 skipped = docker/真实 LLM 门控）。
- **文档面**：spec 新增 33–36 四篇 + 21/22/00/34 增补；api-surface effort#7 面；
  CONTEXT 术语两节；runbook 四处增补；RELEASING V3+SBOM；CONTRIBUTING 黄金集指引；
  docs/perf 增三哨兵基线。
- **累计口径**：effort #5（22 轮）+ #6（9 轮）+ #7（20 轮）= **51 轮完整流程自迭代**；
  票号 T1–T131、impl 切片 1–104 全局连续。
- **fog 毕业候选**（后续 effort）：skill_search 检索工具；LLM 响应缓存（语义边界）；
  dashboard 前端消费过滤列表；outbox 状态 store 前缀扫描的进一步下推（Redis 服务端 SCAN）；
  索引 CLOSED 行保留策略；memory 压缩梯子事件化（当前只发首轮）。
- **过程教训**：探测槽位语义需不变量表达（在飞+已成功≥阈值，非简单计数）；
  JSON 字符串包装使工具结果尺寸 ±引号（断言用关键标记）。
