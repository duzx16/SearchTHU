## THUSearch API Specification

### Basic Search

* URL: /servlet/THUSearch/_query

* Method: GET

* Body

  ```json
  {
    "query": query字符串
    "page": 当前的页数，大于等于1
  }
  ```

  

* Success Response

  * Code: 200 OK

  * Content:

    ```json
    {
      "total": total number of ducuments matching the query,
      "correct": optional, 如果可以进行查询纠正则为纠正后的字符串,
      // the documents in this page, no more than 10 documents.
      "documents": [
      		document1,
      		...
      ]
    }
    ```

* Document:
> 这里的title和content都用highlighter做了高亮处理，其中的部分文字使用\<span class="highlight"\>...\</span\>标明

  ```json
  {
    // title and abstract might be HTML code to show hightlight
    "title": the title of the page,
    "content": a fragment of the content that matches the query,
    "url": the url of the page
  }
  ```

### Advanced Search

> 请参考谷歌的高级搜索页面

* URL:  /servlet/THUSearch/\_advanced\_query

* Method: GET

* Body

  ```json
  {
  	//at least one of these domains must be non-empty
    "exact":
  	"any": 
  	"none":
  	// all these are optional
  	"position": can only be "any", "title", "content", "link",
  	"site": specifiy domain of the site to search, please check it\'s in the format of URL in the frontend
  	"file_type": can only be "any", "html", "pdf", "doc/docx"
  }
  ```

* Success Response

  The same as basic search

### Query Autocompletion

* URL: /servlet/THUSearch/_completion

* Method: GET

* Body

  ```json
  {
    "query":
  }
  ```

* Success Response

  ```json
  {
  	"queries": [
      complete query1, // string of query
      ...
    ]
  }
  ```

  