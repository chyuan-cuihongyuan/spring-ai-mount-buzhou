# 并行工具调用设计

Type: grilling
Status: resolved
Blocked by: 01

## Question

并行工具调用的实现方案：Spring AI 工具执行链上哪里可以并发化（一轮内多个独立 tool_call 的派发点——依赖 01 的结论）？虚拟线程执行器的设计（池化？每轮新建？限界？）；结果顺序与模型请求顺序严格对齐的回注机制；并发下 Span 归属、Spill 命名、微压缩完结判定的一致性规则；超时与部分失败策略（一个工具 hang 住整轮怎么办——单工具超时、取消传播）？"独立调用才可并行"的判定（模型声明 vs 框架假设）？

## Answer

**定案：HarnessToolCallingManager fan-out + 会话级执行器限界 + 单工具超时失败转文本 + 默认可并行声明式串行例外。**

1. **派发点**：`HarnessToolCallingManager implements ToolCallingManager`（01 结论的公开扩展点，经 ToolCallingAdvisor.Builder / Boot Bean 注入）——同轮多个 tool_call 虚拟线程 fan-out，结果严格按 tool_call 原序聚合并回注 ToolResponseMessage；Spill 大结果替换也在同一实现内（拼 ToolResponseMessage 前），与 14 的可观测挂接（advisor/ToolCallback 层）不冲突。
2. **执行器**：会话级共享虚拟线程执行器（每会话一个，随 close 销毁，入会话资源注册表）；每轮并发上限可配（默认 8，信号量限界）防模型失控扇出；虚拟线程不池化。
3. **超时与部分失败**：单工具超时可配（默认 60s）；超时/异常不影响同轮其他调用，失败项结果替换为「执行失败/超时：原因」文本回注（对齐 Spring AI 异常转字符串语义）；轮次取消时取消传播到全部在途调用。
4. **并行判定**：框架默认同轮 tool_call 均可并行；工具可声明串行（注解或策略配置串行组，如同一资源写操作）强制排队——默认可并行 + 声明式串行例外。
5. **并发一致性**：Span 归靠 14 的显式上下文传递；Spill 命名靠 11 的 toolCallId 天然唯一；微压缩完结判定（08）以 tool_calls 全回应为准，并行不改变判定输入。
