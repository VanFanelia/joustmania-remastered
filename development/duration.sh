#!/bin/bash
for file in ./text-to-speech-curl/output/en/mp3/*.mp3; do
    duration=$(ffprobe -i "$file" -show_entries format=duration -v quiet -of csv="p=0")
    echo "$file: ${duration}s"
done
