---
name: code-review
description: 代码评审清单与严重度分级标准
allowed-tools: read_file, read_range, run_command
---

# Code Review Skill

## 评审步骤
1. 通读变更，理解意图
2. 按清单逐项核查
3. 标注严重度并给出修改建议

## 严重度分级
- P0：阻塞合并（安全/数据丢失/崩溃）
- P1：建议修改（逻辑/性能）
- P2：可选优化（风格/可读性）

同目录资源以相对路径引用，如 checklists/security.md。
