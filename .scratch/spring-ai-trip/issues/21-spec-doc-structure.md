# Spec 文档集结构与写作模板

Type: grilling
Status: resolved
Blocked by: 03

## Question

终点交付物的结构：`docs/spec/` 下文档集怎么组织（00-总览 + 每机制一份？编号与命名）？每份机制详设的固定模板（设计目标 / 术语 / API / 配置项 / 存储 Schema / 时序图 / 推演标注 / 开放问题）定成什么样？"文章忠实 vs 自主推演"在文档里的标注规范（如 `> 【推演】` 块）？总览文档包含什么（架构图、模块依赖图、端到端数据流——对应文章第三、九章）？写作语言与图表规范（中文 + Mermaid？）？

## Answer

**定案：总览 + 每机制一份编号档 + 固定八节模板 + 推演就地标注并汇总 + 中文 Mermaid。**

1. **组织**：`docs/spec/00-overview.md` 总览 + 每机制一份编号档：`01-memory-compaction` / `02-spill` / `03-observability` / `04-skill-mcp` / `05-parallel-tools` / `06-atomic-tools` / `07-hooks` / `08-session-config-persistence` / `09-modules-engineering`；编号序稳定，机制间交叉引用。
2. **机制详设固定八节**：设计目标 / 术语（回链 CONTEXT.md）/ API / 配置项 / 存储 Schema / 时序 / 推演标注 / 开放问题。
3. **推演标注**：推演处用 `> 【推演】` 引用块就地标注，蓝本明确处不标；总览附「推演清单」一节汇总全部推演点（含本图各 ticket 标注的推演修正，如 11 的 toolCallId 命名），便于社区挑战。
4. **总览内容**：架构图、模块依赖图（03 的 16 模块星形依赖）、端到端数据流（对应文章第三、九章）、机制索引、推演清单。
5. **语言与图表**：中文正文 + Mermaid 图（时序/架构/模块依赖），术语首次出现附英文原名；GitHub 原生渲染，零工具链。
