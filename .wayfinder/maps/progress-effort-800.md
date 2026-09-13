# H 会话进度台账（rolling，收口轮据此归档）

> **状态：进行中（2026-09-13 开工）。** H 会话 800 系自迭代——
> /goal ≥50 轮 wayfinder→spec→tickets→implement 闭环，全自主决策，借鉴 GitHub >10K star 项目思想。
> 号段：efforts #800–#849 ｜ specs 800–849 ｜ 票 T1101–T1200 ｜ impl 553–602 ｜ 分支 h-session-800-series。
> 前情：D 会话 30 轮入 main（PR #18）｜ E 会话 50 轮入 main（PR #19）｜ G 会话 700 系双会话并存收口（PR #20，spec 700–749 双系列、票 T951–T1099 重叠并存、impl 503–552）。
> **号段声明先行**（G 会撞号教训制度化）：本文件即 H 会话对 800 系的占坑声明，先于 R1 提交并入 main；后续会话开 900 系前必须 fetch+双查 main。

每轮工件配方（C/D/E/G 会话约定延续）：
MAP（`maps/effort-<N>.md` + `.wayfinder/MAP.md` 表登记一行）→ spec
（`docs/spec/<N>-<slug>.md`）→ 2 张票（`tickets/T<NNNN>-*.md` shape+verify，
frontmatter Type/Status）→ 实现+测试 → README「生产级纵深 IX（H 会话 800 系
增量）」表加行（覆盖门：spec 文件名必须出现在 README）→ 新公共类型随轮
regenerate 快照（**必须带 `-Dbuzhou.api-snapshot.regenerate=true`**——700 系
教训：缺属性则静默跳过）+ api-surface.md H 段加行 → 提交尾
`(resolve TXXXX-TYYYY, implNNN, specNNN, H 会话第 N 轮=effort#NNN)`。

验证纪律（Windows 本机）：JDK21 inline + 串行 mvn（禁 -T）+ `-pl <mod> -am`
+ 排除集 `!ClasspathSkillScannerTest,!RunCommandToolTest,!RunCommandHardeningTest,!GuardAndHitlDemoTest,!TenantSandboxTest,!PropertyInvariantsTwoTest,!ErrorSignaturesTest,!UnsubscribedStreamTest,!TurnStallWatchdogTest`；
命令一律 `> /tmp/rN.log 2>&1; echo "MVN_EXIT=$?"` 后看日志。
已知 flaky 族（重跑即绿、与改动零交集即忽略）：webhook 族 / slow drip 流时序 /
WebhookOutboxLag / SpawnGatePriorityTest / SleepTimeConsolidationTest。

主题池（50 轮计划，**每轮勘察后可换——代码库 250+ effort 高度饱和，撞已有能力即换入备选池，换题注记入 map**；排重 grep 必须 -i 且按类名后缀查，每轮先 grep 后动工）：

