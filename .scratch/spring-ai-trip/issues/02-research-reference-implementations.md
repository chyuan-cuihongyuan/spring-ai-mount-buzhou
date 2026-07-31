# 参照系与留白推演素材调研

Type: research
Status: resolved
Blocked by: —

## Question

为文章留白处的自主推演收集参照素材。需查清：

1. **Claude Code 的上下文管理**：其四层上下文机制（文章提到借鉴了三条：不断崖截断、结构化摘要、压缩后显式恢复）的公开资料细节——compact 的触发阈值、摘要 prompt 形态、`/compact` 与 auto-compact 行为。
2. **AgentScope Java**：`ToolResultEvictionMiddleware` 的设计文档与实现（阈值、落盘、预览占位符、回读工具）；其 short-term memory / compaction 思路；eviction（宽度）与 compaction（深度）的分层论述。
3. **LangChain4j**：`ChatMemory`/`ChatMemoryStore`、token 估算（`Tokenizer`）、AiServices 的记忆注入形态。
4. **大厂公开文章**：携程（除蓝本文外的相关文章）、字节/腾讯/阿里在 Agent 上下文管理、记忆压缩、工具结果治理上的公开实践（如字节 HiAgent/扣子、阿里 Spring AI Alibaba 的 memory 实现、腾讯相关分享）。
5. **九段式摘要的业界近似物**：Claude Code compact prompt、MemGPT/Letta 的 memory block、Cognee/Mem0 的摘要结构——为九段模板各段落与 P0–P3 优先级的推演提供候选结构。

产出：素材笔记（带来源链接），写入 `.scratch/spring-ai-trip/research/reference-implementations.md`，并将摘要与结论追加到本 ticket 的 `## Answer`。

## Answer

核心结论：蓝文机制均有强参照。Claude Code 逆向资料显示三层压缩体系（MicroCompact 清工具结果 / Session Memory / Full Compact），compact prompt 恰为九段结构（意图→概念→文件→错误→待办→现场→下一步），auto-compact 阈值=有效窗口−13K，压缩后按预算重注入 top-5 文件、skills 与 CLAUDE.md。AgentScope Java 明确提出 eviction 治"宽"、compaction 治"深"：单结果 80K 字符落盘+首尾 2K 预览+read_file 回读；摘要默认四段；压缩前 flush 双层长期记忆；token 按 2.5 字符估算。LangChain4j 仅滑窗+Store SPI，压缩层是空白。九段模板可按 Claude Code 九段映射，P0=意图/现场/下一步。完整素材（含全部来源链接）见 research/reference-implementations.md。
