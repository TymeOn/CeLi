import fs from 'fs';
import cors from 'cors';
import dotenv from 'dotenv';
import express from 'express';
import vosk from 'vosk';
import { Readable } from 'stream';
import wav from 'wav';
import { exec } from 'child_process';
import multer from 'multer';

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

// VARIABLES SETUP
// 

// Define the multer storage configuration
const storage = multer.diskStorage({
    destination: (req, file, callback) => {
      callback(null, 'resources/song');
    },
    
    filename: (req, file, callback) => {
      callback(null, file.originalname);
    }
});

// Define the multer upload filter configuration
const fileFilter = (req, file, cb) => {
    if (file.mimetype === 'audio/mpeg') {
      cb(null, true);
    } else {
      cb(new Error('Only mp3 files are allowed!'), false);
    }
};

// Define the multer upload configuration
const upload = multer({ 
    storage: storage,
    fileFilter: fileFilter
});

// Define the error handling middleware function
const handleError = (err, req, res, next) => {
    if (err instanceof multer.MulterError) {
      // A Multer error occurred when uploading.
      res.status(400).send({ message: 'Multer error: ' + err.message });
    } else if (err) {
      // An error occurred when uploading.
      res.status(500).send({ message: err.message });
    } else {
      // Everything went fine.
      next();
    }
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

app.post('/upload', upload.single('file'), handleError, (req, res) => {
    res.status(200).send('File uploaded successfully!');
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

app.listen(process.env.NODE_PORT, () => {
    console.log('CeLi ASR app listening on port ' + process.env.NODE_PORT);
});