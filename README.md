# MiningAnnotationUsageRules

This repo contains two tools:

- Annotation mining tool that generates candidate rules (located in `/miner` directory),
- Static checkers (located in `/checkers` directory)

As well as results (i.e., generated and confirmed candidate rules for both MicroProfile and Spring Boot) placed in `/results` directory.

#### Artifacts

- 15 explicit and implicit usage rules extracted from questions are
  [here](./artifacts/manually-extracted-rules.xlsx).
- Manifestations of violations of 9 Spring Boot rules are [here](./artifacts/spring-boot-questions-on-so.txt).

## Reproducing Results

### Mining candidate usage rules

Note that the output results may slightly vary due to proprietary projects
being removed.

Steps:

1. Clone this repository.
2. Open IntelliJ project (Open -> Choose `/miner` as directory).
3. In another directory elsewhere (e.g., `X/Y/Z/projs`), clone all the
   [MicroProfile](./miner/clientProjects_MicroProfile.txt) and [Spring
   Boot](./miner/clientProjects_mining_SpringBoot.txt) client projects.
4. Set correct project paths in
   [Configuration.java](./miner/src/main/java/miner/Configuration.java). If you
   decide to run Spring Boot (or MicroProfile), make sure to select
   MicroProfile-specific configs and comment out the Spring Boot ones (or vice
   versa).
5. Run (press green left arrow on the class definition of `Runner` in
   [Runner.java](./miner/src/main/java/parser/Runner.java).

### Scanning for violations in client projects

The static analysis tool that encodes all rules scans one program at a time
(i.e., no paths of projects to scan). The rules that we mined are already encoded
[here](./checkers/src/main/java/parser/rules).

Note that the output results may slightly vary due to proprietary projects being removed.

Steps (might need to skip some steps if you have already went through steps for
**Mining**):

1. Clone this repository.
2. Go into `/checkers` directory: 

```bash
cd checkers/
```

3. Create the static analysis checker jar:

```bash
mvn clean compile package
```

4. Run checkers on the desired project to scan for violations:

```bash
java -jar target/rules-checker-1.0-SNAPSHOT-jar-with-dependencies.jar path/to/scan
```

## Struggling with some steps?

File an issue on this repo and we will get back to you asap.
