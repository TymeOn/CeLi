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

app.post('/nlp', (req, res) => {
    let command = req.body.command ? req.body.command : '';
    let match = '';

    if (command.match(new RegExp('(' + process.env.COMMAND_WORDS + ')', 'g')) != null) {
        match = command.replace(new RegExp('.*(' + process.env.COMMAND_WORDS + ') (.*)', 'g'), '$2');
    }

    res.status(200).json({match});
});


// STARTUP
// -------

app.listen(process.env.NODE_PORT, () => {
    console.log('CeLi NLP app listening on port ' + process.env.NODE_PORT);
});
  