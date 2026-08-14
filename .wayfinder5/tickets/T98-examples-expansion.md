---
Type: task
Status: closed
assignee: zcode
blocked-by: T82,T83,T87,T88,T89
---
## Question

examples 如何扩充新能力 e2e？决策点：新增 demo 测试清单（熔断打开→降级链切换、成本预算触顶 Block、fork 后双会话分叉、webhook 收到事件断言、结构化输出 REASK、配额超限）——全部 ScriptedChatModel/Fake 驱动、纳入 examples 既有四簇 README 结构、每测外部行为断言。产出 impl 73。

## Resolution

AFK 自决（授权同 T81，可推翻）：

1. **新增 `NewCapabilitiesDemoTest`**（demo 簇）7 用例：降级链保命 / token 预算触顶 / fork 分支+预算重置 / webhook 签名投递（JDK HttpServer 收件）/ 结构化输出 REASK 恢复 / 两败 StructuredOutputException / 日配额拦截——全部替身模型驱动、外部行为断言。
2. **examples 增 buzhou-resilience test 依赖**（聚合侧跨机制 e2e 承载，对齐 guard/tools 先例）。
3. README 四簇结构不动——新簇归 demo（机制演示簇），收口轮（T102）统一更新 README。
