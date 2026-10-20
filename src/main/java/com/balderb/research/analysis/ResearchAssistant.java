package com.balderb.research.analysis;

import com.balderb.research.data.Dataset;

/**
 * Turns a research question into an analysis plan.
 *
 * <p>This is the extension point for the future AI agent: a new implementation (for example one
 * that calls a large language model) can replace {@link KeywordResearchAssistant} without changing
 * the shell commands.
 */
public interface ResearchAssistant {

    AnalysisReport answer(String question, Dataset dataset);
}
