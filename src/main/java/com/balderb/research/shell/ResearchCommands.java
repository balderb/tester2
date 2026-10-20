package com.balderb.research.shell;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.shell.core.command.annotation.Argument;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

import com.balderb.research.analysis.AnalysisReport;
import com.balderb.research.analysis.ReasoningStep;
import com.balderb.research.analysis.ResearchAssistant;
import com.balderb.research.data.ColumnInfo;
import com.balderb.research.data.Dataset;
import com.balderb.research.data.DatasetProvider;

/**
 * The commands you can type in the shell. Each method annotated with {@link Command} is one command.
 */
@Component
public class ResearchCommands {

    private final DatasetProvider datasetProvider;
    private final ResearchAssistant researchAssistant;

    public ResearchCommands(DatasetProvider datasetProvider, ResearchAssistant researchAssistant) {
        this.datasetProvider = datasetProvider;
        this.researchAssistant = researchAssistant;
    }

    @Command(name = "load", description = "Switch to another CSV file", group = "Research")
    public String load(@Argument(index = 0, description = "path to the CSV file") String file) {
        try {
            return "Loaded.\n\n" + formatDataset(datasetProvider.load(Path.of(file)));
        }
        catch (IllegalArgumentException | UncheckedIOException e) {
            return e.getMessage();
        }
    }

    @Command(name = "describe", description = "Show the loaded file and its columns", group = "Research")
    public String describe() {
        try {
            return formatDataset(datasetProvider.current());
        }
        catch (IllegalStateException e) {
            return e.getMessage();
        }
    }

    @Command(name = "ask", description = "Answer a research question with an R script and an explanation",
            group = "Research")
    public String ask(@Argument(index = 0, description = "research question, in quotes") String question,
            @Option(longName = "save", description = "file to write the R script to") String savePath) {
        if (question == null || question.isBlank()) {
            return "Please give a research question, for example: ask \"Does trust in government differ by country?\"";
        }
        Dataset dataset;
        try {
            dataset = datasetProvider.current();
        }
        catch (IllegalStateException e) {
            return e.getMessage();
        }
        AnalysisReport report = researchAssistant.answer(question, dataset);
        String output = format(report);
        if (savePath != null && !savePath.isBlank()) {
            Path path = Path.of(savePath).toAbsolutePath();
            try {
                Files.writeString(path, report.rScript());
                output += "\nR script saved to " + path;
            }
            catch (IOException e) {
                output += "\nCould not save the R script to " + path + " (does the folder exist?)";
            }
        }
        return output;
    }

    private static String formatDataset(Dataset dataset) {
        StringBuilder out = new StringBuilder();
        out.append("File: ").append(dataset.file()).append("\n");
        out.append("Rows: ").append(dataset.rowCount()).append("\n\n");
        out.append("  %-25s %-8s %s%n".formatted("COLUMN", "TYPE", "MISSING"));
        for (ColumnInfo column : dataset.columns()) {
            out.append("  %-25s %-8s %d%n".formatted(column.name(), column.numeric() ? "number" : "text",
                    column.missingCount()));
        }
        return out.toString();
    }

    private static String format(AnalysisReport report) {
        StringBuilder out = new StringBuilder();
        out.append("Question: ").append(report.question()).append("\n");
        out.append("Variables used: ").append(String.join(", ", report.variablesUsed())).append("\n\n");
        out.append("How I got to this output:\n");
        int number = 1;
        for (ReasoningStep step : report.steps()) {
            out.append("  ").append(number++).append(". ").append(step.title()).append(": ")
                    .append(step.detail()).append("\n");
        }
        out.append("\nR script:\n").append(report.rScript());
        return out.toString();
    }
}
