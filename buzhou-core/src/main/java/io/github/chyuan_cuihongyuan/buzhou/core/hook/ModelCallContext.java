package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;

public interface ModelCallContext extends HookContext {

    ChatClientRequest request();

    ChatClientResponse response();

    void replaceRequest(ChatClientRequest newRequest);

    void replaceResponse(ChatClientResponse newResponse);
}
