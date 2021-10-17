# Runs a fat jar (or any executable, actually) against the latest commit of projects
#
# Author: oneturkmen
# Year: 2021

PATH_TO_JAR="$1"
CURR_DIR="$(pwd)"

if [ $# -eq 0 ]
then
    echo "No arguments supplied! Need a path to the jar file."
    exit 1
fi

for d in *
do (
    if [[ -d "$d" ]]
    then
        cd $d;
        echo "-------------- $d -------------";
        java -jar "$PATH_TO_JAR" "$(pwd $d)"
    fi
)
done
