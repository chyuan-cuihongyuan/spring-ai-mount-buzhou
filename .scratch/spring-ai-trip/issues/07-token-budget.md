# 动态预算算法与 token 估算器

Type: grilling
Status: resolved
Blocked by: 01

## Question

动态预算的精确设计：文章给出公式（有效窗口 = 窗口 − 输出预留8000 − 安全缓冲3000；历史预算 = 有效窗口 − 固定开销；触发 ⇔ 预估总量 > 有效窗口 × 0.90）。留白待推演：各模型上下文窗口大小从哪获取（配置表？Spring AI 模型元数据？）；token 本地估算器（英文4字符/token、中文2字符/token、JSON上浮15%）的精确规则与校准方式；系统提示词/工具 Schema/当前输入的 token 数在注入前如何实际测量；阈值与预留是否可按 agent 配置。

## Answer

**定案：内置窗口表 + 启发式估算器（SPI 可换精确）+ Schema 缓存现算分离。**

1. **预算公式**（忠于蓝本，阈值与预留均可经四层配置按 agent 调整——见 ticket 05）：
   ```
   有效窗口   = contextWindow − reserveOutput(默认8000) − safetyBuffer(默认3000)
   固定开销   = systemPromptTokens + toolSchemaTokens + currentInputTokens
   历史预算   = 有效窗口 − 固定开销
   预估总量   = 固定开销 + summaryTokens + historyTokens   // 摘要本身计入
   触发压缩   ⇔ 预估总量 > 有效窗口 × threshold(默认0.90)
   ```
2. **窗口大小来源**：core 内置主流模型窗口表（GPT/Claude/通义/DeepSeek/Gemini 等，按模型名前缀匹配）；`buzhou.memory.context-window` 配置覆盖；未知模型保守默认 32K 并首次使用 warn。
3. **token 估算器**：core 内置**字符启发式**（英文 ~4 字符/token、中文 ~2 字符/token、JSON 上浮 15%，忠于蓝本，零依赖），估算误差由 10% 阈值余量兜底；定义 `TokenEstimator` SPI，另设可选扩展模块 `buzhou-tokenizer-jtokkit`（OpenAI cl100k/o200k 精确分词）供校准级场景。
4. **测量时机**：工具 Schema token 数按**工具集内容哈希缓存**（工具集不变即复用，与 MCP 热插拔的差量刷新联动失效）；系统提示词与当前输入**每轮现算**（可能含 Skill 清单、Attachment 等动态注入）。三项 + 摘要 + 历史在**构建注入视图那一刻**（记忆加载路径，与微压缩/摘要同一管线）统一做触发判断。

### 影响面

- 03 模块清单再增补 1 个可选扩展：`buzhou-tokenizer-jtokkit`（模块总数 14 → 15）。
