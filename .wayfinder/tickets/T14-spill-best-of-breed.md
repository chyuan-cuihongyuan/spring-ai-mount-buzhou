---
id: T14
title: spill 对标开源最优——hot-tail/自描述 stub/语义回读 等 best-of-breed 落地
type: task
status: open
assignee: ""
blocked-by: [T11]
created: 2026-08-13
---

## Question

把 [T11](T11-oss-best-ideas-core-memory-spill-guard.md) / `docs/research/oss-best-of-breed.md` §3 §5 里 **spill** 的 best-of-breed 思想择优写入，把「已 SOTA 的回读广度/非对称/完整性」补齐为端到端 best-in-class。落地项：

### Tier 1（先做）
- **hot-tail / cold-storage 两级**（Claude Code microcompaction）：近期 N 条全量内联、其余溢出至 evidence store；"keep inline" 数/大小按 session 可调。
- **per-tool durable override**（Claude Code `_meta["anthropic/maxResultSizeChars"]`）：工具/hook 可声明「永不溢出」或「超 X 才溢出」。
- **自描述 stub**（arXiv pointer 论文 + MCP widget）：stub 一律含 handle + 形状/schema 提示 + 字节/token 大小 + 精确回读动词与参数（裸路径表现最差）。
- **token-aware 可配阈值**（Codex 反面教材）：阈值可配且按 token 计（非硬编码行数），per-tool override。

### Tier 2
- **语义回读（第 4 模式）**（MemGPT archival / OpenAI file search）：溢出时 embed、回读时按语义查。
- **AST-aware 切片**（Aider repo map / LangChain 代码感知 splitter）：源码按 AST 节点切，**先切再解析**避 32KB cliff；非代码回退 line/byte。
- head+tail 回读风味（Codex）。
- 显式截断标记 + 回读 handle（永不静默截断）。

### Tier 3
- 结构化 offload（MCP `structuredContent` + 下载 URL + 一次性 token）：模型见预览+句柄、UI 见结构化载荷。
- 内容寻址 handle 校验（强于 MemGPT 精确串匹配）。
- just-in-time 标识纪律 + context-clearing（Anthropic）。

## Context

- **已领先（勿重做）= 当前 SOTA**：byte/jsonpath/pagination 三模回读（survey 最广，JSONPath 基本独一无二）、读写失败非对称（**真原创**）、content-addressed evidence-id（**最严谨完整性保证**）。
- 反模式：硬编码 head+tail 有损截断（静默损坏）；无标记静默截断；「抬高天花板」当策略；临时句柄当完整性保证；巨文件先解析再切片；FIFO+有损摘要逐出工具制品；裸路径 stub；读写无差别对待。
- 详源见 `docs/research/oss-best-of-breed.md` §3 与 §6（Claude Code / Anthropic / MCP / Codex / copilot-cli / arXiv / Aider / LangChain / OpenAI file search）。

## Resolution
<!-- 实现后填 -->

## Resolution（Tier-1 部分，2026-08-13）

Tier-1 四项全部落地并闭合于细化票：[自描述 stub + token-aware 阈值](T20-spill-self-describing-handle.md)、[hot-tail/cold-storage 两级](T21-spill-hot-tail-cold-storage.md)、[per-tool durable override](T22-spill-per-tool-durable-override.md)（显式截断标记随 T20 占位符内建）。Tier-2（语义回读/AST 切片/head+tail 风味/结构化 offload/内容寻址校验）与 Tier-3 继续由本票追踪。
