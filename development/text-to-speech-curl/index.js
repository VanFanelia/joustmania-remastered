import {ElevenLabsClient} from '@elevenlabs/elevenlabs-js';
import 'dotenv/config';

import fs from 'fs';
import {pipeline} from 'stream';
import {promisify} from 'util';
const streamPipeline = promisify(pipeline);


console.log(process.env.VOICE_ID);

const elevenlabs = new ElevenLabsClient();
const audio = await elevenlabs.textToSpeech.convert('JBFqnCBsd6RMkjVDRZzb', {
    text: 'Hi',
    modelId: 'eleven_multilingual_v2',
    outputFormat: 'mp3_44100_128',
});


await streamPipeline(audio, fs.createWriteStream('./output.mp3'));


