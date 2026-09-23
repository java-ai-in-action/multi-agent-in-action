package com.javaai.agent.api;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.javaai.agent.graph.CustomerServiceGraph;
import com.javaai.agent.state.WorkflowKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * 演示入口：POST /api/chat
 *
 * <pre>
 * {"sessionId":"demo","message":"我的订单到哪了，顺便帮我改下收货地址"}
 * </pre>
 *
 * <p>注意：{@code CompiledGraph} <b>不是泛型类</b>，{@code invoke(...)} 返回
 * {@code Optional<OverAllState>}，最终状态通过「键」读取。
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final CompiledGraph graph;

    public ChatController(CustomerServiceGraph graphBuilder) throws Exception {
        this.graph = graphBuilder.build();
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody ChatRequest req) {
        // threadId 用 sessionId：同一会话共享同一份状态快照
        RunnableConfig config = RunnableConfig.builder()
                .threadId("session-" + req.sessionId())
                .build();

        // 同步执行，返回最终状态（要做流式可改用 graph.stream(...)）
        Optional<OverAllState> result = graph.invoke(
                Map.of(WorkflowKeys.MESSAGE, req.message()), config);

        OverAllState state = result.orElseThrow(
                () -> new IllegalStateException("图执行未返回状态"));

        // 通过「键」读取最终状态
        String answer = state.value(WorkflowKeys.FINAL_ANSWER, "");
        Integer retryCount = state.value(WorkflowKeys.RETRY_COUNT, 0);

        log.info("会话[{}]执行完成，重试次数={}", req.sessionId(), retryCount);

        return Map.of(
                "sessionId", req.sessionId(),
                "answer", answer,
                "retryCount", retryCount);
    }

    public record ChatRequest(String sessionId, String message) {
    }
}
