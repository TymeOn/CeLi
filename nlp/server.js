import express from 'express';
import dotenv from 'dotenv';
import cors from 'cors';


// GENERAL SETUP
// ---------

dotenv.config();


// EXPRESS SETUP
// -------------

const app = express();
app.use(express.json());
app.use(cors());
app.use(express.urlencoded({ extended: true }));


// ROUTES
// ------

app.get('/', (req, res) => {
    res.send('Hello World!')
});

app.post('/asr', (req, res) => {
    let command = req.body.command ? req.body.command : '';
    var match = command.match(new RegExp(process.env.REGEX, "g"));
    console.log(match);
    res.status(200).json({match});
});


// STARTUP
// -------

app.listen(process.env.NODE_PORT, () => {
    console.log('CeLi ASR app listening on port ' + process.env.NODE_PORT);
});
  