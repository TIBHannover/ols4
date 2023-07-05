#!/usr/bin/env bash

curl "https://service.tib.eu/ts4tib/api/ols-config" \
        | yq eval -j - > TS-config.json



