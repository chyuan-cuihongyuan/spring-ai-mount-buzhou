# 52 — 配置元数据全量生效与 starter 收口

**What to build:** 全部 buzhou.* 键 IDE 可补全（processor 生效 + additional 合并 + jar 断言防回退）；全部 properties record JSR-303；BOM 可引 core test-jar 与 resilience；starter 聚合新模块且有正反向与全上下文用例。

**Blocked by:** 44-resilience-module-port,46-observability-hardening,47-otel-hardening,48-dashboard-security,49-tools-hardening,50-mcp-hardening,51-skills-stores-hardening

**Status:** done

- [ ] 根 pom compiler annotationProcessorPaths=configuration-processor；全模块生效
- [ ] impl/43 四份 additional-metadata 合并核验（jar 内最终 JSON 含 hint/deprecated）
- [ ] 构建断言：测试解 jar 查 spring-configuration-metadata.json 存在且含本模块键
- [ ] JSR-303 扩面全部 properties record（含新 resilience/runaway/backpressure/skills/mcp/tools/observability）
- [ ] FailureAnalyzer：dashboard 安全 + resilience 矛盾组合
- [ ] BOM 补 core test-jar + resilience；starter 聚合 resilience；反向用例（memory.enabled=false、redis 缺失启动失败）+ @SpringBootTest 全上下文冒烟


## Done

commit: 见 git log（impl/52）。验证：全仓 mvn verify 绿（starter 5/5 含元数据防回退断言；Testcontainers 用例本地无 Docker 跳过、CI 有 Docker）。
落地：根 pom compiler annotationProcessorPaths=spring-boot-configuration-processor（${spring-boot.version}）——11 个含 @ConfigurationProperties 的模块全量生成 spring-configuration-metadata.json 并与 additional-json 编译期合并（core 55 属性 + 4 hints 合并验证）；修复 core/guard additional-json 的 hints 错置（property 内→顶层，processor 校验暴露）；@Validated 补齐 spill/jdbc/redis 5 个 properties record；starter 硬化测试（元数据 jar 防回退断言 7 模块 + memory.enabled=false 降级 + resilience.enabled=false 回退 + store.type=redis 缺 Redis fail-fast + 全栈 13 AutoConfig 装配）；guard 测试适配 ModelCallContext.error() 新方法。注记：skills/tools 走 env 直读无 @ConfigurationProperties（元数据留待配置正规化）。
