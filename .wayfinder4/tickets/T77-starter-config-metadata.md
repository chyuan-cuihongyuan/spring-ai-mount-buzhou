---
Type: grilling
Status: closed
blocked-by: T69
---

## Question

配置元数据与装配基建收口：spring-boot-configuration-processor 引入方式（根 pom 统一 vs 各模块）、additional-metadata 合并生效验证、JSR-303 @Validated 扩展到全部 properties record（spill/memory/guard/stores/otel/dashboard/新模块）、FailureAnalyzer 扩面（哪些非法配置值得翻译）、BOM 补 core test-jar 与 resilience 条目、observability 的 Binder 直绑迁 @ConfigurationProperties、starter 聚合反向用例与全上下文冒烟。

## Resolution

进本轮（采纳 T69 §5）：
1. **根 pom pluginManagement 统一配 maven-compiler-plugin annotationProcessorPaths = spring-boot-configuration-processor（optional 语义）**；全部含 @ConfigurationProperties 的模块自动生效（core/spill/memory/guard/store-jdbc/store-redis/otel/dashboard/resilience/skills/tools/mcp 新增 properties 后全量）；构建断言：单测抽查 jar 内 spring-configuration-metadata.json 存在且含本模块键（防回退为死文件）。
2. impl/43 的 4 份 additional-metadata 随 processor 生效（合并语义核验：additional 只补 hint/deprecated，不与生成条目冲突）。
3. JSR-303 扩面：全部 properties record 加 @Validated + 约束注解（spill/memory/guard/stores/otel/dashboard/五模块/resilience）。
4. FailureAnalyzer 扩面：新增 dashboard 安全配置 analyzer（非 loopback 无 token）+ resilience（deadline < maxBackoff 等矛盾组合）两个值得翻译的；其余非法配置靠 Binder 校验异常（不逐个翻译，注记）。
5. BOM 补 buzhou-core test-jar 与 buzhou-resilience 条目。
6. observability Binder 直绑迁 @ConfigurationProperties（T71 已含）。
7. starter 补反向用例（memory.enabled=false 降级、store.type=redis 无 Redis 时启动失败路径）与 @SpringBootTest 全上下文冒烟（Runner 不覆盖 FailureAnalyzer/SmartLifecycle 段）。（可推翻）
