#!/usr/bin/env bash

if [ $# == 0 ]; then
    echo "Usage: $0 <solrpath> <csvdir>"
    exit 1
fi

$1/bin/solr start -force -Djetty.host=127.0.0.1
sleep 10

FILES=$2/*_*.jsonl
for f in $FILES
do
  echo "$f"
  if [[ $f == *_ontologies.jsonl ]] || [[ $f == *_classes.jsonl ]] || [[ $f == *_properties.jsonl ]] || [[ $f == *_individuals.jsonl ]]; then
    echo 'entity'
    wget --method POST --no-proxy -O - --server-response --content-on-error=on --header="Content-Type: application/json" --body-file $f http://127.0.0.1:8983/solr/ols4_entities/update/json/docs?commit=true
  elif [[ $f == *_autocomplete.jsonl ]]; then
    echo 'autocomplete'
    wget --method POST --no-proxy -O - --server-response --content-on-error=on --header="Content-Type: application/json" --body-file $f http://127.0.0.1:8983/solr/ols4_autocomplete/update/json/docs?commit=true
  fi
done
sleep 5
echo 'update entities'
wget --no-proxy http://127.0.0.1:8983/solr/ols4_entities/update?commit=true
sleep 5
echo 'update autocomplete'
wget --no-proxy http://127.0.0.1:8983/solr/ols4_autocomplete/update?commit=true
echo 'loading solr finished'

$1/bin/solr stop
