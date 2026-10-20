package com.balderb.research.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.balderb.research.data.ColumnInfo;
import com.balderb.research.data.Dataset;

class KeywordResearchAssistantTest {

    private final KeywordResearchAssistant assistant = new KeywordResearchAssistant();

    private final Dataset dataset = new Dataset("survey", Path.of("/data/survey.csv"), ',', 8, List.of(
            new ColumnInfo("wave", true, 0),
            new ColumnInfo("country", false, 0),
            new ColumnInfo("life_satisfaction", true, 1),
            new ColumnInfo("income_bracket", false, 0)));

    @Test
    void selectsColumnsThatMatchTheQuestion() {
        AnalysisReport report = assistant.answer("Is life satisfaction related to income?", dataset);

        assertThat(report.variablesUsed()).containsExactly("life_satisfaction", "income_bracket");
        assertThat(report.steps()).extracting(ReasoningStep::title).contains("Missing values in life_satisfaction");
    }

    @Test
    void writesAScriptThatReadsTheCsvFile() {
        String script = assistant.answer("life satisfaction and income", dataset).rScript();

        assertThat(script).contains(
                "survey <- read.csv(\"/data/survey.csv\", check.names = FALSE, na.strings = c(\"NA\", \"\"), "
                        + "fileEncoding = \"UTF-8-BOM\")");
        assertThat(script).contains("summary(survey$`life_satisfaction`)");
        assertThat(script).contains("table(survey$`income_bracket`, useNA = \"ifany\")");
        assertThat(script).doesNotContain("`wave`");
    }

    @Test
    void usesReadCsv2ForSemicolonFiles() {
        Dataset dutch = new Dataset("enquete", Path.of("/data/enquete.csv"), ';', 2,
                List.of(new ColumnInfo("leeftijd", true, 0)));

        assertThat(assistant.answer("leeftijd", dutch).rScript()).contains("enquete <- read.csv2(");
    }

    @Test
    void describesEveryColumnWhenNothingMatches() {
        AnalysisReport report = assistant.answer("What about the weather?", dataset);

        assertThat(report.variablesUsed()).hasSize(4);
        assertThat(report.steps()).extracting(ReasoningStep::title).contains("No matching variables");
    }
}
