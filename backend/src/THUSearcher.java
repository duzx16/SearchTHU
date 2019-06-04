import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class THUSearcher {
    private IndexReader reader;
    private IndexSearcher searcher;
    private Analyzer analyzer;
    private SimpleQueryParser parser;
    private Map<String, Float> field_weights = new HashMap<>();
    private SimpleHTMLFormatter htmlFormatter;

    public THUSearcher(String indexDir, Similarity similarity) throws IOException {
        field_weights.put("title", 1.0f);
        field_weights.put("content", 0.5f);
        field_weights.put("h1", 5.0f);
        analyzer = new SmartChineseAnalyzer();
        Path indexPath = Paths.get(indexDir);
        reader = DirectoryReader.open(FSDirectory.open(indexPath));
        searcher = new IndexSearcher(reader);
        searcher.setSimilarity(similarity);
        parser = new SimpleQueryParser(analyzer, field_weights);
        // highlighter
        htmlFormatter = new SimpleHTMLFormatter("<span class=\"highlight\">", "</span class=\"highlight\">");
    }

    public SearchResults searchQuery(String query_str, int offset, int max_num) throws IOException {
        Query query = parser.parse(query_str);
        TopDocs topDocs = searcher.search(query, offset + max_num);
        SearchResults results = new SearchResults();
        results.documents = new SearchDocument[max_num];
        Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
        for (int i = offset; i < topDocs.scoreDocs.length; ++i) {
            int id = topDocs.scoreDocs[i].doc;
            Document document = searcher.doc(id);
            String title = document.get("title");
            String content = document.get("content");
            String url = document.get("url");
            TokenStream titleTokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), id, "title", analyzer);
            try {
                title = highlighter.getBestFragment(titleTokenStream, title);
            } catch (InvalidTokenOffsetsException e) {
                System.out.println(title);
            }
            TokenStream contentTokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), id, "content", analyzer);
            try {
                content = highlighter.getBestFragments(contentTokenStream, content, 3, "...");
            } catch (InvalidTokenOffsetsException e) {
                System.out.println(content);
            }
            results.documents[i] = new SearchDocument(title, content, url);
        }
        return results;
    }


    public static void main(String[] args) throws IOException {
        Scanner scan = new Scanner(System.in);
        THUSearcher thuSearcher = new THUSearcher(args[0], new BM25Similarity());
        String query = scan.nextLine();
        if (query.length() > 1) {
            SearchResults topDocs = thuSearcher.searchQuery(query, 0, 10);
            System.out.println(topDocs.total);
            for (int i = 0; i < topDocs.documents.length; ++i) {
                SearchDocument document = topDocs.documents[i];
                System.out.println("title=" + document.title + " content=" + document.content + " url=" + document.url);
            }
        } else {
            for (int i = 1; i < 100; ++i) {
                System.out.println("doc=" + thuSearcher.searcher.doc(i));
            }
        }
    }
}
