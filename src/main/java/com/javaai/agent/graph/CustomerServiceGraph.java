package com.javaai.agent.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.javaai.agent.nodes.ExecutorNode;
import com.javaai.agent.nodes.PlannerNode;
import com.javaai.agent.nodes.ReviewerNode;
import com.javaai.agent.nodes.SummarizerNode;
import com.javaai.agent.state.WorkflowKeys;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 客服系统的 Multi-Agent 编排核心。
 *
 * <pre>
 *  START → Planner → Executor ─┬─(通过)──→ Summarizer → END
 *                              └─(不通过)→ Reviewer ─┬─(retry&lt;3)→ Executor
 *                                                   └─(retry≥3)→ Summarizer
 * </pre>
 *
 * <p><b>⚠️ 注意</b>：{@code StateGraph} / {@code CompiledGraph} <b>都不是泛型类</b>，
 * 状态由 {@code OverAllState}（Map 包装）承载。所以这里是 {@code StateGraph} 而不是
 * {@code StateGraph<AgentState>}。
 */
@Component
public class CustomerServiceGraph {

    /** 重试硬上限：防止 Reviewer 反复打回、无限烧钱 */
    private static final int MAX_RETRY = 3;

    private final PlannerNode planner;
    private final ExecutorNode executor;
    private final ReviewerNode reviewer;
    private final SummarizerNode summarizer;

    public CustomerServiceGraph(PlannerNode planner, ExecutorNode executor,
                                ReviewerNode reviewer, SummarizerNode summarizer) {
        this.planner = planner;
        this.executor = executor;
        this.reviewer = reviewer;
        this.summarizer = summarizer;
    }

    public CompiledGraph build() throws GraphStateException {
        // 1. 建图：状态键策略由 KeyStrategyFactory 提供
        StateGraph graph = new StateGraph(WorkflowKeys.keyStrategyFactory());

        // 2. 注册 4 个节点（node_async 把同步 NodeAction 包成异步节点）
        graph.addNode("planner", node_async(planner));
        graph.addNode("executor", node_async(executor));
        graph.addNode("reviewer", node_async(reviewer));
        graph.addNode("summarizer", node_async(summarizer));

        // 3. 开始 → Planner → Executor
        graph.addEdge(START, "planner");
        graph.addEdge("planner", "executor");

        // 4. 条件边：审核通过 → 汇总；不通过 → 交 Reviewer
        graph.addConditionalEdges("executor",
                edge_async(state -> state.value(WorkflowKeys.REVIEW_PASSED, false)
                        ? "summarizer" : "reviewer"),
                Map.of("summarizer", "summarizer", "reviewer", "reviewer"));

        // 5. 条件边：打回重做（最多 MAX_RETRY 次，超了直接汇总，防止死循环）
        graph.addConditionalEdges("reviewer",
                edge_async(state -> state.value(WorkflowKeys.RETRY_COUNT, 0) < MAX_RETRY
                        ? "executor" : "summarizer"),
                Map.of("executor", "executor", "summarizer", "summarizer"));

        graph.addEdge("summarizer", END);

        return graph.compile();
    }
}
