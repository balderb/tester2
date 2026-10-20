package com.balderb.research.analysis;

import java.util.List;

/**
 * The answer to a research question: which variables were used, why, and the R script to run.
 *
 * @param question      the research question as the user asked it
 * @param variablesUsed names of the columns the analysis focuses on
 * @param steps         explanation of every decision that led to the script
 * @param rScript       R syntax that produces the tables and analysis
 */
public record AnalysisReport(String question, List<String> variablesUsed, List<ReasoningStep> steps,
        String rScript) {

    public AnalysisReport {
        variablesUsed = List.copyOf(variablesUsed);
        steps = List.copyOf(steps);
    }
}
