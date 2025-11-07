#!/bin/bash

# Directories to compare
dir1="output_csv93"
dir2="output_csv81"

output_dir="diff_results"
mkdir -p "$output_dir"

for file in "$dir1"/*; do
    filename=$(basename "$file")
    file2="$dir2/$filename"
    diff_file="$output_dir/${filename}.diff"

    if [[ ! -f "$file2" ]]; then
        echo "Skipping $filename: not found in $dir2"
        continue
    fi

    echo "Comparing: $filename"


    # --- Compare sizes ---
    size1=$(stat -c%s "$file" 2>/dev/null || stat -f%z "$file")
    size2=$(stat -c%s "$file2" 2>/dev/null || stat -f%z "$file2")

    if (( size1 == size2 )); then
        echo "  ↔️  Same size ($size1 bytes)"
    else
        echo "  ⚖️  Different sizes: $size1 vs $size2 bytes"
    fi

    case "$filename" in
        *.json)
            # Use jq to pretty-print & sort JSON keys before diffing
            if command -v jq >/dev/null; then
                diff -u <(jq -S . "$file") <(jq -S . "$file2") > "$diff_file"
            else
                echo "jq not found — falling back to plain diff for $filename"
                diff -u "$file" "$file2" > "$diff_file"
            fi
            ;;
        *.csv)
            # Normalize CSVs by removing extra quotes and spaces
            # Use csvformat (from csvkit) if available
            if command -v csvformat >/dev/null; then
                diff -u <(csvformat -U 1 "$file") <(csvformat -U 1 "$file2") > "$diff_file"
            else
                echo "csvformat not found — using plain diff for $filename"
                diff -u "$file" "$file2" > "$diff_file"
            fi
            ;;
        *)
            # Fallback for any other file types
            diff -u "$file" "$file2" > "$diff_file"
            ;;
    esac

    # Remove empty diffs
    [[ ! -s "$diff_file" ]] && rm "$diff_file"
done

echo "✅ Comparison complete. See results in $output_dir/"

