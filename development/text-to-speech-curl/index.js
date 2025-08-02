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
        const outputFilePathWav = `./output/${language.toLowerCase()}/wav/${fileName}.${language}.${md5OfText}.wav`

        // Check if MP3 exists
        if (fs.existsSync(outputFilePath)) {
            console.log(`File already exists: ${outputFilePath}. Skipping...`);
        } else {
            // Generate MP3 using ElevenLabs
            await getSoundFileFromElevenLabs(outputFilePath, text);
            console.log(`Sound ${soundKey} generated successfully. Output file: ${outputFilePath}`);
        }

        // Convert to WAV if not already converted
        if (!fs.existsSync(outputFilePathWav)) {
            console.log(`Converting MP3 to WAV: ${outputFilePath} → ${outputFilePathWav}`);
            try {
                execSync(`ffmpeg -i "${outputFilePath}" "${outputFilePathWav}"`);
                console.log(`Conversion successful: ${outputFilePathWav}`);
            } catch (error) {
                console.error(`Error during conversion: ${error.message}`);
            }
        }
        
        // copy to resources if not exists there
        const resourceFilePathWav = `../../src/main/resources/sound/${language.toLowerCase()}/wav/${fileName}.wav`;

        if (!fs.existsSync(resourceFilePathWav)) {
            console.log(`Copying WAV file to resources: ${outputFilePathWav} → ${resourceFilePathWav}`);
            try {
                fs.copyFileSync(outputFilePathWav, resourceFilePathWav);
                console.log(`File copied to resources: ${resourceFilePathWav}`);
            } catch (error) {
                console.error(`Error copying WAV file: ${error.message}`);
            }
        }
    }
}

// go through every file in ./output/en/wav and replace

async function getSoundFileFromElevenLabs(outputFile, textToGenerate) {
    const elevenLabs = new ElevenLabsClient();
    const audio = await elevenLabs.textToSpeech.convert(VOICE_ID, {
        text: textToGenerate,
        modelId: 'eleven_multilingual_v2',
        outputFormat: 'mp3_44100_128',
    });
    await streamPipeline(audio, fs.createWriteStream(outputFile));
}