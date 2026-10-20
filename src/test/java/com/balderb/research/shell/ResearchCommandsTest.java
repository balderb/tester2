package com.balderb.research.shell;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.balderb.research.analysis.KeywordResearchAssistant;
import com.balderb.research.data.ColumnInfo;
import com.balderb.research.data.Dataset;
import com.balderb.research.data.DatasetProvider;

class ResearchCommandsTest {

    @TempDir
    Path folder;

    private final Dataset dataset = new Dataset("survey", Path.of("survey.csv"), ',', 2,
            List.of(new ColumnInfo("age", true, 0)));

    private final DatasetProvider provider = new DatasetProvider() {
        @Override
        public Dataset current() {
            return dataset;
        }

        @Override
        public Dataset load(Path file) {
            throw new IllegalArgumentException("CSV file not found: " + file);
        }
    };

    private final ResearchCommands commands = new ResearchCommands(provider, new KeywordResearchAssistant());

    @Test
    void asksForAQuestionWhenNoneIsGiven() {
        assertThat(commands.ask(null, null)).startsWith("Please give a research question");
        assertThat(commands.ask("  ", null)).startsWith("Please give a research question");
    }

    @Test
    void savesTheRScript() {
        Path script = folder.resolve("age.R");

        assertThat(commands.ask("age", script.toString())).contains("R script saved to " + script);
        assertThat(script).content().contains("summary(survey$`age`)");
    }

    @Test
    void explainsWhenTheScriptCannotBeSaved() {
        String output = commands.ask("age", folder.resolve("missing-folder/age.R").toString());

        assertThat(output).contains("Could not save the R script").contains("does the folder exist?");
    }

    @Test
    void showsTheErrorWhenAFileCannotBeLoaded() {
        assertThat(commands.load("nope.csv")).isEqualTo("CSV file not found: nope.csv");
    }
}
