package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HarnessToolCallingManager implements ToolCallingManager {

    private final DefaultToolCallingManager delegate;
    private final ExecutorService executor;
    private final Semaphore turnPermits;
    private final Duration toolTimeout;
    private final Map<String, String> serialGroups;
    private final ConcurrentHashMap<String, Object> groupLocks = new ConcurrentHashMap<>();
    private final List<Future<?>> inFlight = new CopyOnWriteArrayList<>();

    public HarnessToolCallingManager(DefaultToolCallingManager delegate,
                                     ExecutorService executor,
                                     int maxConcurrencyPerTurn,
                                     Duration toolTimeout,
                                     Map<String, String> serialGroups) {
        this.delegate = delegate;
        this.executor = executor;
        this.turnPermits = new Semaphore(maxConcurrencyPerTurn);
        this.toolTimeout = toolTimeout;
        this.serialGroups = serialGroups == null ? Map.of() : serialGroups;
    }

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions toolCallingChatOptions) {
        return delegate.resolveToolDefinitions(toolCallingChatOptions);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
        List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
        if (toolCalls.isEmpty()) {
            return delegate.executeToolCalls(prompt, chatResponse);
        }

        ToolCallingChatOptions options = prompt.getOptions() instanceof ToolCallingChatOptions t
                ? t : ToolCallingChatOptions.builder().build();
        Map<String, ToolCallback> callbacksByName = new java.util.HashMap<>();
        for (ToolCallback callback : options.getToolCallbacks()) {
            callbacksByName.put(callback.getToolDefinition().name(), callback);
        }

        Map<String, Object> toolContextMap = options.getToolContext() == null
                ? Map.of() : options.getToolContext();
        ToolContext toolContext = new ToolContext(toolContextMap);
        List<Future<ToolResponseMessage.ToolResponse>> futures = new ArrayList<>();
        List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
        boolean returnDirect = false;
        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            futures.add(executor.submit(() -> executeOne(toolCall, callbacksByName, toolContext)));
        }
        for (Future<ToolResponseMessage.ToolResponse> future : futures) {
            try {
                ToolResponseMessage.ToolResponse response = future.get();
                responses.add(response);
                returnDirect |= isReturnDirect(callbacksByName.get(response.name()));
            } catch (Exception e) {
                throw new IllegalStateException("Tool execution aggregation failed", e);
            }
        }

        List<Message> conversationHistory = new ArrayList<>(prompt.getInstructions());
        conversationHistory.add(assistantMessage);
        conversationHistory.add(ToolResponseMessage.builder().responses(responses).build());
        return ToolExecutionResult.builder()
                .conversationHistory(conversationHistory)
                .returnDirect(returnDirect)
                .build();
    }

    public void cancelInFlight() {
        inFlight.forEach(future -> future.cancel(true));
    }

    private ToolResponseMessage.ToolResponse executeOne(
            AssistantMessage.ToolCall toolCall,
            Map<String, ToolCallback> callbacksByName,
            ToolContext toolContext) {
        ToolCallback callback = callbacksByName.get(toolCall.name());
        if (callback == null) {
            return new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(),
                    "未知工具：" + toolCall.name());
        }
        Object lock = groupLock(toolCall.name());
        synchronized (lock) {
            String result;
            try {
                turnPermits.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(),
                        "执行被中断");
            }
            Future<String> task = executor.submit(() -> callback.call(toolCall.arguments(), toolContext));
            inFlight.add(task);
            try {
                result = task.get(toolTimeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.CancellationException e) {
                result = "执行已取消";
            } catch (TimeoutException e) {
                task.cancel(true);
                result = "执行超时（" + toolTimeout.toSeconds() + "s）";
            } catch (Exception e) {
                result = "执行失败：" + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
            } finally {
                inFlight.remove(task);
                turnPermits.release();
            }
            return new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), result);
        }
    }

    private Object groupLock(String toolName) {
        String group = serialGroups.get(toolName);
        return group == null ? new Object() : groupLocks.computeIfAbsent(group, k -> new Object());
    }

    private boolean isReturnDirect(ToolCallback callback) {
        return callback != null && callback.getToolMetadata() != null
                && callback.getToolMetadata().returnDirect();
    }
}
