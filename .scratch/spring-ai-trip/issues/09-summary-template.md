# 九段式摘要模板与增量合并

Type: grilling
Status: resolved
Blocked by: 02

## Question

九段式结构化摘要的完整推演：文章只点名了 User Intent（P0）与 Current State——其余七段是什么、各段优先级 P0–P3 怎么排（参照 Claude Code compact prompt、MemGPT memory block 等推演）？摘要的 prompt 怎么写（含"合并更新而非重写"的增量摘要指令）？摘要以 `<system-reminder>` 包裹、插在近期原文之前的注入形态细节？"摘要之上再做摘要"的触发条件与合并算法？摘要本身 token 计入预算的处理？

## Answer

**定案：CC 九段映射 + 增量合并（analysis 草稿）+ 主模型默认可配独立 + 段落降级为指针。**

1. **九段模板定稿**（Claude Code compact 九段映射，蓝本点名的两段对齐 P0）：
   | # | 段落 | 优先级 |
   |---|---|---|
   | 1 | User Intent 用户核心诉求 | **P0** |
   | 2 | Current State 当前工作现场 | **P0** |
   | 3 | Next Step 下一步（附最近对话原文引用防漂移） | **P0** |
   | 4 | Pending Tasks 待办任务 | P1 |
   | 5 | Errors & Fixes 错误与修复（含用户纠偏反馈，权重最高） | P1 |
   | 6 | Key Artifacts 关键产物（路径/签名，附 evidence 指针替代全文） | P1 |
   | 7 | Problem Solving 已解决问题与排障进展 | P2 |
   | 8 | Technical Concepts 关键技术概念与决策 | P2 |
   | 9 | User Messages Log 用户消息清单 | P3（最先降级） |
2. **生成方式**：**增量合并**——已有旧摘要 + 新积累对话 → 逐段合并更新（非重写），摘要质量随会话深度线性提升（忠于蓝本「摘要之上再做摘要」）。Prompt 采用「先想后写」：先让模型把时间序复盘写进 `<analysis>` 草稿、再输出九段 `<summary>`，注入前剥掉 analysis（CC 技巧，提质不占预算）。支持业务自定义追加指令（如「重点关注订单号」）。
3. **模型与容错**：默认复用会话主模型；`buzhou.memory.summary-model` 可配独立便宜模型。摘要调用失败→本轮降级为「只做微压缩 + 滑窗」，不炸会话；连续失败 3 次本会话熔断 auto-compact（CC 先例）。
4. **注入形态**：摘要以 `<system-reminder>` 包裹、作为一条历史消息插在**近期原文之前**（忠于蓝本）。摘要 token 计入动态预算总量（ticket 07 公式）。
5. **段落降级**：段落标题恒留；token 仍不够时按 P3→P0 顺序把段落正文降级为「一句话 gist + evidence 指针」，**不整段删除**；P0 段正文死保。对齐蓝本「信息不丢弃，只降级」。
