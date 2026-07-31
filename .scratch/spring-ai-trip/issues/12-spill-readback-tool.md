# Spill 回读工具设计

Type: grilling
Status: open
Blocked by: 11

## Question

回读工具的接口设计：范围读取的三种模式（字节区间 / JSON path / 分页游标）如何统一成一个模型可调用的工具签名？JSON List 的智能预览（前 N 项 + 计数摘要）规则？回读结果本身的二次 Spill 防护（回读又超阈值怎么办）？这个工具作为内置原子工具自动注册的条件？模型"知道可以回读"的提示从哪里注入（spill 占位符文案 vs 系统提示词 vs Skill）？
