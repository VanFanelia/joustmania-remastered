#!/bin/bash

# Konfiguration
JSON_FILE="../src/main/resources/sound/sounds.json"
AUDIO_DIR="../src/main/resources/sound/en/wav"  # Anpassen: Verzeichnis wo sich die Audiodateien befinden
AUDIO_EXTENSION="wav"  # Anpassen: Dateiendung der Audiodateien (wav, mp3, ogg, etc.)

# Überprüfung ob jq installiert ist
if ! command -v jq &> /dev/null; then
echo "Fehler: jq ist nicht installiert. Bitte installieren Sie jq."
exit 1
fi

# Überprüfung ob ffprobe (Teil von ffmpeg) installiert ist
if ! command -v ffprobe &> /dev/null; then
echo "Fehler: ffprobe ist nicht installiert. Bitte installieren Sie ffmpeg."
exit 1
fi

# Überprüfung ob JSON-Datei existiert
if [ ! -f "$JSON_FILE" ]; then
echo "Fehler: $JSON_FILE wurde nicht gefunden."
exit 1
fi

echo "Erweitere soundFiles mit durationInMs..."

# Backup erstellen
cp "$JSON_FILE" "${JSON_FILE}.backup"
echo "Backup erstellt: ${JSON_FILE}.backup"

# Temporäre Datei für die Ausgabe
TEMP_FILE=$(mktemp)

# JSON verarbeiten
jq --arg audio_dir "$AUDIO_DIR" --arg extension "$AUDIO_EXTENSION" '
def get_duration(file_path):
(file_path | @sh) as $escaped_path |
("ffprobe -v quiet -show_entries format=duration -of csv=p=0 " + $escaped_path) as $cmd |
($cmd | @sh) as $shell_cmd |
try (
$shell_cmd | getpath(["output"]) | tonumber | . * 1000 | floor
) catch 0;

# Durchlaufe alle Einträge und deren soundFiles
to_entries | map(
.value.soundFiles = (
.value.soundFiles | to_entries | map(
.value.file as $filename |
($audio_dir + "/" + $filename + "." + $extension) as $file_path |
.value.durationInMs = get_duration($file_path)
) | from_entries
)
) | from_entries
' "$JSON_FILE" > "$TEMP_FILE"

# Funktion zur Ermittlung der Audiodauer in Millisekunden
get_audio_duration() {
local file_path="$1"

if [ ! -f "$file_path" ]; then
echo "0"  # Fallback wenn Datei nicht existiert
return
fi

# ffprobe verwenden um die Dauer zu ermitteln
duration_seconds=$(ffprobe -v quiet -show_entries format=duration -of csv=p=0 "$file_path" 2>/dev/null)

if [ -z "$duration_seconds" ] || [ "$duration_seconds" = "N/A" ]; then
echo "0"
return
fi

# In Millisekunden konvertieren und runden
duration_ms=$(echo "$duration_seconds * 1000" | bc -l | cut -d'.' -f1)
echo "$duration_ms"
}

# Da jq keine externen Befehle ausführen kann, müssen wir das anders machen
# Erstelle eine neue JSON-Datei mit den Dauern

echo "Verarbeite soundFiles und ermittle Dauern..."

# Alle Dateipfade und Keys sammeln
files_and_keys=$(jq -r '
to_entries[] |
.key as $main_key |
.value.soundFiles | to_entries[] |
.key as $lang_key |
"\($main_key).\($lang_key) \(.value.file)"
' "$JSON_FILE")

# JSON Schritt für Schritt aktualisieren
cp "$JSON_FILE" "$TEMP_FILE"

while IFS=' ' read -r key_path filename; do
if [ -n "$filename" ] && [ -n "$key_path" ]; then
audio_file="${AUDIO_DIR}/${filename}.${AUDIO_EXTENSION}"
duration=$(get_audio_duration "$audio_file")

echo "  $filename -> ${duration}ms"

# JSON aktualisieren
jq --arg key_path "$key_path" --argjson duration "$duration" '
def update_at_path($path; $value):
def _update_at_path:
if length == 0 then $value
elif length == 1 then .[.[0]] = $value
else .[.[0]] |= (_update_at_path | . as $result | if . == null then {} else . end | . + {"temp": $result} | .temp)
end;
($path | split(".")) as $path_array |
_update_at_path;

.[$key_path | split(".")[0]].soundFiles[$key_path | split(".")[1]].durationInMs = $duration
' "$TEMP_FILE" > "${TEMP_FILE}.tmp" && mv "${TEMP_FILE}.tmp" "$TEMP_FILE"
fi
done <<< "$files_and_keys"

# Überprüfung ob die Verarbeitung erfolgreich war
if jq . "$TEMP_FILE" >/dev/null 2>&1; then
mv "$TEMP_FILE" "$JSON_FILE"
echo ""
echo "✅ Erfolgreich abgeschlossen!"
echo "Die Datei $JSON_FILE wurde mit durationInMs-Eigenschaften erweitert."
echo "Ein Backup der ursprünglichen Datei wurde als ${JSON_FILE}.backup gespeichert."
else
echo "❌ Fehler beim Verarbeiten der JSON-Datei."
rm -f "$TEMP_FILE"
exit 1
fi

# Aufräumen
rm -f "$TEMP_FILE"

echo ""
echo "Beispiel der aktualisierten Struktur:"
jq '.NEW_CONTROLLER.soundFiles.EN' "$JSON_FILE"