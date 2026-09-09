# D 会话进度台账（rolling，收口轮据此归档）

> **状态：0/30（2026-09-08 开工）。** D 会话 400 系自迭代——
> /goal ≥30 轮 wayfinder→spec→tickets→implement 闭环，全自主决策。
> 号段：efforts #400–#429 ｜ specs 400–429 ｜ 票 T691–T750 ｜ impl373–402。
> 前情：C 会话 50 轮已收官入 main（PR #17，见
> [progress-effort-300.md](progress-effort-300.md)）。

每轮工件配方（C 会话约定延续）：
MAP（`maps/effort-<N>.md` + 总索引 MAP.md 登记）→ spec
（`docs/spec/<N>-<slug>.md`，B 会话格式）→ 2 张票
（`tickets/TXXX-*.md`，frontmatter Type/Status）→ 实现+测试 →
README「生产级纵深 V」表加行（覆盖门 213：spec↔README 双向一致）→
新公共类型随轮 regenerate 快照 → 提交尾
`(resolve TXXX-TYYY, implNNN, specNNN, D 会话第 N 轮=effort#NNN)`。

验证纪律（Windows 本机）：JDK21 inline + 串行 mvn（禁 -T）+ `-pl <mod> -am`
+ 排除集 `!ClasspathSkillScannerTest,!RunCommandToolTest,!RunCommandHardeningTest,!GuardAndHitlDemoTest,!TenantSandboxTest,!PropertyInvariantsTwoTest,!ErrorSignaturesTest,!UnsubscribedStreamTest,!TurnStallWatchdogTest`；
命令一律 `> /tmp/rN.log 2>&1; echo "MVN_EXIT=$?"` 后看日志。

主题池（30 轮计划，每轮勘察后可调——撞已有能力即换）：
模型回退链(LiteLLM)/响应缓存共享(GPTCache)/事件死信(SQS DLQ)/租户限速(nginx)/
输出schema执法(instructor)/密钥扫描(gitleaks)/成本预测(AWS Budgets)/基数护栏(Prometheus)/
共享熔断Redis(fog遗留)/审计Merkle(CT log)/配置diff(ArgoCD)/提示词注册表(Langfuse)/
OLAP预聚合(M3)/延迟作业(Sidekiq)/Retry-After(HTTP 429)/退役通告(K8s API)/
依赖图并行(LangGraph)/健康时间线(PagerDuty)/会话快照DR(etcd)/黏性路由(Ketama)/
预算日历重置/在线采样评测(Honeycomb)/结果schema校验(MCP)/共享事实ACL(mem0)/
泳道优先级/审计WORM(S3 Object Lock)/事件偏移重放(Kafka seek)/Webhook幂等(Stripe)/
租约抢占/供应商并发配额。

| 轮 | effort | spec | 票 | impl | 主题 | 提交 |
|----|--------|------|----|------|------|------|
| 1 | #400 | 400 | T691-692 | 373 | 密钥扫描护栏（gitleaks；勘察换题：原拟回退链已存在） | 51378ee |
| 2 | #401 | 401 | T693-694 | 374 | 提示词注册表（Langfuse） | d5fb233 |
| 3 | #402 | 402 | T695-696 | 375 | 结构化输出执法（instructor） | 4225ba5 |
| 4 | #403 | 403 | T697-698 | 376 | 成本预测外推（AWS Budgets forecast） | fa1aa6d |
| 5 | #404 | 404 | T699-700 | 377 | 审计 Merkle 根+包含证明（CT log） | efec052 |
| 6 | #405 | 405 | T701-702 | 378 | 健康事件时间线（PagerDuty；勘察换题：Retry-After 已存在） | e30c239 |
| 7 | #406 | 406 | T703-704 | 379 | 工具退役通告（K8s API deprecation） | 690c893 |
| 8 | #407 | 407 | T705-706 | 380 | 在线采样入评测集（Honeycomb；勘察换题×2：并行管理器与快照 DR 均已存在） | 8e8017a |
| 9 | #408 | 408 | T707-708 | 381 | 预算日历周期（AWS Budgets calendar） | 604268f |
| 10 | #409 | 409 | T709-710 | 382 | 工具结果 schema 校验（MCP outputSchema） | 05a81da |
| 11 | #410 | 410 | T711-712 | 383 | 共享事实库 ACL（mem0） | f0f5c6c |
| 12 | #411 | 411 | T713-714 | 384 | 泳道优先级原语（Envoy priority levels；审计 WORM 勘察弃——append-only 已是设计） | a663179 |
| 13 | #412 | 412 | T715-716 | 385 | 时间桶预聚合（M3；勘察换题：死信重放 replayDeadLetters 已有弃） | dd37a1f |
| 14 | #413 | 413 | T717-718 | 386 | 延迟作业原语（Sidekiq delayed_jobs） | c3a391c |
| 15 | #414 | 414 | T719-720 | 387 | 配置漂移审计（ArgoCD drift detection） | 27782e6 |
| 16 | #415 | 415 | T721-722 | 388 | 会话黏性路由提示（Ketama 确定性键） | 0302ea1 |
| 17 | #416 | 416 | T723-724 | 389 | 时间桶延迟分位数（#412 扩散，Prometheus histogram_quantile） | 41018fe |
| 18 | #417 | 417 | T725-726 | 390 | 价目热更新（320/340 rebind 同模式+Stripe） | 5de1b3b |
| 19 | #418 | 418 | T727-728 | 391 | 秘密命中统计与导出（#400 扩散，PiiHitStats 镜像） | dff30e8 |
| 20 | #419 | 419 | T729-730 | 392 | 周期预算健康面（#408 扩散，AWS Budgets 面板） | d15d998 |
| 21 | #420 | 420 | T731-732 | 393 | 工具目录 lint（ESLint 构建期） | 1e96830 |
| 22 | #421 | 421 | T733-734 | 394 | 审计封印导出（#404 扩散，CT STH 公示） | e7003cf |
| 23 | #422 | 422 | T735-736 | 395 | 工具泳道优先级装配（#411 扩散，Envoy 接线） | |


## 备忘
- R7 又见 StreamTerminationMetricsTest.slowDripStreamCutByCumulativeCap 一次
  时序假红（重跑即绿；与改动零交集——slow drip 类时序同族）。
- R6 又见 webhook 族偶发假红（WebhookEventForwarderTest.deliversSignedJsonEnvelope
  + WebhookFanoutTest.noFanoutKeeps...，重跑同命令即绿；与改动零交集）——webhook
  族假红与 R3 WebhookOutboxLag 同族，持续观察。
- R3 观察到 WebhookOutboxLagTest.backedOffRecordStillCountsTowardAge 顺序型
  假红一次（NoSuchElement @ getFirst，重跑同命令即绿；与当轮改动零交集）——
  持续观察，复现再入排除集。
- R3 关键机制发现：Spring AI advisor 链为**单遍弹出式 Deque**——重入
  nextCall 必炸「No CallAdvisors available」；内层重试/修复必须直达
  modelTerminal（getCallAdvisors() 链尾，ResilienceAdvisor.modelTerminal
  同法）。后续凡做「模型重调」类 advisor 均循此约定。
