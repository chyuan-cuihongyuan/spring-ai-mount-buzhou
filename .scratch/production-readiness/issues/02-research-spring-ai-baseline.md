# Spring AI 2.0 / Spring Boot 4 内置能力盘点

Type: research
Status: resolved

## Question

不周山"叠加而非替代 Spring AI",缺口决策必须精确知道**底座已有什么**。盘点 Spring AI 2.0.0 与 Spring Boot 4.x 生态内置的、与生产韧性/治理相关的能力边界:

1. **重试**:`spring.ai.retry.*` 配置的覆盖范围(哪些 client、什么语义、可否按模型区分),与 Spring Retry 的关系
2. **超时**:模型调用/工具调用超时的官方配置途径
3. **错误处理模型**:模型调用异常体系、内容过滤拒绝的表现形式
4. **熔断/fallback**:ChatClient/ChatModel 层有无官方支持;与 Resilience4j 组合的官方/社区做法
5. **可观测**:内置 Observation/Micrometer 埋点的维度(token usage、耗时等),与自有观测的叠加方式
6. **工具调用**:ToolCallingManager 扩展点、工具异常处理语义
7. **Spring Boot 4 侧**:graceful shutdown 语义、虚拟线程支持现状
8. **Advisors API**:Advisor 链的切面能力与边界(不周山 Hook 挂于其上)

产出:能力清单 + 关键配置示例 + **明确不管的缺口**(底座留白即不周山的合法空间),写入 `../research/02-spring-ai-baseline.md`,中文,附官方 reference docs / GitHub 一手链接。

## Answer

(2026-08-11,研究子代理 AFK 收口)

**要点**:Spring AI 2.0 底座——重试 `spring.ai.retry.*` 为全局单例且 **429 默认不重试**(须显式配置);无统一超时属性、工具调用无任何超时;**熔断/fallback 官方零支持**(社区用 Resilience4j 注解包装);内置观测遵循 OTel GenAI 约定但仅单调用粒度、无会话级 Span 树;ToolCallingManager 可整体替换、默认多工具串行单线程无并行/超时/取消(与不周山执行脊柱设计完全对齐);Boot 4 `server.shutdown=graceful` 只管 web 请求。文末附 8 条底座留白清单(= 不周山合法空间)。

**成果**:分支 `research/spring-ai-baseline`(commit 962793f)→ `.scratch/production-readiness/research/02-spring-ai-baseline.md`。注:本环境 docs.spring.io 直连被拦,论断以 context7 镜像官方 javadoc/reference + DeepWiki 直读 v2.0.0 源码为准,未逐字核验处文中已标注。

**解锁**:03 模型层韧性 由此解除阻塞,进入 frontier。
