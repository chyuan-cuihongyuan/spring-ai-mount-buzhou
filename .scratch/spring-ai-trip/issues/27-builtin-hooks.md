# 内置通用 Hook 清单

Type: grilling
Status: open
Blocked by: 23

## Question

框架自带哪些通用 Hook（DECO 全景里的业务 Hook——血缘 offload、文件树事件、发布条目收集——不进框架，但其通用对应物要评估）：候选包括工具调用日志 Hook（入参/出参/耗时/成功率，与可观测层的关系）、对话持久化 Hook（落库 USER/MODEL/TOOL——是否就是记忆写入路径本身）、工具返回截断器（超阈值 Rerank 重排保留相关片段——与 Spill 的关系）、响应格式化、取消响应（beforeModel 检查用户取消）。每个候选：进框架内置 / 降为示例 / 不做；内置 Hook 与 Harness 核心机制的边界怎么划（哪些其实是核心机制而非可选 Hook）？
