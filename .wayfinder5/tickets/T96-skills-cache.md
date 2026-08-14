---
Type: task
Status: open
blocked-by:
---
## Question

skills 清单缓存与预热及深化小项？现状：effort#4 已做资源上限/缓存/正文上限+frontmatter 多行；后续项剩「清单缓存」。核实后若已覆盖则转为其他深化项：DB SkillStore 启动预热（首次清单读取延迟消除）、清单变更事件、tools read_range 与 spill 的只读句柄复用等小缺口核实与补齐。产出 spec 22 增量 + impl 71。
