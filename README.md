# MiningAnnotationUsageRules

The artifact repo for the paper titled:

> *Mining Annotation Usage Rules: A Case Study with MicroProfile*. Batyr Nuryyev, Ajay Kumar Jha, Sarah Nadi, Yee-Kang Chang, Emily Jiang, Vijay Sundaresan. ICSME'22: Industry Track. Status: submitted (in review).

In this paper, we investigate whether
the idea of pattern-based discovery of rules can be applied to annotation-based
API usages in the industrial case study of MicroProfile (an open source Java
microservices framework).

## Contents

#### Tools

- **The miner:** Annotation mining tool that generates candidate rules (located in `/miner`
  directory).
- **The scanner/checkers:** Confirmed mined rules encoded into a program that leverages JavaParser to find violations of the rules (located in `/checkers` directory). Note that the static checkers encode only MicroProfile rules (i.e., usage rules for any other library must be encoded/added manually separately).

#### Input data

- **Manually extracted usage rules:** 12 usage rules manually extracted from documentation and developers' forums are [here](./artifacts/manually-extracted-rules.xlsx).
- **MicroProfile client projects:** The list of open-source projects used for mining and scanning for violations is available [here](./miner/clientProjects_MicroProfile.txt) (same list of projects for both tasks). The list does not include the 81 proprietary projects.

#### Output data (results)

Rules:

- **Mined rules:** Mined rules for [MicroProfile](./results/rules/minedRules_MicroProfile.json) along with edit distance comments.
- **Unique confirmed rules:** Confirmed (unique) rules for [MicroProfile](./results/rules/uniqueMinedAndConfirmedRules_MicroProfile.json).

Violations:

- **Detected violations:** Violations in [MicroProfile](./artifacts/MicroProfile_ClientProjectsViolations.csv) client projects.

## Reproducing Results

### Mining candidate usage rules

Note that the output results may slightly vary due to proprietary projects
being removed. The steps below describe how to mine MicroProfile API usages.

Steps:

1. **Clone the repo:** download this repository.
2. **Fetch client projects to mine from**: In another directory elsewhere (e.g., `X/Y/Z/projs`), clone all the
   [MicroProfile](./miner/clientProjects_MicroProfile.txt) client projects. The txt files contain project URLs as well as commit hashes (for the commits that were latest at the time we cloned and mined/analyzed them). You can use the `clone_projects.py` [script](./utils/clone_projects.py) to clone client projects from GitHub. **Note** that not all projects may be available, so the end result may not be quite the same as the results in the paper. For example, if you want to clone MicroProfile client projects along with checking out commits that were the latest at the time (i.e., the repos may have been updated), run the following:

```bash
python3 utils/clone_projects.py miner/clientProjects_MicroProfile.txt <where-to-clone>
```

Make sure you have `gitpython` installed. See installation instructions at the top of the `clone_projects.py` file.

3. **Configure the settings:** Set correct project paths (should be absolute, not relative) in
   [Configuration.java](./miner/src/main/java/miner/Configuration.java). 
4. **Compile:** Build and package a fat jar: `mvn clean compile package`.
5. **Run:** Run the fat jar: `java -jar target/annotation-parser-1.0-SNAPSHOT.jar`.


### Scanning for violations in client projects

The static analysis tool that encodes all rules scans one program at a time.
The rules that we mined are already encoded
[here](./checkers/src/main/java/parser/rules/microprofile).

Note that the output results may slightly vary due to proprietary projects being removed.

Steps (given that you have already cloned this repository):

1. **Clone the repo:** download this repository.
2. **Compile:** Go into `/checkers` directory: 

```bash
cd checkers/
```

Create the static analysis checker fat jar:

```bash
mvn clean compile package
```

This creates a fat jar in the `/target` directory. Look for the one that says `...-jar-with-dependencies.jar`.

3. **Run:** Run checkers on the desired (one) project to scan for violations:

```bash
java -jar target/rules-checker-1.0-SNAPSHOT-jar-with-dependencies.jar /absolute/path/to/scan/
```

