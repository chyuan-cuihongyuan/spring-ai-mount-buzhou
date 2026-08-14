package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;

public interface ModelCallContext extends HookContext {

    ChatClientRequest request();

    ChatClientResponse response();

    /** 模型调用的终态失败原因（仅 {@code onModelError} 触发时非空）。 */
    Throwable error();

    void replaceRequest(ChatClientRequest newRequest);

    void replaceResponse(ChatClientResponse newResponse);
}
