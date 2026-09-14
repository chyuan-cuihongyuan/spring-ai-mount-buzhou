package io.github.chyuan_cuihongyuan.buzhou.spill;
/** spec 1529 / T2309：长内容参数对——原参名与溢写路径参数的成对映射（拦截/回读的改写依据）。 */

public record LongContentParamPair(String contentParam, String pathParam) {
}
