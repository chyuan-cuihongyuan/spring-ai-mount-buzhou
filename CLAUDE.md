# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目定位

Spring AI Mount Buzhou（不周山）是挂载在 **Spring AI 与业务 Agent 之间的运行时中间层（Harness）**——叠加而非替代 Spring AI。提供九大机制：渐进式记忆压缩、Spill 溢出保护、Span+Event 认知可观测、Skill 体系、MCP 热插拔、并行工具调用、原子工具、Hook 护栏体系（读写护栏/HITL/事实闭环）、持久化 SPI。

- 技术基线：JDK 21+（虚拟线程）、Spring Boot 4.x、Spring AI 2.0.0（工具调用循环在 Advisor 链内）
- 文档与注释主语言为中文；领域术语以根目录 [CONTEXT.md](CONTEXT.md) 为准
- 仓库处于设计落地阶段：设计 Spec 在 [docs/spec/](docs/spec/)（00-overview 总入口 + 9 份机制详设），**改机制先改 Spec**

## 构建与测试

```bash
mvn verify                          # 全量构建 + 测试（CI 守门命令，PR 必过）
mvn -pl buzhou-core -am test        # 单模块测试（-am 连带构建依赖模块）
mvn -pl buzhou-core test -Dtest=HookChainTest           # 单个测试类
mvn -pl buzhou-core test -Dtest=HookChainTest#method    # 单个测试方法
```

- 环境：JDK 21+、Maven 3.9+
- 本机构建（对 repo.maven.apache.org 有 TLS 指纹级拦截的环境）：首次执行 `cp settings.xml ~/.m2/settings.xml`，之后 `mvn` 直连阿里云镜像；CI（ubuntu-latest）直连 central，无需此文件。**POM 不声明 repositories**——阿里云镜像只放仓库根 `settings.xml`，避免已发布 POM 把镜像传染下游、被 Central Portal 拒收
- `buzhou-store-jdbc` 的测试用 Testcontainers，需要 Docker 可用

## 模块架构（16 模块星形依赖）

依赖图是以 `buzhou-core` 为根的两层星形，**物理无环**。硬性规则（见 [docs/spec/09-modules-engineering.md](docs/spec/09-modules-engineering.md)）：

1. **feature 模块之间禁止直接依赖**（memory/spill/skills/mcp/guard/tools/store-* 互不依赖）；跨机制协作一律走 core 的**事件总线**或 core SPI。这是依赖白名单，不是建议。
2. 唯一允许的二层边：`buzhou-observe-otel` / `buzhou-observe-dashboard` 依赖 `buzhou-observability`。
3. store 实现（jdbc/redis）只依赖 core SPI，由用户按需引入、`buzhou.store.type` 配置激活。
4. 每个机制模块独立可用（用户只引 `buzhou-memory` 即得记忆压缩）；`buzhou-spring-boot-starter` 只做依赖聚合、无代码。
5. 每模块一个 AutoConfiguration，`META-INF/spring/...AutoConfiguration.imports` 注册，`@ConditionalOnProperty("buzhou.<mech>.enabled")` 控制开关；safe-by-default 项默认开，otel/dashboard 默认关。

### buzhou-core 内核分包（`io.github.chyuan_cuihongyuan.buzhou.core`）

- `session` — 会话入口：`AgentRuntime.spawn()` 门面、`AgentSession`、租约（同会话单活跃实例）、SpawnOptions/Builder
- `exec` — **执行脊柱** `HarnessToolCallingManager`：替换 Spring AI 默认 ToolCallingManager，虚拟线程并行 fan-out、按序回注、超时与取消传播
- `hook` — Hook 链基础设施（beforeTool/afterTool/beforeModel/afterModel 等切面）；memory/spill/guard 等机制都是挂在 Hook 链上的内置 Hook
- `spi` — 全部扩展点：持久化五 SPI（MessageStore/SummaryStore/SessionStateStore/SessionLeaseStore/ObservabilityStore）+ TokenEstimator、ToolSetProvider、FactStore 等，内存默认实现
- `policy` — 四层配置覆盖模型：默认 < yml < 绑定级 < 工具级
- `internal/*` — 实现细节

### 包结构约定

- 根包 `io.github.chyuan_cuihongyuan.buzhou`（groupId 连字符转下划线）；机制模块为 `...buzhou.<mech>`
- `api` 子包是公共 API（语义版本承诺）；`internal` 子包跨模块禁止引用、不承诺兼容
- groupId `io.github.chyuan-cuihongyuan`，artifactId 统一 `buzhou-*`，全模块同版本由 `buzhou-bom` 统一

## 测试约定

- **持久化 SPI 契约测试模式**：core 发布 test-jar，内含 `AbstractBuzhouStoresContractTest`（`buzhou-core/src/test/.../contract/`）；store 实现模块（jdbc/redis）依赖该 test-jar 并继承契约测试类，保证所有存储实现语义一致。新增 store 实现时复用此模式。
- 行为变更必须带测试（CONTRIBUTING 约定）。

## 工作流程约定

- **Spec 先行**：机制设计以 `docs/spec/` 为准，改机制先改 Spec；忠实度原则——蓝本（携程 Spring-Ai-Trip 文章、腾讯 DECO hooks 文章）明确描述的机制严格遵循，留白处自主推演并以 `> 【推演】` 标注
- **Issue tracker 是本地 markdown**（[docs/agents/issue-tracker.md](docs/agents/issue-tracker.md)）：`.scratch/<feature-slug>/issues/NN-<slug>.md`，一票一文件，`Status:` 行记录 triage 状态；实现期 ticket 在 `.scratch/buzhou-implementation/issues/`（01–21），设计决策票在 `.scratch/spring-ai-trip/`
- 提交信息遵循 Conventional Commits（`feat:`/`fix:`/`docs:`/…），一个 PR 只做一件事
- 公共 API 变更需在 PR 描述中说明兼容性影响

## 关键机制速查

- **微压缩**：纯内存不调 LLM，旧工具返回替换为带 evidence-id 的占位符；以「完结轮次」为原子单位
- **动态预算**：「先扣后算」——窗口减去输出预留/安全缓冲/系统提示/工具 Schema/当前输入后才是历史预算
- **Spill**：工具返回超阈值（默认 32000 字符）自动落盘，上下文留预览（默认 2048）+ 路径，模型持 `read_range` 回读（字节区间/JSON path/分页）；读侧 offload 失败降级透传，写侧 onload 失败阻断（失败语义非对称）
- **悬空调用修复**：加载历史时自动修复残缺工具调用消息（完全悬空剔除/部分悬空合成中断结果）
- **Hook→state→Attachment 闭环**：Hook 确定性采集事实写会话 state，下一轮注入前渲染为 Attachment 进 prompt，不靠 LLM 自觉
