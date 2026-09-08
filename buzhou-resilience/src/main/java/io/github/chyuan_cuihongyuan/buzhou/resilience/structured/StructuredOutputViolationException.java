package io.github.chyuan_cuihongyuan.buzhou.resilience.structured;

import java.util.List;

/**
 * 结构化输出契约违规终态（spec 402 / T695）：修复耗尽后上抛——错误清单
 * 随异常可见（外层 hook/观测按模型调用错误口径感知）。
 */
public class StructuredOutputViolationException extends RuntimeException {

    private final transient List<String> errors;

    public StructuredOutputViolationException(List<String> errors) {
        super("模型输出不符合结构化契约（修复耗尽）：" + errors);
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }
}
