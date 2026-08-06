---
name: sql-tuning
description: 慢 SQL 诊断与索引优化指引
---

# SQL Tuning Skill

## 诊断流程
1. EXPLAIN 执行计划
2. 定位全表扫描/临时表/文件排序
3. 检查索引覆盖度与选择性

## 索引原则
- 高选择性列优先
- 覆盖索引避免回表
- 联合索引最左前缀
