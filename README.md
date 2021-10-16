# MiningAnnotationUsageRules

The artifact repo for the paper titled:

> *Mining Annotation Usage Rules: A Case
Study with MicroProfile*. Batyr Nuryyev, Ajay Kumar Jha, Sarah Nadi, Yee-Kang
Chang, Vijay Sundaresan, Emily Jiang. 

In this paper, we investigate whether
the idea of pattern-based discovery of rules can be applied to annotation-based
API usages in the industrial case study of MicroProfile (an open source Java
microservices framework).

## Contents

#### Tools

- **The miner:** Annotation mining tool that generates candidate rules (located in `/miner`
  directory).
- **The scanner/checkers:** Confirmed mined rules encoded into a program that leverages JavaParser to find violations of the rules (located in `/checkers` directory). Note that the static checkers encode only MicroProfile and Spring Boot rules (i.e., usage rules for any other library must be encoded/added manually separately).

#### Input data

- **Manually extracted usage rules:** 15 usage rules manually extracted from documentation and developers' forums are [here](./artifacts/manually-extracted-rules.xlsx).
- **MicroProfile client projects:** The list of projects used for mining and scanning for violations is available [here](https://github.com/ualberta-smr/MiningAnnotationUsageRules/blob/main/miner/clientProjects_MicroProfile.txt) (same list of projects for both tasks).
- **Spring Boot client projects:** The list of projects used for mining is available [here](https://github.com/ualberta-smr/MiningAnnotationUsageRules/blob/main/miner/clientProjects_mining_SpringBoot.txt). The list of projects used for scanning for violations is available [here](https://github.com/ualberta-smr/MiningAnnotationUsageRules/blob/main/miner/clientProjects_scanning_SpringBoot.txt).

#### Output data (results)

- TODO: Mined rules (MP + SB)
- TODO: Unique confirmed rules (MP + SB)
- **Detected violations:** Violations in the commit history of [MicroProfile](./artifacts/MicroProfile_ClientProjectsViolations.csv) client projects, as well as in the latest commit of [Spring Boot](./artifacts/SpringBoot_ClientProjectsViolations.csv) projects.
- **Spring Boot violations in Stack Overflow:** Manifestations of violations of 9 Spring Boot rules are [here](./artifacts/spring-boot-questions-on-so.txt). **Note** that we discuss the manifestations of violations in the thesis only (which is more expansive). Ignore this artifact if you come here from our article/paper.

## Reproducing Results

### Mining candidate usage rules

Note that the output results may slightly vary due to proprietary projects
being removed. The steps below describe how to mine API usages only for 1
library at a time (though you could combine different configs/settings and
input client projects together and be able to mine several libraries at a
time).

Steps:

1. **Clone the repo:** download this repository.
2. **Fetch client projects to mine from**: In another directory elsewhere (e.g., `X/Y/Z/projs`), clone all the
   [MicroProfile](./miner/clientProjects_MicroProfile.txt) and [Spring
   Boot](./miner/clientProjects_mining_SpringBoot.txt) client projects.
3. **Configure the settings:** Set correct project paths in
   [Configuration.java](./miner/src/main/java/miner/Configuration.java). If you
   decide to run Spring Boot, make sure to select
   Spring-Boot-specific configs and comment out the MicroProfile ones (or vice
   versa).
4. **Compile:** Build and package a fat jar: `mvn clean compile package`.
5. **Run:** Run the fat jar: `java -jar target/annotation-parser-1.0-SNAPSHOT.jar`.


### Scanning for violations in client projects

The static analysis tool that encodes all rules scans one program at a time.
The rules that we mined are already encoded
[here](./checkers/src/main/java/parser/rules).

Note that the output results may slightly vary due to proprietary projects being removed.
Also, we use different (disjoint) sets of Spring Boot projects for [scanning](./miner/clientProjects_scanning_SpringBoot.txt) and [mining](./miner/clientProjects_mining_SpringBoot.txt),
respectively.

Steps (skip Step 1 if you have already cloned this repository):

1. **Clone the repo:** download this repository.
2. **Compile:** Go into `/checkers` directory: 

```bash
cd checkers/
```

Create the static analysis checker fat jar:

```bash
mvn clean compile package
```

4. **Run:** Run checkers on the desired project to scan for violations:

```bash
java -jar target/rules-checker-1.0-SNAPSHOT-jar-with-dependencies.jar path/to/scan/
```

## General Usage

If you would like to use the miner to mine usages for a library of your choice
(i.e., anything other than MicroProfile or Spring Boot), do the following
steps:

1. Find client projects that use your library. Clone all of them into some
   directory.  Then, specify the path to that directory
   [here](https://github.com/ualberta-smr/MiningAnnotationUsageRules/blob/6affc29cb05e8d0e4dde3d32e363e9e2693e6f87/miner/src/main/java/miner/Configuration.java#L19).
2. Specify the correct package (API) prefix for your library
   [here](https://github.com/ualberta-smr/MiningAnnotationUsageRules/blob/6affc29cb05e8d0e4dde3d32e363e9e2693e6f87/miner/src/main/java/miner/Configuration.java#L27-L28).
3. Find your library source code. This is needed to resolve fully-qualified
   names of types provided by the library. Specify the path to the library
   sources
   [here](https://github.com/ualberta-smr/MiningAnnotationUsageRules/blob/6affc29cb05e8d0e4dde3d32e363e9e2693e6f87/miner/src/main/java/miner/Configuration.java#L32)
   (should be stored in one directory).
4. Choose what sub-APIs (sub-packages) you would like to focus on. You may want to ignore some sub-packages depending on your goal (e.g., some sub-package is soon to be deprecated). Specify the sub-packages [here](https://github.com/ualberta-smr/MiningAnnotationUsageRules/blob/6affc29cb05e8d0e4dde3d32e363e9e2693e6f87/miner/src/main/java/miner/Configuration.java#L65).
5. Specify your library sub-package API prefix [here](https://github.com/ualberta-smr/MiningAnnotationUsageRules/blob/6affc29cb05e8d0e4dde3d32e363e9e2693e6f87/miner/src/main/java/miner/Configuration.java#L103), e.g., `org.eclipse.microprofile` is the top-level package that provides entire MicroProfile API.

**Note** that the usage violation scanner is irrelevant because only
MicroProfile and Spring Boot rules are hardcoded, i.e., we do not encode (or
provide tools that can easily encode) other usage rules of other libraries.
However, if you are familiar with JavaParser, you can still clone the scanner
and adapt the rules to a library of your interest. It is probably easier to
start your own JavaParser project and encode the rules there from scratch.

## Struggling with some steps?

File an issue on this repo and we will get back to you asap.

## FAQ

- **Can I mine from client projects that use multiple libraries together?**

Yes. You will need to combine all client projects of the libraries in the same
directory and then combine configuration of all these libraries
[here](https://github.com/ualberta-smr/MiningAnnotationUsageRules/blob/main/miner/src/main/java/miner/Configuration.java).


## Contributors (artifacts)

Batyr Nuryyev and Ajay Kumar Jha.
