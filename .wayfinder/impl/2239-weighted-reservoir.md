# impl 2239 — T 会话 T39 Weighted Reservoir Sampler 加权蓄水池采样（spec 6039 / T6277–T6278 / T39）

纵切片：WeightedReservoirSampler（core/policy）——A-Chao
概率替换+种子化确定性。

- 验证：`mvn -pl buzhou-core test -Dtest='WeightedReservoirSamplerTest'` 全绿（MVN_EXIT=0）。