If you would like to run the checkers **through a commit history** of client projects, follow these steps (after doing Step 2 above). This assumes that you already have a directory with all client projects inside (say, `/home/foo/projects` for example):

1. **Copy commit analyzer (script):** Copy the contents of the directory [here](./utils/commit-history-analyzer) to the directory that contains all your projects you want to scan (e.g., `/home/foo/projects`). The directory includes the `traverse_git.bash` script that runs through commit history of some project. Note that we do not run checkers on all commits (albeit that is too time-consuming and ineffective). So we focus only on *relevant* commits, i.e., the commits whose content (diff) matches any keyword in the `dictionary.txt` file. The txt file contains keywords from the encoded usage rules (such annotation names and types that are part of usage rules).

```bash
cp utils/commit-history-analyzer/* <dir-with-projects>
```

2. **Run the script:** Assuming that you packaged the checkers' fat jar, use the **absolute** path to the jar when you run the checkers script. If you want to analyze commit history:

```bash
cd <dir-with-projects>
bash traverse_git.bash /correct/absolute/path/rules-checker-1.0-SNAPSHOT-jar-with-dependencies.jar
```

It might take time and easily overflow your terminal with lots of output. So you can use the redirection operation, i.e.:

```
bash traverse_git.bash <absolute-path> > output.txt
```

and the `output.txt` file will contain all the output. Violations will be marked as `[VIOLATION DETECTED]` so you can use that to search through the humongous output file.

The script basically goes through all projects: for a given project, it checks
out all relevant commits and for each commit, runs a jar, keeps the output,
checks back out to `master` or `main`.

In case you want to run checkers only on the latest commit of each project, run the following:

```bash
cp utils/run_checkers.bash <dir-with-projects>
bash run_checkers.bash /correct/absolute/path/rules-checker-1.0-SNAPSHOT-jar-with-dependencies.jar
```

The script just runs the jar file for each cloned project. Also works if it is
not a git repository (while the `traverse_git.bash` requires git repo to access its commit history).

***!!! Disclaimer !!!:*** The above `bash` scripts might not work properly on other than Linux OS.


## General Usage

If you would like to use the miner to mine usages for a library of your choice
(i.e., anything other than MicroProfile), do the following
steps:

1. Find client projects that use your library. Clone all of them into some
   directory.  Then, specify the absolute path to that directory
   [here](https://github.com/ualberta-smr/MiningAnnotationUsageRules/blob/6affc29cb05e8d0e4dde3d32e363e9e2693e6f87/miner/src/main/java/miner/Configuration.java#L19).
2. Specify the correct package (API) prefix for your library
   [here](https://github.com/ualberta-smr/MiningAnnotationUsageRules/blob/6affc29cb05e8d0e4dde3d32e363e9e2693e6f87/miner/src/main/java/miner/Configuration.java#L27-L28).
3. Find your library source code. This is needed to resolve fully-qualified
   names of types provided by the library. Specify the absolute path to the library
   sources
   [here](https://github.com/ualberta-smr/MiningAnnotationUsageRules/blob/6affc29cb05e8d0e4dde3d32e363e9e2693e6f87/miner/src/main/java/miner/Configuration.java#L32)
   (should be stored in one directory).
4. Choose what sub-APIs (sub-packages) you would like to focus on. You may want to ignore some sub-packages depending on your goal (e.g., some sub-package is soon to be deprecated). Specify the sub-packages [here](https://github.com/ualberta-smr/MiningAnnotationUsageRules/blob/6affc29cb05e8d0e4dde3d32e363e9e2693e6f87/miner/src/main/java/miner/Configuration.java#L65).
5. Specify your library sub-package API prefix [here](https://github.com/ualberta-smr/MiningAnnotationUsageRules/blob/6affc29cb05e8d0e4dde3d32e363e9e2693e6f87/miner/src/main/java/miner/Configuration.java#L103), e.g., `org.eclipse.microprofile` is the top-level package that provides entire MicroProfile API.

**Note** that the usage violation scanner is irrelevant because only
MicroProfile rules are hardcoded, i.e., we do not encode (or
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


## Contributors to this repo

Batyr Nuryyev (Main developer) and Ajay Kumar Jha.
