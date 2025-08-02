import {ElevenLabsClient} from '@elevenlabs/elevenlabs-js';
import 'dotenv/config';

import fs from 'fs';
import {pipeline} from 'stream';
import {promisify} from 'util';
import md5 from 'md5';
const streamPipeline = promisify(pipeline);

if (!process.env.ELEVENLABS_API_KEY) {
    throw new Error('Please set the ELEVENLABS_API_KEY environment variable.');
}

// copy sounds.json next to this file
fs.copyFile('../../src/main/resources/sound/sounds.json', './sounds.json', (err) => {
    if (err) {
        console.error('Error copying sounds.json:', err);
        throw err
    } else {
        console.log('sounds.json copied successfully.');
    }
});

// load sounds.json into variable
const sounds = JSON.parse(fs.readFileSync('./sounds.json', 'utf8'));

// iterate over sounds when it is a json object with key-value pairs
for (const [soundKey, soundConfig] of Object.entries(sounds)) {
    console.log(`Sound: ${soundKey}`);

    for(const [language, soundConfigFor] of Object.entries(soundConfig.soundFiles)) {
        const text = soundConfigFor.localizedText;
        const fileName = soundConfigFor.file;
        // hash the text to create a unique identifier

        console.log(`Text: ${text}`);
        const md5OfText = md5(text);
        const outputFilePath = `./output/${language}/${fileName}.${language}.${md5OfText}.mp3`;
        // check if the output file already exists
        if (fs.existsSync(outputFilePath)) {
            console.log(`File already exists: ${outputFilePath}. Skipping...`);
            continue;
        }
        console.log(`Output file path: ${outputFilePath}`);
    }
}

/*
const elevenlabs = new ElevenLabsClient();
const audio = await elevenlabs.textToSpeech.convert('JBFqnCBsd6RMkjVDRZzb', {
    text: 'Hi',
    modelId: 'eleven_multilingual_v2',
    outputFormat: 'mp3_44100_128',
});


await streamPipeline(audio, fs.createWriteStream('./output.mp3'));
*/