R1 工具自动封禁(fail2ban ≈14K)→guard｜R2 Redis 大值审计(BIGKEY, Redis ≈68K)→store-redis｜
R3 指标新鲜度审计(Prometheus staleness ≈59K)→core｜R4 检索多路改写+RRF(LangChain MultiQueryRetriever ≈105K)→memory｜
R5 嵌入 L2 归一装饰器(sentence-transformers ≈17K)→resilience｜R6 路由分布倾斜读数(Spark skew ≈41K)→resilience｜
R7 预算分位推荐(k8s VPA ≈115K)→core｜R8 签名密钥到期审计(cert-manager ≈13K)→guard｜
R9 导出去重统计(restic ≈31K)→core｜R10 不可路由事件计数(RabbitMQ alternate-exchange ≈13K)【高危待勘 EventBusStats】→core｜
R11 存储提交延迟环形(etcd backend latency ≈50K)→core｜R12 断路器 crash-loop 检测(k8s CrashLoopBackOff)→resilience｜
R13 观测管道内存限流(OTel Collector memory_limiter)→observability｜R14 技能发布通道 dist-tag(pnpm ≈32K)→skills｜
R15 MCP 断路器变迁读数(Resilience4j events 扩散)→mcp｜R16 spill 写放大读数(RocksDB compaction stats ≈30K)→spill｜
R17 记忆分层容量读数(MemGPT/Letta ≈18K)→memory｜R18 错误预算燃烧率(Google SRE multiwindow)→core【高危待勘 ErrorBudget】｜
R19 限流自适应收紧(AWS adaptive retry, aws-cli ≈16K)→resilience｜R20 PII 置信分布读数(Presidio scores)【高危待勘 PiiDetector】→guard｜
R21 校验错误聚合(Pydantic ValidationError ≈25K)【高危待勘 ToolArgsValidator】→core｜R22 目录 lint 严重度分级(rust-clippy ≈13K)【高危待勘 ToolCatalogLinter】→core｜
R23 豁免登记面带过期(ESLint suppressions ≈26K)→guard｜R24 MCP 能力协商快照(LSP ≈11K)→mcp｜
R25 启动阶段耗时读数(spring-boot ApplicationStartup ≈78K)→core｜R26 取消原因分布读数(Temporal ≈14K)→core｜
R27 会话迁移对账(gh-ost ≈16K)→core｜R28 注入检测分级(ModSecurity paranoia)→guard｜
R29 定价表覆盖审计(LiteLLM model_prices ≈28K)【高危待勘 PricingTable】→core｜R30 连接池等待读数(HikariCP ≈20K 扩散:leak→等待)→store-jdbc【高危待勘】｜
R31 事实合并决策分布(mem0 扩散)【高危待勘 SummaryFactReconciler】→memory｜R32 死信重投成功率(sidekiq retry set 扩散)→core【高危待勘】｜
R33 审计树形健康读数(CT 扩散)→guard｜R34 会话准入拒绝分布(k8s admission 扩散)→core【高危待勘 SpawnGate】｜
R35 技能加载延迟读数(LangSmith analytics 扩散)→skills【高危待勘 SkillUsageStats】｜R36 危险工具命中分布(Cloudflare top-rules 思想)→tools【高危待勘】｜
R37 上下文截断策略读数(HF tokenizer truncation ≈150K)→core【高危待勘 ContextWindowResolver】｜R38 尾采样决策读数(OTel tail-sampling)→observability【高危待勘 ErrorSampler】｜
R39 半开探测成功率读数(Resilience4j 扩散)→resilience｜R40 选举竞争读数(Redisson RedLock ≈36K)→store-redis【高危待勘 LeaderElector】｜
R41 滚动导出统计读数(Vector.dev buffers ≈25K)→core【高危待勘 RollingJsonlWriter】｜R42 预算水位分档读数(k8s ResourceQuota)→core【高危待勘 TokenBudgetHook】｜
R43 脱敏命中矩阵读数(Presidio 扩散)→guard【高危待勘 PiiHitStats】｜R44 数据集近重复读数(Cleanlab ≈10K)→core【高危待勘 EvalDatasetStore】｜
R45 配置默认偏离读数(spring-boot config metadata 扩散)→starter【高危待勘 ConfigFingerprint】｜R46 MCP 连接遥测读数(gRPC channelz)→mcp【高危待勘 McpConnection】｜
R47 证据引用失效率读数(spill 扩散)→spill【高危待勘 EvidenceRefLedger】｜R48 摘要降级原因读数(Envoy degraded 扩散)→memory【高危待勘 SummaryDegrader】｜
R49 限流键基数与热点读数(Envoy per-conn RL 扩散)→resilience【高危待勘】｜R50 收口终验(全仓 verify+快照再生+台账核查+MAP 达成)。

备选池（换题时入替）：S1 事件慢订阅者读数(BufferedEventDispatcher 扩散)/S2 Hook 失败率读数(HookChain 扩散)/S3 双写偏差读数(MicrometerDualWriter 扩散)/S4 孤儿会话索引对账(SessionIndex×SessionStore)/S5 空闲时长分布(IdleSessionMonitor 扩散)/S6 Prompt 回滚使用读数(PromptVersion 扩散)/S7 TurnMemo 命中率读数/S8 延迟队列死信读数(DelayedJobQueue 扩散)/S9 泄漏疑似对象聚合(ResourceLeakDetector 扩散)/S10 价格表币种一致性(PricingTable 扩散)。

## 轮次台账（滚动回填）

