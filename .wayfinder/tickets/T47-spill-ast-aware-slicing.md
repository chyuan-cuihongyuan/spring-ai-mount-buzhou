---
id: T47
title: spill · AST-aware 切片（JavaParser + 分隔符回退）
type: task
status: closed
assignee: ""
blocked-by: T28
created: 2026-08-14
---

## Question

回读源码时按字节切会斩断函数/类——切片边界如何语义感知？事实源（达标）：Aider（48,169★ repomap：tree-sitter tags→图→PageRank→**token 预算二分 ±15%**→只渲染 lines-of-interest、每行截 100 字符、解析失败 Pygments 兜底；**先切再解析避 32KB cliff**）、tree-sitter 主库（26,632★）、LangChain（144,172★：`RecursiveCharacterTextSplitter.from_language(Language.JAVA)` 声明分隔符序）。JVM 绑定全不达标（JavaParser 6,133★ / java-tree-sitter 138★）→ **工程注记选型**。

## 待定决策（研究推荐已备）

1. **Java 全 AST 用 JavaParser**（纯 JVM 无 native，非达标源、工程注记）；**其他语言走 LangChain 式分隔符表 + 行边界启发式回退**，不追求全语言 AST——采纳。
2. **先切再解析**：不把整文件/残片喂解析器，先按安全边界切段、段内解析、超长行硬截——采纳（避 32KB cliff）。
3. 解析失败回退链：AST → 语言分隔符 → 行边界，永不静默失败——采纳。
4. JavaParser 依赖坐标与 optional 性质（spill 模块新第三方依赖——项目首个非 Spring 系重依赖，需评估）——spec 定。

依据：`docs/research/oss-perfect-tier23.md` §4.5（工作量中，ROI 中，可后置）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §spill-19**（用户常设授权 2026-08-14 ratify、可推翻）。JavaParser（Java 全 AST）+语言分隔符回退；先切再解析避 32KB cliff；永不静默失败。
