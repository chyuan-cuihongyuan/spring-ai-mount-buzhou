# Spill 回读工具设计

Type: grilling
Status: resolved
Blocked by: 11

## Question

回读工具的接口设计：范围读取的三种模式（字节区间 / JSON path / 分页游标）如何统一成一个模型可调用的工具签名？JSON List 的智能预览（前 N 项 + 计数摘要）规则？回读结果本身的二次 Spill 防护（回读又超阈值怎么办）？这个工具作为内置原子工具自动注册的条件？模型"知道可以回读"的提示从哪里注入（spill 占位符文案 vs 系统提示词 vs Skill）？

> 范围追加（ticket 08 决议）：范围读取实现提升为 **core 共享能力**——Spill 回读与微压缩 evidence 回查是两个包装，共用同一套范围读取（字节区间/JSON path/分页）。

## Answer

**定案：单工具 mode 参数 + 递归 spill 防护 + 占位符自含提示 + 前 N 项预览 + 默认注册。**

1. **工具签名**：单工具 `read_range(path, mode=bytes|json|page, offset|jsonPath|cursor, limit)`——模型一次调用只走一种模式，底层共用 ticket 08 定案的 core 共享范围读取能力；微压缩 evidence 回查工具是同一能力的另一包装。
2. **二次 Spill 防护**：回读结果递归走 spill 管道（复用工具返回统一出口），超阈值再落盘，上下文中留新预览 + 新路径，模型可逐层缩小范围续读；实现零新增。
3. **提示注入**：spill 占位符文案自含回读指引（路径 + `read_range` 调用示例），模型在看到占位符的当轮即获知；系统提示词放一句简短兜底声明；不依赖 Skill 加载。
4. **JSON List 智能预览**：`{"items": [前 20 项], "totalCount": N, "truncated": true}` + 可用 jsonPath/分页续读的提示；N 可配，默认 20。
5. **注册条件**：作为内置原子工具默认注册，工具策略可关；仅在所在会话启用 spill 时出现在工具清单。
