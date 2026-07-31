# 持久化 SPI 与默认实现选型

Type: grilling
Status: open
Blocked by: 01

## Question

记忆/摘要/evidence 的持久化抽象怎么定：消息只追加落库、摘要版本化、evidence 指针回查，三者是一个 SPI 还是分开？默认实现给什么（JDBC？Redis？内存？）——开源项目不能绑定企业内部存储。跨实例会话续接对接口语义的要求（无本地状态、任意实例可加载）。与 Spring AI `ChatMemoryRepository` 是适配还是另立？事务与并发写（多实例同 sessionId）怎么处理？
