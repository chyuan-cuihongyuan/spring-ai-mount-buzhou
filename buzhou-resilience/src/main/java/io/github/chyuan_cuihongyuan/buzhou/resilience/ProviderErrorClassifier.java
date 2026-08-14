package io.github.chyuan_cuihongyuan.buzhou.resilience;

import org.springframework.ai.chat.client.ChatClientResponse;

/**
 * 「异常 + 响应元数据 → {@link Classification}」的可扩展分类点（spec「归一化错误分类」）。
 *
 * <p>框架内置 {@link DefaultErrorClassifier}（按异常形态做 provider 无关的启发式归类）；
 * 非标 provider 或需要精细识别（如解析私有错误码、内容过滤元数据）时，实现本接口补映射。
 *
 * <p>分类须是纯函数：同一输入恒定输出同一类别，不持状态、不抛异常（无法识别时返回
 * {@link ErrorCategory#UNKNOWN}）。
 *
 * @param error    模型调用抛出的异常；内容拒绝（静默通道）时为 {@code null}
 * @param response 模型返回的响应元数据；异常路径下可能为 {@code null}，内容拒绝路径下非空
 */
@FunctionalInterface
public interface ProviderErrorClassifier {

    Classification classify(Throwable error, ChatClientResponse response);
}
