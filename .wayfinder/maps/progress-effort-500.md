# E 会话进度台账（rolling，收口轮据此归档）

> **状态：0/50 进行中（2026-09-11 起）。** E 会话 500 系自迭代——
> /goal ≥50 轮 wayfinder→spec→tickets→implement 闭环，全自主决策。
> 号段：efforts #500–#549 ｜ specs 500–549 ｜ 票 T751–T850 ｜ impl403–452。
> 前情：D 会话 30 轮已收官入 main（PR #18，merge 4c0ed7a，见
> [progress-effort-400.md](progress-effort-400.md)）。
> 600 系号段留给后续会话（550–599 未占用）。

每轮工件配方（C/D 会话约定延续）：
MAP（`maps/effort-<N>.md` + 总索引 MAP.md 登记一行）→ spec
（`docs/spec/<N>-<slug>.md`，B 会话格式）→ 2 张票
（`tickets/TXXX-*.md`，frontmatter Type/Status）→ 实现+测试 →
README「生产级纵深 VI（E 会话 500 系增量）」表加行（覆盖门：spec↔README
双向一致）→ 新公共类型随轮 regenerate 快照 + api-surface.md 加行 →
提交尾 `(resolve TXXX-TYYY, implNNN, specNNN, E 会话第 N 轮=effort#NNN)`。

验证纪律（Windows 本机）：JDK21 inline + 串行 mvn（禁 -T）+ `-pl <mod> -am`
+ 排除集 `!ClasspathSkillScannerTest,!RunCommandToolTest,!RunCommandHardeningTest,!GuardAndHitlDemoTest,!TenantSandboxTest,!PropertyInvariantsTwoTest,!ErrorSignaturesTest,!UnsubscribedStreamTest,!TurnStallWatchdogTest`；
命令一律 `> /tmp/rN.log 2>&1; echo "MVN_EXIT=$?"` 后看日志。
快照：`mvn -pl buzhou-spring-boot-starter -am test -Dtest='ApiSurfaceSnapshotTest#regenerateSnapshot' -Dsurefire.failIfNoSpecifiedTests=false`（比对测试须 Skipped: 0 且绿）。
已知 flaky 族（重跑即绿、与改动零交集即忽略）：webhook 族 / slow drip 流时序 /
WebhookOutboxLag 顺序型。

主题池（50 轮计划，每轮勘察后可调——撞已有能力即换，换题注记入 map）：
流式输出 PII 脱敏(Presidio streaming)/提示词注入特征扫描(OWASP LLM01·NeMo Guardrails)/
请求幂等键(Stripe Idempotency-Key)/模型能力注册表路由(LiteLLM router)/
语义路由(aurelio-labs semantic-router)/时段路由窗口(K8s CronJob)/
模型蓝绿发布自动回滚(Argo Rollouts)/在线实验分桶(GrowthBook·Statsig)/
MCP 服务器级聚合熔断(resilience4j)/MCP 工具名空间前缀(420 lint 扩散)/
HTTP 工具出口域名白名单(OWASP SSRF·envoy egress)/工具入参限幅(nginx client_max_body_size)/
输入体积限幅(nginx)/可逆 PII 代管库(Presidio vault)/成本异常尖峰检测(Prometheus z-score)/
时延 SLO 燃尽(Google SRE 多窗——321 扩散)/缓存 token 计费口径(OpenAI cached_tokens·vLLM)/
租户级价目档案(Stripe metering)/模型访问策略 allowlist(K8s RBAC 扩散)/
配置刷新原子门(Netflix Archaius·K8s rollout)/spill 制品透明压缩(S3 gzip)/
实体记忆登记簿(mem0·Zep entity memory)/技能依赖传递加载(npm·Maven 依赖解析)/
宿主测试工具箱(Testcontainers 思想)/评估集合成扩增(Ragas testset generation)/
评估 A/A 抖动检测(HELM repeat)/judge 校准跟踪(LightEval)/工具调用图谱统计(LangSmith analytics)/
事故复盘一键包(PagerDuty postmortem——317 扩散)/金丝雀词泄漏检测(thinkst canarytokens)/
内容安全词表过滤(OpenAI moderation)/提示词模板变量严格校验(Jinja2 StrictUndefined——401 扩散)/
会话导出加密(333 通道扩散)/归档冷存完整性校验(S3 checksum)/outbox 投递时延分位数(416 同法)/
per-tool 超时预算 yml(308 deadline 扩散)/失败轮快照面(Sentry event payload)/
会话标签检索(Grafana label)/重试原因分类统计(Finagle)/水位翻转告警接入(181×312 扩散)/
熔断状态事件外发(webhook 族扩散)/配置 env 占位缺失 fail-fast(doctor 扩散)/
会话生命周期事件补齐/技能金丝雀(324 扩散)/评估 run 预算闸(budget 族扩散)/
记忆压缩率分布观测/会话导出脱敏选项(28×86 扩散)/MCP 重连退避策略(机动)/
机动轮（按勘察旁注选定）/收口终验+台账归档。

