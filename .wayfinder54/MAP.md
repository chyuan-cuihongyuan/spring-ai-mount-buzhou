# Wayfinder Map — Buzhou A/B run 指纹（effort #54，50 轮自迭代第 19 轮）

> effort #54，延续 #53（T349–T350 / impl-239）。主线：**spec 82 fog 项「ab.run
> 记录同字段是零成本扩展」**——A/B 结论的适用数据集版本不可验（改了数据集后旧
> A/B 结论与新结论不可比）。

## Destination

compare() 计算指纹入 PairwiseEvalResult（新 7 参 record + 6 参旧构造兼容）；
落盘 map 携带 datasetFingerprint（null 不写键——旧记录诚实无字段）；abRuns 摘要行
与 abRun 明细回读均携带；零新类型（字段级扩展）、零新键。

## Notes

- 借鉴：spec 82 同语义复用（EvalDatasetStore.fingerprint 内容寻址）。

## Decisions so far

- null 不写键（旧记录/空集场景解码 get→null 自然兼容——不写哨兵值）。

## Not yet specified

- AB run JSONL 导出（spec 88 同构扩展）；A/B 对比的 diff 面（两个 abRun 对齐
  itemId 裁决迁移——需求证据后议）。

## Out of scope

- 沿用 #7–#53；指纹寻址存储。

## Tickets

- [x] [T351 指纹入档 + 双查询面携带 + 旧构造兼容](tickets/T353-ab-fingerprint.md)（impl-240）
- [x] [T352 红队（内存/明细/摘要三面等值）+ 收口](tickets/T354-ab-fingerprint-close.md)
