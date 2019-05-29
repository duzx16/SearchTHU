import os, sys, json, jieba
from tqdm import tqdm
from bs4 import BeautifulSoup as bs

class DocParser:
    def __init__(self):
        pass
    
    def parse(self, path):
        return {}

    def check_token(self, token):
        is_english, is_chinese = True, True
        for c in token:
            if 'A' <= c <= 'Z' or 'a' <= c <= 'z':
                type = 0
            elif u'\u4e00' <= c <= u'\u9fff':
                type = 1
            else:
                type = 2
            if type != 0: is_english = False
            if type != 1: is_chinese = False
        return is_english or is_chinese

    def tokenize(self, str):
        l = list(jieba.cut_for_search(str))
        res = []
        for token in l:
            if self.check_token(token):
                res.append(token)
        res = " ".join(res)
        return res

class DocParserHtml(DocParser):
    def __init__(self):
        pass

    def parse(self, path):
        charsets = ["utf-8", "gb18030", "gb2312"]
        content = None
        for charset in charsets:
            try:
                with open(path, encoding=charset) as file:
                    content = file.read()
                    break
            except:
                continue
        if content is None: 
            print("failed to parse %s" % path)
            return {}

        soup = bs(content, "html.parser")
        res = {}
        
        title = soup.find("title")
        if title is not None:
            res["title"] = title.text

        for i in range(1, 7):
            hkey = "h%d" % i
            h = soup.find_all(hkey)
            if len(h) > 0:
                res[hkey] = []
                for item in h:
                    res[hkey].append(item.text)

        res["links"] = []
        for a in soup.find_all("a"):
            attrs = a.attrs
            if "href" in attrs:
                res["links"].append({
                    "href": attrs["href"],
                    "text": a.text
                })

        content_tags = ["p", "span"]
        res["content"] = []
        for tag in content_tags:
            for item in soup.find_all(tag):
                res["content"].append(item.text)
        res["content"] = self.tokenize(" ".join(res["content"]))
                
        return res

class DocParserPdf(DocParser):
    pass

class DocParserDoc(DocParser):
    pass

def search(dir, files):
    for item in os.listdir(dir):
        path = os.path.join(dir, item)
        if os.path.isdir(path):
            search(path, files)
        else:
            for suffix in files:
                if path.endswith("." + suffix):
                    files[suffix].append(path)
                    break

files = {
    "html": [],
    "pdf": [],
    "doc": [],
    "docx": []
}
root = sys.argv[1]
print("Searching files...")
search(root, files)
print("%d files found" % sum([len(files[suffix]) for suffix in files]))
parser = {
    "html": DocParserHtml(),
    "pdf": DocParserPdf(),
    "doc": DocParserDoc(),
    "docx": DocParserDoc()
}
for suffix in files:
    print("Parsing .%s files..." % suffix)
    for path in tqdm(files[suffix]):
        # print(path)
        parsed = parser[suffix].parse(path)
        # print(parsed)
        with open(path + ".json", "w") as file:
            file.write(json.dumps(parsed))