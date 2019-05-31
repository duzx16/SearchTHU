import os, sys, json, jieba, subprocess
from tqdm import tqdm
from bs4 import BeautifulSoup as bs
from docx import Document
from multiprocessing import Pool

num_cpus = 32

class ProgressBar:
    def __init__(self):
        pass

    def set_total(self, tot):
        self.bar = tqdm(total=tot)
    
    def update(self):
        self.bar.update(1)

progress_bar = ProgressBar()  

class DocParser:
    def __init__(self):
        pass
    
    def parse(self, path):
        res = self._parse(path)
        progress_bar.update()
        return res

    def _parse(self, path):
        pass

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

    def translate_name(self, path):
        name_old, name_new = "", ""
        for c in path:
            if c in [" ", "(", ")", "&", "’", "'"]: name_old += "\\%s" % c
            else:
                name_old += c
                name_new += c   
        return name_old, name_new     

class DocParserHtml(DocParser):
    def _parse(self, path):
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
            print("Failed to parse %s" % path)
            return {}

        soup = bs(content, "html.parser")
        res = {}
        
        title = soup.find("title")
        if title is not None:
            res["title"] = self.tokenize(title.text)

        for i in range(1, 7):
            hkey = "h%d" % i
            h = soup.find_all(hkey)
            if len(h) > 0:
                res[hkey] = ""
                for item in h:
                    res[hkey] += self.tokenize(item.text) + " "

        res["links"] = []
        for a in soup.find_all("a"):
            attrs = a.attrs
            if "href" in attrs:
                res["links"].append({
                    "href": attrs["href"],
                    "text": self.tokenize(a.text)
                })

        content_tags = ["p", "span"]
        res["content"] = ""
        for tag in content_tags:
            for item in soup.find_all(tag):
                res["content"] += self.tokenize(item.text) + " "
                
        return res

class DocParserPdf(DocParserHtml):
    def __init__(self):
        super().__init__()

        if not os.path.exists("tools/pdfbox.jar"):
            print("PDFBox not found")
            print("Installing PDFBox...")
            if not os.path.exists("tools"):
                os.mkdir("tools")
            os.system("wget https://www-eu.apache.org/dist/pdfbox/2.0.15/pdfbox-app-2.0.15.jar -O tools/pdfbox.jar")
            print("PDFBox installed")

    def _parse(self, path):
        name_old, name_new = self.translate_name(path)
        name_new += ".tmp.pdf"
        os.system("cp %s %s" % (name_old, name_new))
        new_path = "%s.html" % name_new
        os.system("java -jar tools/pdfbox.jar ExtractText -html -encoding UTF-8 %s %s 1> /dev/null 2> /dev/null" % (
            name_new, new_path))
        os.system("rm %s" % name_new)
        res = super()._parse(new_path)
        os.system("rm %s" % new_path)
        return res

class DocParserDocx(DocParser):
    def _parse(self, path):
        try:
            document = Document(path)
        except:
            print("Failed to parse %s" % path)
            return {}

        res = {}
        res["content"] = ""
        for para in document.paragraphs:
            style = para.style.name
            if style.startswith("Heading"):
                hkey = "h%d" % int(style.split()[1])
                if not hkey in res: res[hkey] = ""
                res[hkey] += self.tokenize(para.text) + " "
            elif style == "Title":
                res["title"] = self.tokenize(para.text)
            else:
                res["content"] += self.tokenize(para.text) + " "

        return res

class DocParserDoc(DocParserPdf):
    def _parse(self, path):   
        name_old, name_new = self.translate_name(path)
        _name_new = name_new
        name_new += ".tmp.doc"
        os.system("cp %s %s" % (name_old, name_new))
        os.system("lowriter --convert-to pdf --outdir %s %s 1> /dev/null 2> /dev/null" % (
            os.path.dirname(name_new), name_new))
        os.system("rm %s" % name_new)
        name_new = _name_new + ".tmp.pdf"
        res = super()._parse(name_new)
        os.system("rm %s" % name_new)
        return res

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
    "docx": DocParserDocx(),
    "doc": DocParserDoc()
}
for suffix in files:
    print("Parsing .%s files..." % suffix)
    progress_bar.set_total( (len(files[suffix]) + num_cpus - 1) // num_cpus)
    with Pool(processes=num_cpus) as pool:
        res = pool.map(parser[suffix].parse, files[suffix])
    print("Writting results...")
    for i, path in enumerate(tqdm(files[suffix])):
        with open(path + ".json", "w") as file:
            file.write(json.dumps(res[i]))
    print()