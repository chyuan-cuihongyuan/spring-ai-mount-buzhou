# 崩溃中轮次恢复 + 幂等控制

Type: grilling
Status: resolved

## Question

进程崩溃时 **in-flight turn**(正在执行中的轮次,含在途工具调用)怎么办?与之孪生的**幂等控制**(参考文档二.6:请求唯一 ID、重复请求拦截、防重复调用支付等高危工具)怎么做?

需回答:
1. **做不做**——崩溃中轮次是"丢弃重放"还是"检查点续跑",还是按场景分档
2. **机制边界**——恢复粒度(轮次级/工具调用级);副作用工具的幂等归框架还是归工具实现;幂等键的产生与存储归谁
3. **接缝**——与租约(同会话单活跃实例)、悬空调用修复(加载时修复历史)的关系;现有 MessageStore/SessionStateStore SPI 够不够,要不要检查点 SPI;幂等拦截挂在执行脊柱还是 Hook 链

答题要求:同 03 号票(做/不做 + 机制边界 + 接缝;借鉴成熟实现——如 LangGraph checkpointer 的语义——并注明来源)。

## Answer

(2026-08-11,grilling 收口,四问全采纳推荐)

**决策:做**——恢复语义分档 + 持久化强度分档 + 幂等三件套;不新增 SPI。

**机制边界(管什么/不管什么)**:
- **崩溃中轮次恢复语义分档**:默认"轮次作废"——新实例修复历史后等用户重驱动(现状语义明确化+文档化);opt-in"自动重驱动续跑"(无人值守/长任务会话)——修复历史后自动发起模型调用继续该轮。自动重驱动的崩溃循环风险由 03/04 韧性层熔断配合兜底
- **持久化强度三档**(直接对标 LangGraph durability):`sync`(下一步前同步落盘)/`async`(默认,边执行边落盘)/`exit`(仅退出时落盘,最高吞吐),按绑定级配置;exit 档崩溃丢失整轮的风险由恢复语义兜底
- **幂等三件套**(业界空白,Spec 需 `> 【推演】`+自建评测):① 工具声明幂等性(元数据,副作用工具默认非幂等)② 幂等键——框架默认生成(会话+轮次+调用序号),业务可覆盖(如订单号)③ 去重记录——重试/重放命中键时返回首次结果而非重执行(at-least-once 调用 + 去重 = 效果恰好一次)
- 不管:工具内部业务逻辑的正确性;跨会话幂等(键作用域=会话内)

**接缝**:
- 不新增 SPI:消息即检查点(MessageStore 追加流),恢复=租约过期→新实例获取→加载+悬空修复→按档重驱动;五 SPI 够用,去重记录复用现有 SPI 扩展(细则留 Spec)
- 持久化强度写路径:MessageStore/SessionStateStore 的 UnitOfWork
- `exit` 档与 06 优雅停机强联动(停机必须 flush);恢复流程与租约、悬空调用修复(已有)咬合

**借鉴**:
- LangGraph Durability 三档(sync/async/exit,按调用显式取舍一致性/吞吐)— https://langchain-ai.github.io/langgraph/concepts/durable_execution/ 、https://reference.langchain.com/python/langgraph/types/Durability
- LangGraph Checkpointer SPI + 官方契约测试包(与不周山五 SPI + test-jar 模式同向,先例背书)— https://docs.langchain.com/oss/python/langgraph/checkpointers
- LangGraph put_writes"已完成写入不重放"思想(幂等去重的蓝本)— https://docs.langchain.com/oss/python/langgraph/persistence
- OpenAI Agents SDK Session 多后端 + 装饰器式包装(EncryptedSession 等)— https://github.com/openai/openai-agents-python
