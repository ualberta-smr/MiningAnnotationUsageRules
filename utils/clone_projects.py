"""
Author: oneturkmen
Description: Script for displaying JSON data. Used for manual copy-pasting into Excel document.
Year: 2021


!!!!!!!!!!!!!! NOTE !!!!!!!!!!!!!!
Requires `gitpython` module.

Install it as follows (unless you already have it):

    pip3 install gitpython --user

"""

import sys
import git

from pprint import pprint

def read_projects_list(filename):
    """
    Read the file that contains a list of projects to clone from.
    """
    projs = []

    with open(filename) as file:
        lines = file.readlines()
        lines = [line.rstrip() for line in lines]

        for i in range(0, len(lines), 3):
            url = lines[i]
            commit = lines[i + 1]

            if '.git' in url:
                if 'git@github.com:' in url:
                    projs.append((url.split(':')[1], commit))
                else:
                    projs.append(('/'.join(url.split('/')[-2:]), commit))
    return projs

if __name__ == "__main__":
    # Check
    if len(sys.argv) != 3:
        print("Incorrect number of args! I just need a path to the file and nothing else!")
        sys.exit(1)

    file_path = sys.argv[1]
    target_dir = sys.argv[2]

    # Read rules from TXT
    projs = read_projects_list(file_path)

    # Clone each repo and checkout the target commit
    for proj, commit in projs:
        try:
            repo = git.Repo.clone_from("git://github.com/{0}".format(proj), target_dir + "/{0}".format('#'.join(proj.split('/'))), no_checkout=True)

            for submodule in repo.submodules:
                submodule.update(init=True)
        except git.exc.InvalidGitRepositoryError:
            print("Could not clone " + proj)
            continue
        except git.exc.GitCommandError:
            print("Could not clone " + proj)
            print("Check if it exists (i.e., public)?")
            continue

        try: 
            repo.git.checkout(commit)
        except git.exc.GitCommandError:
            print("Could not check out commit {0} for {1}".format(commit, proj))
            continue

