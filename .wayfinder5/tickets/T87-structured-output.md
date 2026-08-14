---
Type: task
Status: open
blocked-by:
---
## Question

结构化输出怎么做？现状：全仓无 EntityResponse/BeanOutputConverter 集成，agent 无法声明类型化输出；工具入参校验已有（ToolArgsValidator+REASK）。借鉴：LangChain withStructuredOutput、Spring AI 自带 entity() API。决策点：API 形态（AgentSession.chatForEntity(Class,schema)？流式？）、校验失败 REASK 一次再降级异常的语义、与 hook 链/observability 的关系（结构化输出算 event？）、schema 注入方式（Spring AI BeanOutputConverter format）。产出 spec 19 + impl 62。
