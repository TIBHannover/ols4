Converts ontologies represented in OWL RDF/XML to Solr and Neo4j databases.

# Usage

Start with a config JSON file that lists the ontologies you want to load. You can get the OBO config into a file called `foundry.json` like so (make sure you have yq installed):

    curl "https://raw.githubusercontent.com/OBOFoundry/OBOFoundry.github.io/master/_config.yml" \
        | yq eval -j - > foundry.json
        
        
## Step 1: OWL to JSON

Use rdf2json to download all the OWL files, resolve imports, and export JSON files:

     java -jar rdf2json/target/rdf2json-1.0-SNAPSHOT.jar --config file://$(pwd)/foundry.json --output foundry_out.json
     
Now (after about 15 min) you should have a huge file called `foundry_out.json` that contains not only the original config for each ontology loaded from `foundry.json`, but also the ontologies themselves represented in an intermediate JSON format! (Note: the intermediate JSON format is a non-standardised application format totally specific to this tool and is subject to change.)

## Step 2: Link JSON
Use linker to link the json into a jsonl file. 

    java -jar linker/target/linker-1.0-SNAPSHOT.jar --input foundry_out.json --output foundry_out.jsonl

## Step 2: JSON to CSV *for Neo4j*

You can now convert this huge JSON file to a CSV file ready for Neo4j, using json2neo:

    rm -rf output_csv && mkdir output_csv
    java -jar json2neo/target/json2neo-1.0-SNAPSHOT.jar --input foundry_out_.jsonl --outDir output_csv

## Step 3: CSV to Neo4j

Now (after 5-10 mins) you should have a directory full of CSV files. These files are formatted especially for Neo4j. You can load them using `neo4j-admin import` command or the `csv2neo` module. 

### Alternative 1: Neo4j Import Command

The Neo4J import command can only be used when initializing a database in the community edition of Neo4J. On the contrary, the enterprise version of Neo4j enables multiple imports which can yield in a more flexible ontology ingestion. 
IWhen you are using `neo4j-admin import` command, you'll need to provide the filename of every single CSV file on the command line, which is boring, so included in this repo is a script called `make_csv_import_cmd.sh` that generates the command line for you.

    neo4j-admin import \
	    --ignore-empty-strings=true \
	    --legacy-style-quoting=false \
	    --multiline-fields=true \
	    --array-delimiter="|" \
	    --database=neo4j \
	    $(./make_csv_import_cmd.sh)

Now you should have a Neo4j database ready to start!

### Alternative2: CSV to Neo4J Module:

The module is flexible and enables you to perform multiple ingestions on a live database. It can be triggered with the following command:

    java -jar csv2neo/target/csv2neo-1.0-SNAPSHOT.jar -i -d output_csv

## Step 4: JSON to JSON *for Solr*

Similar to how the Neo4j CSV was generated, you can also generate JSON files ready for uploading to SOLR using `json2solr` which can also be performed on a live Solr instance.

    java -jar json2solr/target/json2solr-1.0-SNAPSHOT.jar --input foundry_out.jsonl --outDir output_csv


