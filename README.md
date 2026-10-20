# Research Assistant

A command line tool for social science research. You ask a research question about a CSV file,
and it answers with:

1. the variables (columns) it used,
2. an **R script** with the analysis, which reads the same CSV file, and
3. an explanation of **how it got to that output** (every choice it made).

Right now the "thinking" is a simple keyword-matching placeholder. The goal is to replace it
with an AI agent later, without changing the commands.

## Requirements

- Java 21 or newer
- Nothing else: the Maven wrapper (`mvnw`) downloads Maven for you
- R, to run the generated scripts

## Build and run

```bash
./mvnw verify                                            # compile and run the tests
java -jar target/research-assistant-0.0.1-SNAPSHOT.jar   # start the interactive shell
```

Inside the shell:

```
$> describe                                   # file, number of rows, columns, types, missing values
$> ask "Does trust in government differ by country?"
$> ask "Does trust in government differ by country?" --save analysis.R
$> load path/to/other-file.csv                # switch to another CSV file
$> help
$> exit
```

To run a single command without the interactive shell:

```bash
SPRING_SHELL_INTERACTIVE_ENABLED=false java -jar target/research-assistant-0.0.1-SNAPSHOT.jar describe
```

## Your data

The data is a CSV file with a header row. The app starts with `data/survey.csv`, a small
**made-up** sample. To use another file:

- replace `data/survey.csv`, or
- change `research.data-file` in `src/main/resources/application.properties`, or
- start with `java -Dresearch.data-file=path/to/file.csv -jar target/research-assistant-0.0.1-SNAPSHOT.jar`, or
- use `load path/to/file.csv` while the app runs.

Comma, semicolon and tab separated files are recognised automatically. Files saved by Excel with
Dutch settings (`;` between values, `7,5` as a number) work too; the R script then uses `read.csv2`.

## How the code is organised

| Package | What it does |
|---------|--------------|
| `data` | Reads the CSV file and works out each column's type and missing values (`CsvDatasetProvider`) |
| `analysis` | Turns a question into an `AnalysisReport`: variables used, reasoning steps and R script |
| `shell` | The commands you type (`ResearchCommands`), built with Spring Shell |

Two interfaces keep the parts replaceable:

- `DatasetProvider`: where the data comes from. Add an implementation to read Excel, SPSS or a database.
- `ResearchAssistant`: how a question is answered. The future AI agent will implement this one.

## Tech stack

Spring Boot 4.1, Spring Shell 4.0, Java 21, Maven, Apache Commons CSV, JUnit 5 + AssertJ.
