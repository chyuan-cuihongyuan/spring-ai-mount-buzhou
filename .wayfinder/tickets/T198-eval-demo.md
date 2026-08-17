---
Type: task
Status: closed
blocked-by: T193-eval-runner.md, T194-eval-query.md
---
## Question

examples 评估闭环演示：负反馈产生 → 回流建数据集 → run 执行 → 汇总查询 → 事件到达
宿主视角单测试。

## Resolution

impl-164 落地：演示① 完整闭环（负反馈→回流→run→查询→事件五段断言）；演示② 幂等回流 +
自定义评估器 SPI 插入。2 测试绿。T198 关闭。
