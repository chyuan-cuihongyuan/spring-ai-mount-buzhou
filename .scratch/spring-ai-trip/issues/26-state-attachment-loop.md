# Hook→state→Attachment 上下文联动闭环

Type: grilling
Status: open
Blocked by: 06, 23

## Question

「补失忆」的联动闭环如何抽象成框架能力：会话级 state（Hook 采集的事实——如「改了表」「产出了图」）的数据模型与存储（挂持久化 SPI？）；Attachment 注入机制——下一轮注入模型前，把 state 中待消费的事实渲染进 prompt（注入位置、格式、消费后清除还是累积）；业务如何声明采集规则（框架给「afterTool 采集 → state → 下轮注入」的通用脚手架，业务填判定与渲染逻辑？）；与九段式摘要的交互（Attachment 事实要不要进摘要的 Current State 段？）；注入内容的 token 预算归属？
