# CONTEXT

项目领域术语表（只收术语，不收实现细节）。

## 核心概念

- **Harness（马具）** — 挂载在 Spring AI 与业务 Agent 之间的运行时中间层；本项目的定位。叠加而非替代 Spring AI。
- **Agent 运行时（Agent Runtime）** — 让单个 Agent 稳定、可控、可解释地跑在生产里的那层能力：记忆、溢出保护、可观测、能力供给、并发执行。
- **会话（Session / Conversation）** — 一次 Agent 与用户的完整多轮交互，以 sessionId 标识，可跨实例续接。
- **轮次（Turn）** — 一次用户输入到 Agent 最终回复的完整往返；内部可能含多轮"思考—工具调用"递归。
- **完结轮次（Completed Turn）** — 工具调用链完整结束、不再有在途调用的轮次；微压缩的原子单位。

## 记忆与压缩

- **渐进式压缩（Progressive Compaction）** — 信息从高精度原文连续、分级地降级到高密度摘要，永不断崖式丢弃。
- **微压缩（Micro-compaction）** — 纯内存、不调 LLM 的工具结果回收：旧工具返回替换为带证据指针的占位符。
- **证据指针（evidence-id）** — 微压缩占位符中指向持久化层原始工具返回的标识，供排障回查。
- **九段式摘要（Structured Summary）** —  LLM 按固定九段模板生成的结构化对话摘要，段落带优先级（P0 死保，P3 先砍）。
- **动态预算（Dynamic Budget）** — "先扣后算"：窗口减去输出预留、安全缓冲、系统提示词、工具 Schema、当前输入后，剩余才是历史预算。
- **悬空调用（Dangling Tool Call）** — 进程中断导致工具调用发出而结果未落库的残缺消息，加载历史时需自动修复。

## Spill

- **Spill（溢出保护）** — 超大工具返回值自动落盘持久化，上下文中只留预览 + 说明 + 回读路径。
- **回读（Read-back）** — 模型持 spill 路径主动取回数据，支持范围读取（字节区间 / JSON path / 分页）。

## 可观测

- **Span** — 有始有终、可嵌套的执行区间（会话 ⊃ 轮次 ⊃ 模型调用/工具调用）。
- **Event** — Span 内部的关键瞬间：思维链（Thinking）、最终回复、工具入参/出参、错误。
- **认知可观测（Cognitive Observability）** — 记录模型基于什么证据、做出什么推理、得到什么结论，而不只是"调用发生了"。

## 能力供给

- **Skill（技能）** — 按需加载的能力单元；上下文只放清单（name + description），需要时再取正文。分内置（classpath）与 DB 动态两种来源，同名 DB 覆盖内置。
- **MCP 热插拔** — 工具集由配置驱动、运行时热更新；靠差量刷新 + 引用计数延迟关闭保证安全。
- **原子工具（Atomic Tools）** — 框架内置的最小可复用工具集：文件读写、命令执行、HTTP 调用、任务清单等。

## Hook 护栏

- **Hook 链（Hook Chain）** — 框架在模型调用与工具调用前后暴露的 Callback 切面（beforeTool/afterTool/beforeModel/afterModel 等）；护栏逻辑挂于其上，与推理循环解耦。
- **引用句柄（Reference Handle）** — 长内容落盘后留在上下文中的指针文案（含路径与操作指引），LLM 凭句柄按需回读或编辑。
- **Onload（写侧加载）** — 工具执行前由 Hook 从文件加载长内容全文、覆盖工具入参的写侧护栏；与读侧 Offload 对称。
- **失败语义非对称** — 读侧 offload 失败降级透传（不阻断），写侧 onload 失败阻断调用（杜绝残缺产物外流）。
- **HITL 门禁（Dangerous Tool Guard）** — 配置驱动的危险工具拦截：未获真实用户授权，不可逆操作在框架层物理走不通；授权以 state 标记放行。
- **Hook→state→Attachment 闭环** — 补失忆范式：Hook 确定性采集事实写入会话 state，下一轮注入模型前以 Attachment 渲染进 prompt，不靠 LLM 自觉。
