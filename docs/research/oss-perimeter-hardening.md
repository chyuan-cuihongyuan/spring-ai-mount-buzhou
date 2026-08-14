# OSS Perimeter Hardening — 外围收口外部核验（effort #4 / T69）

> 2026-08-15。10K★ 政策：采纳事实源只认 ≥10K★ OSS；不达标者注记。
> 本地事实源：2026-08-15 三路本地勘察（observability/otel/dashboard、mcp/skills/tools、resilience/starter/BOM/examples/redteam/CI/文档）。

## §0 摘要与建议采纳表

| 线 | 事实源（★） | 采纳结论 |
| --- | --- | --- |
| 管理端安全 | Spring Boot Actuator（75K+） | dashboard 默认绑 127.0.0.1、默认关、opt-in bearer token、写端点必须带鉴权、500 不回显内部异常 |
| 观测生命周期 | Spring Boot Actuator/Micrometer 体系（75K+；micrometer 本体 ~5K 注记借道 Boot 惯例） | 管线成为 bean + destroyMethod=close；指标经 MeterBinder 预注册单一口径；吞异常点必须 WARN |
| MCP 工具风险 | LangChain（100K+）、MCP elicitation 规范 | 不信任 server 自报元数据，按客户端自己的风险分类登记危险工具名 |
| 工具沙箱 | OpenHands（55K+） | 超时/取消杀进程树并告知 agent；环境变量最小化（白名单）；输出有界 |
| 配置元数据 | Spring Boot 官方 processor 文档 | annotationProcessorPaths + optional；additional-metadata 只在被 processor 合并时生效（本仓现状=无效，必须补 processor） |
| redteam 真实性 | promptfoo 官方 guardrails 指南（promptfoo ~5K★，注记：事实源为官方文档而非项目 star 数） | target 返回 `{output, guardrails:{flagged}}` 契约；HTTP target 用 `transformResponse` 从响应头派生 flagged；`type: guardrails` 断言；CI 按 eval 退出码门禁 |

## §1 管理端点/调试 UI 安全

**事实源**：Spring Boot（~75K★）Actuator 官方文档与误配置事故记录（Wiz/Acunetix 对 `exposure.include=*` 的漏洞披露）。

要点：
- 默认只暴露 `/health`（3.x 起 `/info` 也收回到仅 health），never `include=*`。
- 敏感端点（env/heapdump/threaddump）暴露=漏洞类别；管理流量走独立 management port + 内网隔离。
- 认证走 Spring Security 角色；无认证时唯一安全默认=不暴露。

对本仓适配：dashboard 现状（绑 0.0.0.0 + 零鉴权 + Skill 写端点）属于上述漏洞类别的等价物。采纳：
1. `bind-address` 默认 `127.0.0.1`（对齐"管理面默认仅本机"）。
2. `auth-token` 可配：设置后全部 API（尤其写端点）要求 `Authorization: Bearer`；未设置且绑定非 loopback 时启动 WARN（fail-open 但高声）；绑定非 loopback 且无 token 时**拒绝启动**（对齐 fail-fast 文化）。收紧：绑定非 loopback 必须设置 token，否则抛 BuzhouConfigurationException。
3. 500 响应只回 `{"error":"internal"}`+日志留痕，不回显异常 message。

## §2 观测管线生命周期与背压

**事实源**：Spring Boot Actuator/MeterRegistry 惯例（75K+；micrometer 本体 ~5K★ 不达标，注记——但其作为 Spring Boot 官方观测栈组成部分，惯例经 Boot 事实源采纳）。

要点：MeterRegistry 均 AutoCloseable 且随 context 关闭排空；指标经 MeterBinder 在 refresh 期预注册（而非首次记录才出现）；后台发布线程有界且可关。

对本仓：AsyncObservabilityPipeline 必须 bean 化（destroyMethod=close）；MicrometerDualWriter 与 core BuzhouMetricsBinder 收敛为单一预注册家族（删平行家族，保留 outcome tag 语义并入 core 家族）；safeStore/dispatchToSinks 吞异常点补 WARN 日志。otel 桥 openSpans/sessionTrace 有界化（上限+驱逐计数），三处静默 catch 日志化。

## §3 MCP client 运维

**事实源**：LangChain（~100K★）HITL/工具风险实践 + MCP elicitation 规范（modelcontextprotocol 组织；官方 SDK 生态 star 数不足 10K，注记——elicitation 为规范事实而非 star 事实）。

要点：**不要信任工具自报的 read-only/safe 元数据**（恶意 server 可把删除工具标成只读）；审批策略基于**客户端自己的风险分类**（按工具名/动词模式），MCP elicitation 提供协议内审批通道。

对本仓：MCP 注册表提供 `dangerousToolNamePatterns`（客户端侧配置，默认 `*.delete*`、`*.drop*`、`*.write*`、`*.send*` 类动词模式），命中者经 `McpToolDangerRegistry` 暴露给装配侧挂 guard HITL——与 tools 模块的 `enabledDangerousToolNames()` 同一挂点。工具快照重发现本轮不做（注记开放问题）。

## §4 工具沙箱强化

**事实源**：OpenHands（~55K★，issue #207/#1637 + 沙箱文档）。

