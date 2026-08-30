---
Type: task
Status: closed
---
## Question

AgentSession.rateTurn(turnSeq, type=boolean|numeric|categorical, value, comment, source=user|implicit)——落 EventRecord（turn.feedback 事件）+ webhook 外发；校验 turnSeq 存在、value 域合法；Langfuse score API 语义（挂 turn 级）。验证：API 单测 + 事件断言。

## Resolution

spec 47 §B / impl-142 落地：AgentSession.rateTurn(turnSeq, type, value, comment, source)
（Langfuse score 语义收窄）——校验（type 三型值域/source 两值/轮次 [1,currentTurn]/关闭拒绝）→
state store 持久化（buzhou.feedback.<turnSeq>.<millis>-<seq> 键——同轮同毫秒不撞键（实现期纠偏：
纯 epochMillis 键在快速连续反馈下碰撞覆盖，补会话级单调序号）；URLEncoded k=v 五字段 lossless）→
turn.feedback 会话事件外发（comment 非空才带）。HookEnvironment.stateStore() 访问器新增。
core 326 测试绿（三型落库+事件/六路拒绝/关闭后拒绝）。
