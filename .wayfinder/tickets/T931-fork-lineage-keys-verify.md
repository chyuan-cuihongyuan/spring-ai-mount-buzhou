---
id: T931
title: 谱系键常量化的验证
type: task
status: closed
assignee: zcode-f
blocked-by: T930
created: 2026-09-13
---

## Question

写读两侧真的同源了吗？值没有被顺手改掉（wire 契约不破）？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（F 会话第 41 轮）：① grep 断言 main 代码零残留字面量（仅常量类一处定义 + Javadoc/注释提及）；② SessionForkLineageTest 保留字面量断言（真实 fork 双向钉住——常量类若改值测试即红，wire 契约钉死）；③ 常量一致性用例（SessionForkKeys.SOURCE == "buzhou.fork.source" 等三键）钉常量与字面量同值；④ `mvn -pl buzhou-core -am test` 全绿零回归。
