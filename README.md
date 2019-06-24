# SearchTHU

Source code of the course project for Fundamentals of Search Engine Technology in Tsinghua University by Zhengxiao Du and Zhouxing Shi.

## Requirements

* Python 3.5+
* Java 1.8
* Node 10+
* Npm 6+
* Tomcat 9.0.21

## Deployment Guide

### Data

Data of format `.html`, `.pdf`, `.docx` or `.doc` are crawled with Heritrix. 
They are too large (>20G) and thus not included here, but can be provided upon request.

### Preprocessing

To preprocess files under the `<dir>` directory, run:

```
cd preprocess
pip install -r requirements.txt
python DocParser.py <dir>
```

For each file with path `<path>`, it outputs a processed JSON file `<path>.json`.

### Backend

Put `out/artifacts/SearchTHU_war/SearchTHU_war.war` at `webapps/` of the Tomcat directory, and then start the Tomcat service.

The API path is http://hostname:port/SearchTHU_war/. See the API details at [`doc/api.md`](doc/api.md).

### Frontend

To start the frontend service:

```
cd frontend
npm install
npm start
```

And then go to `http://localhost:8080` to start using the application.

