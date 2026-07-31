# 长产物读写护栏（Offload/Onload + 引用句柄）

Type: grilling
Status: open
Blocked by: 11, 23

## Question

在 Spill（读侧溢出）之外，DECO 文章补上了写侧对称问题：LLM 产出的长内容（上千行 SQL/脚本）在拼工具入参时自截断、占位略写。设计待决：读侧 offload 与 Spill 是同一机制还是两层（DECO 的引用句柄 = Spill 占位符的特例？）；写侧 onload——`scriptContent`/`scriptFilePath` 互补参数契约如何泛化成框架级协议（哪些工具参数声明为「长内容参数」，Hook 自动从文件加载全文覆盖）？只读快照/工作副本分离（强制 copy_file 后才能 str_replace）是否纳入框架？失败语义的非对称（读侧降级透传、写侧阻断抛异常）如何建模？与内置原子工具（文件读写/str_replace）的协同？
