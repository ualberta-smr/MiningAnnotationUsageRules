import sys
import json

"""
Author: oneturkmen
Description: Script for displaying JSON data. Used for manual copy-pasting into Excel document.
Year: 2021
"""

def read_rules(path):
    """
    Read JSON file and output a list of rules.
    """
    with open(path) as f:
        data = json.load(f)
        return data

def write_to_csv(rules, path):
    """
    Write rules to CSV.
    """
    df = pd.DataFrame(rules)
    df.to_csv(path, encoding='utf-8', index=False)
    return

def process_rules(rules):
    for r in rules:
        ant = ', '.join(r['antecedent'])
        con = ', '.join(r['consequent'])
        #label = r['label']

        #editDist = "N/A"
        #if 'distanceComments' in r:
        #    editDist = '\n'.join(r['distanceComments'])

        print("\"IF {0},\n\nTHEN {1}\"".format(ant, con))
        #print("\"{0}\"".format(label))
        #print("\"{0}\"".format(editDist))


if __name__ == "__main__":
    # Check
    if len(sys.argv) != 2:
        print("Incorrect number of args! I just need a path to the file and nothing else!")
        sys.exit(1)

    file_path = sys.argv[1]

    if file_path[-5:] != ".json":
        print("Not a correct file extension! I need JSON.")

    # Read rules from JSON
    rules = read_rules(file_path)

    # Process rules
    process_rules(rules)

    # Output rules into CSV

