# G 会话进度台账（rolling，收口轮据此归档）

> **状态：0/50（2026-09-12 开工）。** G 会话 700 系自迭代——
> /goal ≥50 轮 wayfinder→spec→tickets→implement 闭环，全自主决策，借鉴 GitHub >10K star 项目思想。
> 号段：efforts #700–#749 ｜ specs 700–749 ｜ 票 T1000–T1099 ｜ impl600–649 ｜ 分支 g-session-700-series。
> 前情：E 会话 50 轮入 main（PR #19，merge 8741dbc，见 [progress-effort-500.md](progress-effort-500.md)）；
> F 会话 600 系并行中（至 R40+，spec 639、票 T929），700 系已避开。
> 800 系号段留给后续会话（750–799 未占用）。

每轮工件配方（C/D/E 会话约定延续）：
MAP（`maps/effort-<N>.md` + `.wayfinder/MAP.md` 表登记一行）→ spec
（`docs/spec/<N>-<slug>.md`，B 会话格式）→ 2 张票
（`tickets/T<1000+>-*.md`，frontmatter Type/Status）→ 实现+测试 →
README「生产级纵深 VIII（G 会话 700 系增量）」表加行（覆盖门：spec 文件名
必须出现在 README，死链也红）→ 新公共类型随轮 regenerate 快照
（`mvn -pl buzhou-spring-boot-starter -am test -Dtest='ApiSurfaceSnapshotTest#regenerateSnapshot'
-Dsurefire.failIfNoSpecifiedTests=false`）+ api-surface.md G 段加行 →
提交尾 `(resolve TXXXX-TYYYY, implNNN, specNNN, G 会话第 N 轮=effort#NNN)`。

验证纪律（Windows 本机）：JDK21 inline + 串行 mvn（禁 -T）+ `-pl <mod> -am`
+ 排除集 `!ClasspathSkillScannerTest,!RunCommandToolTest,!RunCommandHardeningTest,!GuardAndHitlDemoTest,!TenantSandboxTest,!PropertyInvariantsTwoTest,!ErrorSignaturesTest,!UnsubscribedStreamTest,!TurnStallWatchdogTest`；
命令一律 `> /tmp/rN.log 2>&1; echo "MVN_EXIT=$?"` 后看日志。
已知 flaky 族（重跑即绿、与改动零交集即忽略）：webhook 族 / slow drip 流时序 /
WebhookOutboxLag 顺序型。

主题池（50 轮计划，**每轮勘察后可换——代码库 250+ effort 高度饱和，撞已有能力即换入备选池，换题注记入 map**）：
R1 能力门决策审计读数(OPA decision logs)/R2 语义缓存权重预算驱逐(Caffeine weigher)/
R3 断路器状态变迁事件流(Resilience4j events)/R4 健康加权路由抑制(HAProxy agent-check)/
R5 指标标签基数审计(Grafana Loki label budget)/R6 流式背压水位读数(Netty watermark)【WatermarkHealth 已存——高危待勘】/
R7 MCP 目录差异审计(ArgoCD drift plan)/R8 会话操作 reflog(Git reflog)/
R9 spill 压实读数(Git gc)/R10 评估项结果记忆化(scikit-learn Pipeline cache)/
R11 实验到期自动停(GrowthBook expiry)/R12 全局 holdout 层(Statsig holdouts)/
R13 成本级回落链(LiteLLM fallbacks)/R14 金丝雀词泄漏检测(thinkst)【CanaryToolCallback 已存——辨义待勘】/
R15 死信 JSONL 导出(sidekiq dead set)/R16 webhook 签名双密钥轮换(Google Tink rotation)/
R17 span 状态分布读数(OTel span status)/R18 数据集标签过滤(Langfuse dataset tags)/
R19 judge 分档评分扩展(HELM grading)/R20 评估集噪声注入(Ragas perturbation)/
R21 工具调用 TTL 丢弃(Celery expires)/R22 巡检错过补偿(Airflow catchup)/
R23 导出归档保留分层(MinIO ILM)【retention 包已存——辨义待勘】/R24 prompt 一键回滚(Langfuse rollback)/
R25 导出 schema 版本协商(Protobuf versioning)/R26 配置变更 diff 读数(待勘——ConfigDriftAuditor 已存)/
R27 脱敏分类计数读数(OpenAI moderation categories)/R28 错误指纹聚类读数(Sentry grouping)/
R29 fork 树谱系读数(602 扩散)/R30 选举租约观测读数(etcd lease)/
R31 工具超时预算分配(gRPC deadline propagation)/R32 PII 格式保形掩码(Presidio FPE)/
R33 技能废弃标记(npm deprecate)/R34 技能加载频次读数(LangSmith analytics)/
R35 记忆事实冲突检测(mem0 conflicts)/R36 事实访问时间戳读数(Zep temporal)/
R37 评估分数漂移基线(Evidently drift)/R38 工具契约样本门(Pact contract)/
R39 工具结果缓存 TTL 分层(待勘)/R40 限流头前瞻读数(OpenAI x-ratelimit-remaining)/
R41 事件保留水位读数(Kafka retention)/R42 MCP 并发占用读数(610 扩散)/
R43 会话导出分片(S3 multipart)/R44 判定理由引用抽取(待勘)/
R45 供应商限流头适配(40 同族)/R46 导出分片聚合读数(43 同族——二者择一)/
R47 反应堆自检聚合读数(actuator aggregate)【待勘】/R48 审计链快照导出(AuditChain 扩散)【待勘】/
R49 慢调用 TopN 读数(MySQL slow query log)/R50 收口终验（全反应堆串行回归+快照/覆盖门+台账归档）。

备选池（撞车即换入）：多租户配额继承树/嵌入批量合并/提示词分段预算读数/导出脱敏自定义识别器/
会话元数据验证门/事件计数读数/工具取消传播/会话状态压缩率读数/慢流式首字节读数/导出增量导出。

## 轮次记录（每轮提交后回填哈希）

| 轮 | effort | 主题 | spec | 票 | impl | 提交 |
|----|--------|------|------|----|------|------|
| 1 | #700 | 能力门决策审计读数（OPA Decision Logs） | 700 | T1000–1001 | 600 | （本轮） |
