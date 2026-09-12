# 514 — fork 谱系游走环防护

**What to build:** ForkLineageWalker.walk（SOURCE 链上溯 + visited 环检测 + maxDepth 封顶）+ Lineage 快照——导入路径注入环/超深链时消费方不死循环。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ForkLineageWalker（walk + Lineage record + 默认深度 64）
- [x] 树/环/深链/无源四组用例
- [x] spec 711 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
