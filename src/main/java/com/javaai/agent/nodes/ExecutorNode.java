package com.javaai.agent.nodes;

import com.javaai.agent.state.AgentState;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Executor Agent：调用工具执行子任务。
 *
 * <p><b>踩坑点②</b>：子任务必须能<b>并行</b>执行。串行跑纯属浪费 Token 和时间——
 * 用户问「我的订单到哪了，顺便帮我改下收货地址」是两个独立任务，没理由排队。
 */
@Component
public class ExecutorNode implements AgentNode {

    private final ChatClient chatClient;

    /** 虚拟线程池：IO 密集的 LLM 调用用它最省资源（JDK 21+） */
    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

    public ExecutorNode(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        你是电商客服的执行助手。根据子任务调用对应工具（订单查询、地址修改、退款等），
                        只输出执行结果本身，不要客套话，不要编造数据。
                        """)
                .build();
    }

    @Override
    public Map<String, Object> apply(AgentState state) throws Exception {
        Map<String, String> results = new LinkedHashMap<>();

        // 每个子任务并行执行
        var futures = state.getTasks().stream()
                .map(task -> CompletableFuture.supplyAsync(() -> Map.entry(task, exec(task)), pool))
                .toList();

        for (var future : futures) {
            var entry = future.join();   // 生产环境请加超时，见踩坑清单第 2 条
            results.put(entry.getKey(), entry.getValue());
        }
        return Map.of(AgentState.RESULTS, results);
    }

    private String exec(String task) {
        return chatClient.prompt()
                .user(u -> u.text("执行子任务：{task}")
                        .param("task", task))
                .call()
                .content();
    }
}
