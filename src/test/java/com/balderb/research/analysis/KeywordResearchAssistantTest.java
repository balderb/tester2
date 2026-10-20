package com.balderb.research.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.balderb.research.schema.ColumnInfo;
import com.balderb.research.schema.TableInfo;

class KeywordResearchAssistantTest {

    private final KeywordResearchAssistant assistant = new KeywordResearchAssistant();

    private final List<TableInfo> schema = List.of(
            new TableInfo("RESPONDENT", List.of(
                    new ColumnInfo("ID", "INTEGER", true, false),
                    new ColumnInfo("COUNTRY", "VARCHAR", false, true))),
            new TableInfo("SURVEY_RESPONSE", List.of(
                    new ColumnInfo("WAVE", "INTEGER", true, false),
                    new ColumnInfo("LIFE_SATISFACTION", "INTEGER", true, true),
                    new ColumnInfo("INCOME_BRACKET", "VARCHAR", false, true))));

    @Test
    void selectsTablesWhoseColumnsMatchTheQuestion() {
        AnalysisReport report = assistant.answer("Is life satisfaction related to income?", schema);

        assertThat(report.tablesUsed()).containsExactly("SURVEY_RESPONSE");
        assertThat(report.steps()).extracting(ReasoningStep::detail)
                .anyMatch(detail -> detail.contains("LIFE_SATISFACTION, INCOME_BRACKET"));
    }

    @Test
    void writesMatchedColumnsFirstWithTheRightStatistic() {
        String script = assistant.answer("life satisfaction and income", schema).rScript();

        assertThat(script).contains("summary(survey_response$`LIFE_SATISFACTION`)");
        assertThat(script).contains("table(survey_response$`INCOME_BRACKET`, useNA = \"ifany\")");
        assertThat(script.indexOf("LIFE_SATISFACTION")).isLessThan(script.indexOf("`WAVE`"));
    }

    @Test
    void describesEveryTableWhenNothingMatches() {
        AnalysisReport report = assistant.answer("What about the weather?", schema);

        assertThat(report.tablesUsed()).containsExactly("RESPONDENT", "SURVEY_RESPONSE");
        assertThat(report.steps()).extracting(ReasoningStep::title).contains("No matching tables");
    }
}
