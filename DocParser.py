import os, sys
from bs4 import BeautifulSoup as bs

files = {
    "html": [],
    "pdf": [],
    "doc": [],
    "docx": []
}

def search(dir):
    for item in os.listdir(dir):
        path = os.path.join(dir, item)
        if os.path.isdir(path):
            search(path)
        else:
            for suffix in files:
                if path.endswith(suffix):
                    files[suffix].append(path)

root = sys.argv[1]
search(root)

