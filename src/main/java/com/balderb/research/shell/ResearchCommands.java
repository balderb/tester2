package com.balderb.research.shell;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.shell.core.command.annotation.Argument;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

import com.balderb.research.analysis.AnalysisReport;
import com.balderb.research.analysis.ResearchAssistant;
import com.balderb.research.analysis.ReasoningStep;
import com.balderb.research.schema.ColumnInfo;
import com.balderb.research.schema.SchemaService;
import com.balderb.research.schema.TableInfo;

/**
 * The commands you can type in the shell. Each method annotated with {@link Command} is one command.
 */
@Component
public class ResearchCommands {

    private final SchemaService schemaService;
    private final ResearchAssistant researchAssistant;

    public ResearchCommands(SchemaService schemaService, ResearchAssistant researchAssistant) {
        this.schemaService = schemaService;
        this.researchAssistant = researchAssistant;
    }

    @Command(name = "tables", description = "List the tables in the database", group = "Research")
    public String tables() {
        List<TableInfo> tables = schemaService.listTables();
        if (tables.isEmpty()) {
            return "The database has no tables.";
        }
        return tables.stream()
                .map(table -> "%-25s %d columns".formatted(table.name(), table.columns().size()))
                .collect(Collectors.joining("\n"));
    }

    @Command(name = "describe", description = "Show the columns of a table", group = "Research")
    public String describe(@Argument(index = 0, description = "table name") String tableName) {
        return schemaService.findTable(tableName)
                .map(ResearchCommands::formatColumns)
                .orElse("Unknown table: " + tableName + ". Use 'tables' to see the available tables.");
    }

    @Command(name = "ask", description = "Answer a research question with an R script and an explanation",
            group = "Research")
    public String ask(@Argument(index = 0, description = "research question, in quotes") String question,
            @Option(longName = "save", description = "file to write the R script to") String savePath)
            throws IOException {
        AnalysisReport report = researchAssistant.answer(question, schemaService.listTables());
        String output = format(report);
        if (savePath != null && !savePath.isBlank()) {
            Path path = Path.of(savePath);
            Files.writeString(path, report.rScript());
            output += "\nR script saved to " + path.toAbsolutePath();
        }
        return output;
    }

    private static String formatColumns(TableInfo table) {
        StringBuilder out = new StringBuilder(table.name()).append("\n");
        for (ColumnInfo column : table.columns()) {
            out.append("  %-25s %-20s %s%n".formatted(column.name(), column.typeName(),
                    column.nullable() ? "may be missing" : "required"));
        }
        return out.toString();
    }

    private static String format(AnalysisReport report) {
        StringBuilder out = new StringBuilder();
        out.append("Question: ").append(report.question()).append("\n");
        out.append("Tables used: ").append(String.join(", ", report.tablesUsed())).append("\n\n");
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
