package com.javaai.agent.api;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.javaai.agent.graph.CustomerServiceGraph;
import com.javaai.agent.state.AgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 演示入口：POST /api/chat
 *
 * <pre>
 * {"sessionId":"demo","message":"我的订单到哪了，顺便帮我改下收货地址"}
 * </pre>
 *
 * <p>注意 {@code threadId} 用 sessionId：<b>同一会话共享同一份状态快照</b>，
 * 因而进程重启后也能从断点续跑（见 application.yml 的 checkpoint 配置）。
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final CompiledGraph<AgentState> graph;

    public ChatController(CustomerServiceGraph graphBuilder) throws Exception {
        this.graph = graphBuilder.build();
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody ChatRequest req) {
        AgentState input = new AgentState();
        input.setCurrentMessages(List.of(req.message()));

        RunnableConfig config = RunnableConfig.builder()
                .threadId("session-" + req.sessionId())
                .build();

        AgentState last = null;
        for (NodeOutput<AgentState> output : graph.stream(Map.of(), config)) {
            log.info("节点[{}]完成，重试次数={}", output.node(), output.state().getRetryCount());
            last = output.state();
        }

        return Map.of(
                "sessionId", req.sessionId(),
                "answer", last == null ? "" : last.getFinalAnswer(),
                "retryCount", last == null ? 0 : last.getRetryCount());
    }

    public record ChatRequest(String sessionId, String message) {
    }
}
