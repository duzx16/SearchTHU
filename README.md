# SearchTHU

## Requirements

* Python 3.5+

* Java 8

* Node 10+

* Npm 6+

## Deployment Guide

### Data

Data of format `.html`, `.pdf`, `.docx` or `.doc` are crawled with Heritrix. 
They are too large (>20G) and thus not included here, but can be provided upon request.

### Preprocessing

To preprocess files under the `<dir>` directory, run:

```
pip install -r requirements.txt
python DocParser.py <dir>
```

For each file with path `<path>`, it outputs a processed JSON file `<path>.json`.

### Backend

### Frontend

To start the frontend service:

```
cd frontend
npm install
npm start
```

And then go to `http://localhost:8080` to start using the application.

