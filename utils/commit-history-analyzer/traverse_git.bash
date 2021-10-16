# Runs a fat jar (or any executable, actually) against the commit history of projects
#
# Author: oneturkmen
# Year: 2021

PATH_TO_JAR="$1"
CURR_DIR="$(pwd)"

if [ $# -eq 0 ]
then
    echo "No arguments supplied! Need a path to the jar file."
fi

for d in *
do (
    if [[ -d "$d" ]]
    then
        cd $d;
        echo "-------------- $d -------------";
        #echo "$(git log --grep="fix")";

        # Find all commits with fixes
        for h in $(git rev-list --all)    
        do (
            CONTENT="$(git show $h)"

            # Check if the commit content contains any of the keywords from dictionary.txt
            if [[ "$CONTENT" =~ $(echo \($(paste -sd'|' $CURR_DIR/dictionary.txt)\)) ]]; then
                # Debugging matches
                #MATCH=$(grep -B 10 -A 10 "$keyword" <<< "$CONTENT")
                #grep -v "$opposite" <<< "$MATCH"

                # Check out the repository at this commit
                git checkout "$h" &> /dev/null

                # Run the tool
                echo "Running the tool for commit -> $h";
                java -jar "$PATH_TO_JAR" "$(pwd $d)"
            fi
        )
        done

        # Check out back to master. This might fail since GitHub changed primary branch name
        git checkout master

        # Check out back to main
        git checkout main
    fi
)
done
#> points_of_interest.txt
