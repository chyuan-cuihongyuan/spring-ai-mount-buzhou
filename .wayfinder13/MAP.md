# Wayfinder Map — Buzhou 配置与公共面治理（effort #13）

> effort #13，延续 #5–#12（#12 收口中；累计 134 轮 / T1–T213 / impl 1–177）。
> 本 effort 主线：**配置与公共面治理**——T187 暴露的「yml 键静默不生效」与 api-surface
> 人工维护漂移两类结构性风险，转化为系统性自动化防线（绑定完整性矩阵 + 公共面快照）。
> 到达 = 8 轮自迭代落地、全仓 verify 绿、对抗与文档齐备、MAP 闭合。

## Destination

每个 metadata 键都有真实装配路径的绑定断言（防静默失效）；公共 API 面变更未入档即
测试失败（防意外漂移）；治理流程（快照更新/键新增检查单）入 runbook。

## Notes

- 领域/测试哲学/10K★ 政策/AFK 授权：沿用 effort #6–#12 MAP Notes。
- 外部研究：治理类主题无直接 ≥10K★ 同型对标——参考 Caffeine（~17K★）与 Spring Boot 自身
  的 metadata 处理器思路：**配置元数据即契约，契约须可验证**（本地裁定量身防线，不强引）。
- 本地勘察（2026-08-16）：T187 缺陷（Fallback/Circuit 缺 @ConstructorBinding 静默失效）
  证明「record 归一 + 单点绑定测试」不足以防线——绑定测试按键逐一存在与否决定，多构造器
  record 组合键从未被测；api-surface.md 由 effort#5 起人工维护（impl-75 自动生成+人工整理），
  effort#11 新增 core.eval 包 8 类型已入档但无自动校验，漂移风险随面增长线性上升。
- 过程教训沿用：多构造器嵌套 record 显式 @ConstructorBinding；下游单跑 -am。

## Decisions so far

- [T214 配置绑定完整性矩阵](tickets/T214-config-bindings-matrix.md) — 93 键全矩阵防线；**当场抓获修复 4 存量缺陷**（2 Duration parse 炸 + 3 键名静默失效改名入档）。
- [T215 公共面快照](tickets/T215-apisurface-snapshot.md) — 449 类型黄金快照 + 比对 + regenerate；reactor 形态门。
- [T216 配置文档对账](tickets/T216-config-docs-matrix.md) — runbook/README 键名同步收尾；metadata 事实源口径。
- [T217 治理面对抗](tickets/T217-governance-redteam.md) — 漂移演练删行红/恢复绿；更新流程入档。
- [T218 键拼写演示](tickets/T218-governance-demo.md) — 三道防线钉住（演示票面修订为流程入档）。
- [T219 runbook 增补](tickets/T219-runbook-9.md) — 配置治理节（矩阵/快照/拼写/新键检查单）。
- [T220 CONTEXT/api-surface 增补](tickets/T220-context-api-13.md) — 术语节 3 条 + 修复性改名/行为修复入档。

## Not yet specified

- 分布式限流/多实例共享配额（effort#14 主题候选——Redis SPI 模式勘察已完成）。
- skill 语义排序 / outbox Redis SCAN 下推 / 观测 OLAP / store 静态加密 / 语义缓存（沿用 fog）。

## Out of scope

- 沿用 effort #7–#12 Out of scope 全部条目。
- CI 门禁流水线改造（防线落在测试层，接入 CI 是宿主侧职责）。
- 全量 yml 示例文件生成（metadata IDE 提示已覆盖主要场景）。

## Tickets

初始 8 张（T214–T221，均含 AFK 决议，按轮逐张闭合）：

- [T214 配置绑定完整性矩阵](tickets/T214-config-bindings-matrix.md)
- [T215 公共面快照测试](tickets/T215-apisurface-snapshot.md)
- [T216 配置文档对账](tickets/T216-config-docs-matrix.md)（blocked-by T214）
- [T217 治理面对抗](tickets/T217-governance-redteam.md)（blocked-by T214/T215）
- [T218 键拼写演示](tickets/T218-governance-demo.md)（blocked-by T215）
- [T219 runbook 增补](tickets/T219-runbook-9.md)（blocked-by T216/T217）
- [T220 CONTEXT/api-surface 增补](tickets/T220-context-api-13.md)（blocked-by T214/T215）
- [T221 里程碑 verify + 收口](tickets/T221-effort13-closing.md)（blocked-by T217–T220）
