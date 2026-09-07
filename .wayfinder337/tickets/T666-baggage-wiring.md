---
Type: task
Status: closed
---
## Question

HarnessToolCallingManager 传播点（ToolContext 注入 buzhou.baggage 快照、
空行李零注入）+ HarnessAssembler.withToolBaggage + 装配（yml 播种、bean
恒在）+ 收口。

## Resolution

done（2026-09-04）：impl-360；传播三用例（读到快照/空无键/构造后 put
下次调用可见）+ 装配两用例（yml 播种/无 yml 空视图 bean 恒在）绿；
快照 regenerate +1 型；README 纵深 IV 加行、覆盖门绿。
