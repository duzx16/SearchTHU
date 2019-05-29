# SearchTHU

## 爬虫

爬取清华校园网内的HTML、PDF、DOC(DOCX)文件

结果：每个域名是一个文件夹，其下的目录结构按照域名结构来，比如http://www.sss.tsinghua.edu.cn/publish/sss/8362/2014/20140115152026399842715/20140115152026399842715_.html这个链接对应的文件就是在www.sss.tsinghua.edu.cn->publish->sss->8362->2014->20140115152026399842715/20140115152026399842715_.html

## 预处理

对`<directory>`目录下的文件进行处理：

```
python DocParser.py <directory>
```

对每个文件输出一个json文件，保存于该文件的原目录下，文件名增加后缀`.json`



> 处理结果可以考虑保存成json
>
> * html文件：提取出title、content、h1-h6等结构信息。同时要提取出网页上含有的链接(用来做pagerank)和对应的锚文本(用来作为搜索内容)
> * pdf、doc文件：尽可能多的提取出结构信息(看开源库能做到什么程度)，同时提取出含有的链接(如果有)和锚文本。

