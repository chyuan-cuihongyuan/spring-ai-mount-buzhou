# 九段式摘要模板与增量合并

Type: grilling
Status: open
Blocked by: 02

## Question

九段式结构化摘要的完整推演：文章只点名了 User Intent（P0）与 Current State——其余七段是什么、各段优先级 P0–P3 怎么排（参照 Claude Code compact prompt、MemGPT memory block 等推演）？摘要的 prompt 怎么写（含"合并更新而非重写"的增量摘要指令）？摘要以 `<system-reminder>` 包裹、插在近期原文之前的注入形态细节？"摘要之上再做摘要"的触发条件与合并算法？摘要本身 token 计入预算的处理？
