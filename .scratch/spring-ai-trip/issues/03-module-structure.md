# Maven 多模块划分与依赖方向

Type: grilling
Status: resolved
Blocked by: 01

## Question

多模块 Maven 结构怎么切：每个机制独立可引入（用户硬要求），core、memory、spill、observability、skills（含 MCP 热插拔）、tools、dashboard、starter、BOM、示例模块之间如何划分与依赖？共用内核（会话模型、持久化 SPI、策略模型、token 估算）放哪个模块？模块间的循环依赖如何避免（如 spill 依赖 observability 挂 Span，observability 又需感知 spill）？bom 与 starter 的命名（结合 groupId）。

## Answer

**定案：细粒度 12 模块 + core 事件总线解耦 + buzhou-* 短前缀 + 模块自装配。**

### 模块清单（groupId 预期 io.github.\<用户名\>，待 ticket 20 确认）

| 模块 | 职责 | 依赖 |
|---|---|---|
| `buzhou-bom` | 全模块版本统一 | — |
| `buzhou-core` | 会话入口（spawn API）、持久化 SPI、配置/策略模型、token 估算器、Hook 链基础设施、**事件总线**、执行脊柱（HarnessToolCallingManager，含虚拟线程并行工具调用） | spring-ai-client-chat |
| `buzhou-memory` | 渐进压缩：微压缩、九段摘要、动态预算、悬空修复 | core |
| `buzhou-spill` | 长内容治理：读侧 Spill + **写侧 Onload + 引用句柄**（读写护栏同模块，共享存储抽象）+ 回读工具 | core |
| `buzhou-observability` | Span/Event 采集，经 **core 事件总线订阅** 各机制事件 | core |
| `buzhou-dashboard` | 可视化后台（查询 API + 前端静态资源），独立可部署 | observability |
| `buzhou-skills` | Skill 体系（classpath 内置 + DB 动态） | core |
| `buzhou-mcp` | MCP 热插拔（client 生命周期注册表、差量刷新、引用计数延迟关闭） | core |
| `buzhou-guard` | HITL 危险操作门禁 + Hook→state→Attachment 联动闭环 | core |
| `buzhou-tools` | 内置原子工具（文件/命令/HTTP/任务清单） | core |
| `buzhou-spring-boot-starter` | 聚合全部机制 | 以上全部 |
| `buzhou-example` | 示例应用（不发布 Maven Central，或独立 profile） | starter |

### 关键决策

1. **core 收厚**：会话模型、持久化 SPI、策略模型、token 估算、Hook 链、事件总线、并行执行脊柱全部归 core——它们是各机制的公共地基；并行工具调用不单列模块（它是执行脊柱的属性，其他机制经脊柱注入）。
2. **解环 = core 事件总线**：core 定义事件/SPI（SpanEvent、SpillEvent、CompactionEvent、GuardEvent 等）；机制模块只向总线发事件，observability 订阅总线。**严禁 feature→feature 直接依赖**，依赖图是以 core 为根的两层星形，物理无环。
3. **命名**：artifactId 统一 `buzhou-*` 短前缀（buzhou-core … buzhou-bom），仓库名 spring-ai-mount-buzhou。
4. **装配**：每机制模块内置 AutoConfiguration，`@ConditionalOnProperty("buzhou.<mech>.enabled")`，safe-by-default 项默认开；`buzhou-spring-boot-starter` 聚合全量，用户引单个机制模块即得单机制能力。
5. **护栏归属**：长产物写侧 Onload 归 `buzhou-spill`（与读侧 Spill 共享存储抽象与策略模型）；HITL 与联动闭环归 `buzhou-guard`。

### 增补（ticket 06 决议，2026-08-01）

- 存储扩展模块 ×2：`buzhou-store-jdbc`（生产主推）、`buzhou-store-redis`；内存实现收在 core 内作默认。模块总数 12 → 14。

### 增补（ticket 05/07 决议，2026-08-01）

- 可选扩展约定：配置中心适配（如 `buzhou-config-nacos`）与精确分词（`buzhou-tokenizer-jtokkit`）属 community-extension，按需添加，不计入主干模块数。