已否决池（D 会话累积，勿再做）：模型回退链、响应/语义缓存跨实例、事件 DLQ/去重、
基数护栏、共享熔断/限流 Redis、Retry-After 遵从、依赖图并行、会话快照 DR、审计 WORM、
死信偏移重放、Webhook 事件幂等键（X-Buzhou-Event-Id 已带）。

| 轮 | effort | spec | 票 | impl | 主题 | 提交 |

## 备忘
- R14/R19 两次 ToolCallCoalescerTest 全量下时序假红（differentKeysExecuteIndependently/
  failurePropagatesToAllWaiters——异步完成时序断言），单跑即绿、与改动零交集；
  持续观察，复现升排除集。webhook 族假红 R8 再现一次（同 D 会话 R6 记录）。
|----|--------|------|----|------|------|------|
| 1 | #500 | 500 | T751-752 | 403 | 流式回复 PII 脱敏（Presidio streaming+流式 WAF 回看窗） | 2210b80 |
| 2 | #501 | 501 | T753-754 | 404 | 请求幂等键（Stripe；勘察换题：原拟注入扫描已有 InjectionClassifier/OnnxPromptGuard） | 5c819ea |
| 3 | #502 | 502 | T755-756 | 405 | 模型能力注册表与能力门（LiteLLM Router capabilities） | 920fcf6 |
| 4 | #503 | 503 | T757-758 | 406 | 时段路由窗口（K8s CronJob/Argo Rollouts schedule） | d87793f |
| 5 | #504 | 504 | T759-760 | 407 | MCP 服务器级聚合熔断（Envoy per-host；core ToolCircuitBreaker 键=server 复用） | 496f6c0 |
| 6 | #505 | 505 | T761-762 | 408 | 在线实验分桶（GrowthBook/Statsig） | e231d90 |
| 7 | #506 | 506 | T763-764 | 409 | 工具入参限幅（nginx client_max_body_size；31 入站对称面） | 92c1d12 |
| 8 | #507 | 507 | T765-766 | 410 | 可逆 PII 代管库（Presidio Vault；webhook 族假红重跑绿记录） | 07dc9d0 |
| 9 | #508 | 508 | T767-768 | 411 | 成本异常尖峰检测（Prometheus/Istio z-score；403 互补） | 8031694 |
| 10 | #509 | 509 | T769-770 | 412 | 时延 SLO 燃尽（Google SRE——321 扩散） | 477f0c7 |
| 11 | #510 | 510 | T771-772 | 413 | 会话导出加密（age/OCI 加密 artifact；333 通道扩散） | c697b8c |
| 12 | #511 | 511 | T773-774 | 414 | 归档冷存完整性校验（S3 checksum；同域子前缀污染清单→改独立命名空间） | 3e3fad6 |
| 13 | #512 | 512 | T775-776 | 415 | 提示词模板严格渲染（Jinja2 StrictUndefined；401 扩散） | 8675a39（spec512 行补 a0e1993） |
| 14 | #513 | 513 | T777-778 | 416 | 评估 A/A 抖动检测（HELM/工业 A/A test） | 48b720e |
| 15 | #514 | 514 | T779-780 | 417 | 投递时延分位数（416 分位族同法；135 互补） | ca127e0 |
| 16 | #515 | 515 | T781-782 | 418 | 内容安全词表过滤（OpenAI moderation 规则子集） | 293ce2e |
| 17 | #516 | 516 | T783-784 | 419 | judge 校准跟踪（LightEval；混淆矩阵四率） | df13de8 |
| 18 | #517 | 517 | T785-786 | 420 | 记忆压缩率分布观测（204 分布化；416 同法） | fa9ae86 |
| 19 | #518 | 518 | T787-788 | 421 | 会话导出脱敏（Presidio anonymize；28×86 组合） | 0796cdc |
| 20 | #519 | 519 | T789-790 | 422 | 工具调用图谱统计（LangSmith trace analytics） | b1aac28 |
| 21 | #520 | 520 | T791-792 | 423 | 评估 run 预算闸（AWS Budgets/pytest maxfail 早停） | 68b7fb9 |
| 22 | #521 | 521 | T793-794 | 424 | 事故复盘一键包（317 ExportBundle 事故域组合） | （R22 提交后回填） |
