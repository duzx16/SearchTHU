public class SearchResults {
    public SearchDocument[] documents;
    public int total;
    public String correct;
}

class SearchDocument {
    public String title;
    public String content;
    public String url;

    SearchDocument(String title, String content, String url) {
        this.title = title;
        this.content = content;
        this.url = url;
    }
}