要点：命令必须在沙箱内可被超时终止（#207：前台命令不超时=挂死）；超时终止后**必须告知 agent**（#1637：静默超时让 agent 迷航）；隔离运行时（docker）为主流，进程型沙箱自认 unsafe；env 面最小化。

对本仓采纳（run_command）：
1. 取消/中断路径与超时路径同一 `killProcessTree` 收口（修孤儿进程泄漏）。
2. 环境变量白名单透传（默认最小集 PATH/HOME/LANG/TZ + 显式 allowlist），其余丢弃——进程不再继承父进程全部机密。
3. read_file/http_request 读入阶段上限（超限返回错误文本，不 OOM）。
4. 超时/取消的失败文本保留"告知 agent"语义（已有，验证之）。

guard CommandSandbox 完整接线：本轮不做（run_command 黑名单+kill+env 白名单+限额先行），注记开放问题。

## §5 配置元数据工程

**事实源**：Spring Boot 官方（annotation processor 文档 + GitHub issue #34210 合并语义）。

要点：`spring-boot-configuration-processor` 经 `annotationProcessorPaths` 声明、optional、不漏给消费方；`additional-spring-configuration-metadata.json` **只在编译期被 processor 合并进 `spring-configuration-metadata.json` 才生效**——没有 processor 时 additional 文件是死文件（正是本仓 impl/43 的现状）。

对本仓：根 pom pluginManagement 配 compiler `annotationProcessorPaths`，凡有 @ConfigurationProperties 的模块（core/spill/memory/guard/store-jdbc/store-redis/otel/dashboard/resilience/新五模块收口后全量）统一生效；observability 的 Binder 直绑迁移 @ConfigurationProperties；元数据 jar 内容在测试/构建里断言非空。

## §6 redteam/评测门真实性

**事实源**：promptfoo 官方 Testing Guardrails 指南（promptfoo 本体 ~5K★ 注记；契约是官方文档事实）。

要点：target 返回 `{output, guardrails:{flagged, ...}}`；HTTP provider 经 `transformResponse` 从响应头（如 `x-content-filtered`）派生 flagged；断言用 `type: guardrails`；CI 按退出码门禁；护栏评测关注 F1（真/假阳性平衡）。

对本仓采纳：
1. RedteamTarget 目标端点把真实 agent 回复写进响应体（替换硬编码文案），并以响应头 `x-buzhou-guard-blocked: true|false` 暴露拦截信号（guard 拦截或 HITL 阻断时 true）。
2. promptfooconfig 加 `transformResponse` 派生 `guardrails.flagged`，注入式攻击用例加 `type: guardrails` 断言（拦截=pass）。
3. `redteam/baseline.md` 落档首版基线；nightly 先观测（保留 || true 但输出指标），升硬门标准写入 README（连续 2 次 nightly 全绿且无新 high/critical → 升 `-DblockOnHigh`）。
4. nightly 补审计链重放校验 job（兑现 README 承诺，直接复用 examples 侧 AuditChainVerifier 集成测试）。

## 不达标注记汇总

- micrometer 本体（~5K★）：经 Spring Boot（75K+）官方栈事实间接采纳其惯例。
- MCP 官方 SDK（各语言均 <10K★）：elicitation 为规范文档事实；client 实现细节以 LangChain（100K+）实践为源。
- promptfoo（~5K★）：guardrails 契约为官方文档事实；工具本身已在本仓 CI 使用（现状延续）。
- jaCoCo/spotless/spotbugs 等构建插件：非 runtime classpath，不受 10K★ classpath 政策约束；按 Spring/Gradle 等超大项目的 CI 实践取舍（T79 决）。

## Sources

- [Spring Boot Endpoints 官方文档](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html)
- [Wiz: Actuator Misconfigurations](https://www.wiz.io/blog/spring-boot-actuator-misconfigurations)
- [Acunetix: All Actuator Endpoints Web Exposed](https://www.acunetix.com/vulnerabilities/web/spring-boot-misconfiguration-all-spring-boot-actuator-endpoints-are-web-exposed/)
- [OpenHands #207 Respect timeout in sandbox](https://github.com/OpenHands/OpenHands/issues/207)
- [OpenHands #1637 timeout handling](https://github.com/OpenHands/OpenHands/issues/1637)
- [OpenHands Sandbox 文档](https://docs.openhands.dev/openhands/usage/sandboxes/overview)
- [LangChain HumanInTheLoopMiddleware](https://docs.langchain.com/oss/python/langchain/frontend/human-in-the-loop)
- [LangChain HITL 讨论：勿信工具自报元数据](https://www.reddit.com/r/LangChain/comments/1uv8iul/human_in_the_loop_best_practices/)
- [Spring Boot annotation processor 官方文档](https://docs.spring.io/spring-boot/specification/configuration-metadata/annotation-processor.html)
- [Spring Boot issue #34210：additional-metadata 合并语义](https://github.com/spring-projects/spring-boot/issues/34210)
- [Baeldung: Configuration Metadata（optional 依赖惯例）](https://www.baeldung.com/spring-boot-configuration-metadata)
- [promptfoo: Testing and Validating Guardrails](https://www.promptfoo.dev/docs/guides/testing-guardrails/)
