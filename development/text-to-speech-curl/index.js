import {ElevenLabsClient} from '@elevenlabs/elevenlabs-js';
import 'dotenv/config';

import fs from 'fs';
import {pipeline} from 'stream';
import {promisify} from 'util';
import md5 from 'md5';
import { execSync } from 'child_process';

const streamPipeline = promisify(pipeline);

if (!process.env.ELEVENLABS_API_KEY) {
    throw new Error('Please set the ELEVENLABS_API_KEY environment variable.');
}
const VOICE_ID = process.env.VOICE_ID

// copy sounds.json next to this file
await fs.copyFileSync('../../src/main/resources/sound/sounds.json', './sounds.json');

// load sounds.json into variable
const sounds = JSON.parse(fs.readFileSync('./sounds.json', 'utf8'));

// iterate over sounds when it is a JSON object with key-value pairs
for (const [soundKey, soundConfig] of Object.entries(sounds)) {
    console.log(`Sound: ${soundKey}`);

    if (!soundConfig.isSpokenText) {
        console.log(`Skipping ${soundKey} because it contains non spoken text`);
        continue;
    }

    for(const [language, soundConfigFor] of Object.entries(soundConfig.soundFiles)) {
        if (language !== 'EN') {
            console.log(`Skipping language: ${language} for sound: ${soundKey}`);
            continue;
        }

        const text = soundConfigFor.localizedText;
        const fileName = soundConfigFor.file;
        const md5OfText = md5(text);

        const outputFilePath = `./output/${language.toLowerCase()}/mp3/${fileName}.${language}.${md5OfText}.mp3`;

        // Check if MP3 exists
        if (fs.existsSync(outputFilePath)) {
            console.log(`File already exists: ${outputFilePath}. Skipping...`);
        } else {
            // Generate MP3 using ElevenLabs
            await getSoundFileFromElevenLabs(outputFilePath, text);
            console.log(`Sound ${soundKey} generated successfully. Output file: ${outputFilePath}`);
        }
        
        // copy to resources if not exists there
        const targetResourceFilePathForMp3 = `../../src/main/resources/sound/${language.toLowerCase()}/mp3/${fileName}.mp3`;

        if (!fs.existsSync(targetResourceFilePathForMp3)) {
            console.log(`Copying mp3 file: ${outputFilePath} → ${targetResourceFilePathForMp3}`);
            try {
                fs.copyFileSync(outputFilePath, targetResourceFilePathForMp3);
                console.log(`File copied to resources: ${targetResourceFilePathForMp3}`);
            } catch (error) {
                console.error(`Error copying mp3 file: ${error.message}`);
            }
        }

        // generate wav if not exists there
        const targetResourceFilePathForWav = `../../src/main/resources/sound/${language.toLowerCase()}/wav/${fileName}.wav`;

        if (!fs.existsSync(targetResourceFilePathForWav)) {
            console.log(`try to convert mp3 file to wav: ${outputFilePath} → ${targetResourceFilePathForWav}`);
            try {
                execSync(`ffmpeg -i "${outputFilePath}" -acodec pcm_s16le -ar 48000 -ac 2 "${targetResourceFilePathForWav}"`);
                console.log(`File copied to resources: ${targetResourceFilePathForWav}`);
            } catch (error) {
                console.error(`Error converting mp3 file to wav: ${error.message}`);
            }
        }
    }
}

async function getSoundFileFromElevenLabs(outputFile, textToGenerate) {
    const elevenLabs = new ElevenLabsClient();
    const audio = await elevenLabs.textToSpeech.convert(VOICE_ID, {
        text: textToGenerate,
        modelId: 'eleven_multilingual_v2',
        outputFormat: 'mp3_44100_128',
    });
    await streamPipeline(audio, fs.createWriteStream(outputFile));
}