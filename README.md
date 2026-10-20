# Research Assistant

A command line tool for social science research. You ask a research question about a database,
and it answers with:

1. the tables it used,
2. an **R script** with the analysis, and
3. an explanation of **how it got to that output** (every choice it made).

Right now the "thinking" is a simple keyword-matching placeholder. The goal is to replace it
with an AI agent later, without changing the commands.

## Requirements

- Java 21 or newer
- Nothing else: the Maven wrapper (`mvnw`) downloads Maven for you

## Build and run

```bash
./mvnw verify                                       # compile and run the tests
java -jar target/research-assistant-0.0.1-SNAPSHOT.jar   # start the interactive shell
```

Inside the shell:

```
$> tables
$> describe survey_response
$> ask "Is life satisfaction related to income?"
$> ask "Is life satisfaction related to income?" --save analysis.R
$> help
$> exit
```

To run a single command without the interactive shell:

```bash
SPRING_SHELL_INTERACTIVE_ENABLED=false java -jar target/research-assistant-0.0.1-SNAPSHOT.jar tables
```

## Sample data

The app starts with a small, **made-up** dataset in an in-memory H2 database
(`src/main/resources/schema.sql` and `data.sql`), so you can try the commands straight away.
To use your own database, change the `spring.datasource.*` settings in
`src/main/resources/application.properties` and add the JDBC driver for your database to `pom.xml`.

## How the code is organised

| Package | What it does |
|---------|--------------|
| `schema` | Reads which tables and columns exist (`SchemaService`), using JDBC metadata |
| `analysis` | Turns a question into an `AnalysisReport`: tables used, reasoning steps and R script |
| `shell` | The commands you type (`ResearchCommands`), built with Spring Shell |

`ResearchAssistant` is the interface the future AI agent will implement.
`KeywordResearchAssistant` is the current placeholder implementation.

## Tech stack

Spring Boot 4.1, Spring Shell 4.0, Java 21, Maven, H2 (demo database), JUnit 5 + AssertJ.
