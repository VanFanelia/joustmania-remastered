#!/bin/bash

# Verzeichnis mit MP3-Dateien
INPUT_DIR="../src/main/resources/sound/en/mp3"

# Zielverzeichnis (optional, hier gleiches Verzeichnis)
OUTPUT_DIR="../src/main/resources/sound/en/wav"

# Schleife über alle MP3-Dateien im Verzeichnis
for mp3_file in "$INPUT_DIR"/*.mp3; do
    # Prüfen, ob Datei existiert (für den Fall, dass keine MP3s gefunden wurden)
    [ -e "$mp3_file" ] || continue

    # Basis-Dateiname ohne Erweiterung
    base_name=$(basename "$mp3_file" .mp3)

    # Ziel-Dateiname
    wav_file="$OUTPUT_DIR/${base_name}.wav"

    if [ -e "$wav_file" ]; then
      continue
    fi

    echo "Konvertiere: $mp3_file → $wav_file"

    # ffmpeg zur Konvertierung
    ffmpeg -i "$mp3_file" "$wav_file"
done

echo "Fertig!"