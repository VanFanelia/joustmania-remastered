#!/bin/bash
for file in *.wav; do
    duration=$(ffprobe -i "$file" -show_entries format=duration -v quiet -of csv="p=0")
    echo "$file: ${duration}s"
done
