import fs from 'fs';
import cors from 'cors';
import https from 'https';
import dotenv from 'dotenv';
import express from 'express';
import vosk from 'vosk';
import { Readable }from 'stream';
import wav from 'wav';
import { exec } from 'child_process';

// GENERAL SETUP
// ---------

dotenv.config();

// EXPRESS SETUP
// -------------

const app = express();
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(cors());

const MODEL_PATH = "resources/models/vosk-model-fr-0.22"
const FILE_NAME = "resources/song/AudioRecordCeli"

const options = {
  key: fs.readFileSync('keys/key.pem'),
  cert: fs.readFileSync('keys/cert.pem')
};

// ROUTES
// ------

app.get('/asr', (req, res) => {

    try 
    {
        exec('ffmpeg -i '+ FILE_NAME + '.mp3 -ac 1 -ar 16000 -acodec pcm_s16le ' + FILE_NAME + '.wav')
    } 
    catch (error) 
    {
        res.status(400).send({ errName: e.name, errMessage: e.message });
        process.exit()
    }

    if (!fs.existsSync(MODEL_PATH)) {
        console.log("The model is :" + MODEL_PATH)
        process.exit()
    }

    if (process.argv.length > 2)
        FILE_NAME = process.argv[2]

        vosk.setLogLevel(0);
        const model = new vosk.Model(MODEL_PATH);
        
        const wfReader = new wav.Reader();
        const wfReadable = new Readable().wrap(wfReader);
        
        wfReader.on('format', async ({ audioFormat, sampleRate, channels }) => {
            if (audioFormat != 1 || channels != 1) {
                console.error("Audio file must be WAV format mono PCM.");
                process.exit(1);
            }
            const rec = new vosk.Recognizer({model: model, sampleRate: sampleRate});
            rec.setMaxAlternatives(10);
            rec.setWords(true);
            rec.setPartialWords(true);

            for await (const data of wfReadable) 
            {
                rec.acceptWaveform(data);
            }

            let results = rec.finalResult();
            
            const text = await getBestTranscription(results);

            rec.free();
            
            
            fs.unlink(FILE_NAME + '.wav', (err) => {
                if (err) throw err;
                console.log("File deleted!");
            });
            
            fs.unlink(FILE_NAME + '.mp3', (err) => {
                if (err) throw err;
                console.log("File deleted!");
            });

            res.status(200).send(text);
        });
        
        fs.createReadStream(FILE_NAME + '.wav', {'highWaterMark': 4096}).pipe(wfReader).on('finish', 
            function (err) {
                model.free();
        });
});

// UTILITY FUNCTIONS
// 
async function getBestTranscription(results) 
{
    let confidence = 0;
    let text = '';

    for (const result of results.alternatives) 
    {
        if (confidence < result.confidence) 
        {
            confidence = result.confidence;
            text = result.text;
        }
    }
    
    return text;    
}

// STARTUP
// -------

https.createServer(options, app).listen(process.env.PORT_NODE, () => {
    console.log('CreaPass app listening on port ' + process.env.PORT_NODE);
  });