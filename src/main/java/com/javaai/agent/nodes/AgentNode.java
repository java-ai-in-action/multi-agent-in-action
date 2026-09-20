package com.javaai.agent.nodes;

import com.javaai.agent.state.AgentState;

import java.util.Map;

/**
 * Agent 节点统一契约。
 *
 * <p>每个节点接收当前状态、返回「状态增量」，由 StateGraph 负责合并。
 * 返回的 key 见 {@link AgentState} 中的常量。
 */
@FunctionalInterface
public interface AgentNode {

    Map<String, Object> apply(AgentState state) throws Exception;
}