| 轮 | effort | 主题 | 票 | impl | spec | 状态 |
|----|--------|------|----|------|------|------|
| R1 | #800 | 工具自动封禁 | T1101–1102 | 553 | 800 | ✅ 8/8 绿+快照 1 类 |
| R2 | #801 | Redis 大值审计 | T1103–1104 | 554 | 801 | ✅ 5/5 绿+快照 1 类 |
| R3 | #802 | 指标新鲜度审计 | T1105–1106 | 555 | 802 | ✅ 6/6 绿+封顶 bug 抓获修复 |
| R4 | #803 | 检索多路改写融合 | T1107–1108 | 556 | 803 | ✅ 9/9 绿+快照 1 类 |
| R5 | #804 | 嵌入 L2 归一化装饰器 | T1109–1110 | 557 | 804 | ✅ 7/7 绿+快照 1 类 |
| R6 | #805 | 路由分布倾斜读数 | T1111–1112 | 558 | 805 | ✅ 6/6 绿+破平教训 |
| R7 | #806 | 预算用量分位推荐 | T1113–1114 | 559 | 806 | ✅ 6/6 绿+快照 1 类 |
| R8 | #807 | 签名密钥轮换到期审计 | T1115–1116 | 560 | 807 | ✅ 6/6 绿+快照 1 类 |
| R9 | #808 | 导出内容去重统计 | T1117–1118 | 561 | 808 | ✅ 6/6 绿+快照 1 类 |
| R10 | #809 | 作业死信台账（换题 S8） | T1119–1120 | 562 | 809 | ✅ 5/5+回归 3/3 绿 |
| R11 | #810 | 存储提交延迟环形读数 | T1121–1122 | 563 | 810 | ✅ 7/7 绿+快照 2 类 |
| R12 | #811 | 断路器 crash-loop 检测 | T1123–1124 | 564 | 811 | ✅ 6/6 绿+快照 1 类 |
| R13 | #812 | 观测管道内存限流器 | T1125–1126 | 565 | 812 | ✅ 6/6 绿+并发守恒压测 |
| R14 | #813 | 技能发布通道解析 | T1127–1128 | 566 | 813 | ✅ 5/5 绿+semver 修正 |
| R15 | #814 | MCP 断路器变迁台账 | T1129–1130 | 567 | 814 | ✅ 4/4+回归 4/4 绿 |
| R16 | #815 | Spill 写放大读数 | T1131–1132 | 568 | 815 | ✅ 5/5 绿+窗口账修正 |
| R17 | #816 | 记忆分层容量读数 | T1133–1134 | 569 | 816 | ✅ 4/4 绿+快照 1 类 |
| R18 | #817 | SLO 多窗燃烧率判定 | T1135–1136 | 570 | 817 | ✅ 6/6 绿+快照 1 类 |
| R19 | #818 | 限流自适应收紧器 | T1137–1138 | 571 | 818 | ✅ 6/6 绿+快照 1 类 |
| R20 | #819 | Token 估算校准审计（换题） | T1139–1140 | 572 | 819 | ✅ 5/5 绿+快照 1 类 |
| R21 | #820 | 护栏豁免登记面（换题） | T1141–1142 | 573 | 820 | ✅ 5/5 绿+快照 1 类 |
| R22 | #821 | 目录 lint 严重度分级 | T1143–1144 | 574 | 821 | ✅ 5/5 绿+快照 1 类 |
| R23 | #822 | MCP 能力协商快照 | T1145–1146 | 575 | 822 | ✅ 5/5 绿+快照 1 类 |
| R24 | #823 | 启动阶段耗时读数 | T1147–1148 | 576 | 823 | ✅ 5/5 绿+快照 1 类 |
| R25 | #824 | 取消原因分布读数 | T1149–1150 | 577 | 824 | ✅ 3/3 绿+快照 1 类 |
| R26 | #825 | 会话迁移对账 | T1151–1152 | 578 | 825 | ✅ 6/6 绿+快照 1 类 |
| R27 | #826 | 注入检测分级策略 | T1153–1154 | 579 | 826 | ✅ 6/6 绿+快照 1 类 |
| R28 | #827 | 定价表覆盖审计 | T1155–1156 | 580 | 827 | ✅ 5/5 绿+快照 1 类 |
| R29 | #828 | 连接池获取计时装饰器 | T1157–1158 | 581 | 828 | ✅ 4/4 绿+快照 1 类 |
| R30 | #829 | 事实合并决策分布 | T1159–1160 | 582 | 829 | ✅ 3/3 绿+快照 1 类 |
| R31 | #830 | 审计树形健康读数 | T1161–1162 | 583 | 830 | ✅ 3/3 绿+快照 1 类 |
| R32 | #831 | 会话准入拒绝分布 | T1163–1164 | 584 | 831 | ✅ 3/3 绿+快照 1 类 |
| R33 | #832 | 技能加载延迟读数 | T1165–1166 | 585 | 832 | ✅ 5/5 绿+快照 1 类 |
| R34 | #833 | 危险工具命中分布 | T1167–1168 | 586 | 833 | ✅ 3/3 绿+快照 1 类 |
| R35 | #834 | 上下文截断统计 | T1169–1170 | 587 | 834 | ✅ 3/3 绿+快照 1 类 |
| R36 | #835 | 尾采样决策台账 | T1171–1172 | 588 | 835 | ✅ 4/4 绿+快照 1 类 |
| R37 | #836 | 半开探测成功率读数 | T1173–1174 | 589 | 836 | ✅ 4/4 绿+快照 1 类 |
| R38 | #838 | 选举竞争读数 | T1175–1176 | 590 | 838 | ✅ 3/3 绿+快照 1 类 |
| R39 | #837 | 限流键热点读数（补位轮） | T1177–1178 | 591 | 837 | ✅ 3/3 绿+effort 连续性恢复 |
| R40 | #839 | 泄漏疑似对象聚合器（换题 S9） | T1179–1180 | 592 | 839 | ✅ 4/4 绿+快照 1 类 |
| R41 | #840 | MCP 建连遥测读数 | T1181–1182 | 593 | 840 | ✅ 4/4 绿+快照 1 类 |
| R42 | #841 | 会话空闲时长分桶直方 | T1183–1184 | 594 | 841 | ✅ 4/4 绿+快照 1 类 |
| R43 | #842 | HITL 认证决策分布 | T1185–1186 | 595 | 842 | ✅ 3/3 绿+快照 1 类 |
| R44 | #843 | 证据引用失效率读数 | T1187–1188 | 596 | 843 | ✅ 3/3 绿+快照 1 类 |
