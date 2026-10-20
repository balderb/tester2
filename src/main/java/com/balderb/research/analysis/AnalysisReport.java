package com.balderb.research.analysis;

import java.util.List;

/**
 * The answer to a research question: which tables were used, why, and the R script to run.
 *
 * @param question   the research question as the user asked it
 * @param tablesUsed names of the database tables the analysis relies on
 * @param steps      explanation of every decision that led to the script
 * @param rScript    R syntax that produces the tables and analysis
 */
public record AnalysisReport(String question, List<String> tablesUsed, List<ReasoningStep> steps, String rScript) {

    public AnalysisReport {
        tablesUsed = List.copyOf(tablesUsed);
        steps = List.copyOf(steps);
    }
}
