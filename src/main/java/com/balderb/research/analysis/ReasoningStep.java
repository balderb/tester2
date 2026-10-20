package com.balderb.research.analysis;

/**
 * One step in the explanation of how an analysis was put together.
 * Together the steps form the "how did you get to this output" part of a report.
 */
public record ReasoningStep(String title, String detail) {
}
