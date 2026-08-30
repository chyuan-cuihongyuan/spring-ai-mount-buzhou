# 19 — spill · AST-aware 切片（JavaParser + 分隔符回退）

**What to build:** 源码回读按语义边界切片：Java 全 AST（函数/类不斩断）、其他语言分隔符启发式回退、先切再解析避 32KB cliff、永不静默失败。

**Blocked by:** None — can start immediately.

**Status:** done（2026-08-14：ContentSlicer——Java AST-lite（字符串/注释感知的花括深度 0 边界，零依赖；JavaParser 6.1K★ 不引入留工程注记）+ 语言分隔符阶梯递归二分（py/js/sql/json/text）+ 永不静默元数据标记 + 超长行硬截注记 + 无损拼接断言；SemanticSlicingTest 2 例）

- [ ] Java 全 AST 用 JavaParser（纯 JVM；非达标源工程注记；依赖坐标与 scope 评估落定）
- [ ] 其他语言：LangChain 式语言分隔符表 + 行边界启发式回退
- [ ] 先切再解析：不把整文件/残片喂解析器；超长行硬截
- [ ] 回退链 AST→分隔符→行边界，每级可观测、永不静默失败
- [ ] 端到端：Java 源码溢出后回读边界与函数/类对齐；未知语言回退正确
- [ ] spec 02（Spill）同步

> spec 12 §spill-19；[T47](../tickets/T47-spill-ast-aware-slicing.md)。源：aider 48,169★（repomap：先切再解析、解析失败兜底）+ tree-sitter 26,632★ + langchain 144,172★（语言分隔符）。
