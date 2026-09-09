# Wayfinder Map — Buzhou 工具循环断路器（effort #327，C 会话第 28 轮）

> C 会话第 28 轮。326 抓"文本面复读"（模型输出）；更烧钱的是<b>工具面
> 循环</b>：同工具同参数连续调用——每次都真执行，烧下游配额/真金白银，
> 结果却不可能变。326 的工具面孪生，默认干预（工具调用是真执行，复读
> 文本只是占上下文）。

## Destination

`ToolLoopBreakerHook`（core.runaway，order 245 熔断后）：beforeTool 以
（toolName + args hashCode）为 key 做相邻 run 计数；run 达窗（默认 3）
→ block「[工具循环]」带换参/换工具/收束指令（默认干预——配额在烧）；
不同 key 重计（解闩同 326）；per-session 1024 整体重置。yml
`buzhou.runaway.tool-loop.window` 未配不装配。

## Notes

- 号段：spec 327 / T645–T646 / impl-350。
- 借鉴：326 相邻 run 语义平移到工具面；干预默认开（装配即干预）。

## Decisions so far

- key = toolName + args Map.hashCode（Map.hashCode 与顺序无关——稳定）。
- 交替循环（a,b,a,b）不做——只抓同 key 连续（最具破坏性的形态），
  序列模式检测诚实入 Out of scope。

## Out of scope

- 交替/周期序列检测（a,b,a,b）；embedding 语义近似参数判同；自动改参重试。

## Tickets

- [x] [T645 ToolLoopBreakerHook + 装配](../tickets/T645-tool-loop.md)（impl-350）
- [x] [T646 回归与收口](../tickets/T646-tool-loop-close.md)（impl-350）
