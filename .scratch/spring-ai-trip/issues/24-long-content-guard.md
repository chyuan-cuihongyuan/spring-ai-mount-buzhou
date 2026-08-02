# 长产物读写护栏（Offload/Onload + 引用句柄）

Type: grilling
Status: resolved
Blocked by: 11, 23

## Question

在 Spill（读侧溢出）之外，DECO 文章补上了写侧对称问题：LLM 产出的长内容（上千行 SQL/脚本）在拼工具入参时自截断、占位略写。设计待决：读侧 offload 与 Spill 是同一机制还是两层（DECO 的引用句柄 = Spill 占位符的特例？）；写侧 onload——`scriptContent`/`scriptFilePath` 互补参数契约如何泛化成框架级协议（哪些工具参数声明为「长内容参数」，Hook 自动从文件加载全文覆盖）？只读快照/工作副本分离（强制 copy_file 后才能 str_replace）是否纳入框架？失败语义的非对称（读侧降级透传、写侧阻断抛异常）如何建模？与内置原子工具（文件读写/str_replace）的协同？

## Answer

**定案：offload=Spill Hook 化 + 注解声明写侧协议 + 副本分离默认拦截 + 失败语义三态建模。**

1. **读侧统一**：offload 就是 Spill 的 Hook 化实现（23 狗粮原则落地）——afterTool Hook 检测超长结果走 SpillStore 落盘，上下文留引用句柄（= 11/12 的 spill 占位符 + 回读指引）；不分两层，Spec 统一术语。
2. **写侧 onload 泛化**：框架级协议——工具参数声明 `@LongContentParam`（或 Schema 元数据标记），配套 `xxxPath` 互补参数按命名约定生成；beforeTool Hook 检测到路径参数非空即从文件加载全文覆盖内容参数；任意工具（含 MCP 包装）声明即生效。
3. **只读快照/工作副本分离**：纳入框架——文件编辑类工具（str_replace 等）默认要求目标为工作副本；直改只读来源文件被 Hook 拦截并提示先 `copy_file`；内置原子工具（19）增补 `copy_file` / `str_replace`。
4. **失败语义非对称**：映射 23 密封三态——读侧 offload 失败降级透传（Hook 内部吞异常返 CONTINUE 带原文 + 告警 Event）；写侧 onload 失败 BLOCK 阻断调用（杜绝残缺产物外流）+ Error Event；Spec 专节论述。
