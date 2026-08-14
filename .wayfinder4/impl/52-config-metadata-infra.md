# 52 — 配置元数据全量生效与 starter 收口

**What to build:** 全部 buzhou.* 键 IDE 可补全（processor 生效 + additional 合并 + jar 断言防回退）；全部 properties record JSR-303；BOM 可引 core test-jar 与 resilience；starter 聚合新模块且有正反向与全上下文用例。

**Blocked by:** 44-resilience-module-port,46-observability-hardening,47-otel-hardening,48-dashboard-security,49-tools-hardening,50-mcp-hardening,51-skills-stores-hardening

**Status:** ready-for-agent

- [ ] 根 pom compiler annotationProcessorPaths=configuration-processor；全模块生效
- [ ] impl/43 四份 additional-metadata 合并核验（jar 内最终 JSON 含 hint/deprecated）
- [ ] 构建断言：测试解 jar 查 spring-configuration-metadata.json 存在且含本模块键
- [ ] JSR-303 扩面全部 properties record（含新 resilience/runaway/backpressure/skills/mcp/tools/observability）
- [ ] FailureAnalyzer：dashboard 安全 + resilience 矛盾组合
- [ ] BOM 补 core test-jar + resilience；starter 聚合 resilience；反向用例（memory.enabled=false、redis 缺失启动失败）+ @SpringBootTest 全上下文冒烟
