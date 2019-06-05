import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

import java.io.FileReader;
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
    private SimpleQueryParser anchor_parser;
    private Map<String, Float> field_weights = new HashMap<>();
    private SimpleHTMLFormatter htmlFormatter;

    public THUSearcher(String indexDir, Similarity similarity, String config_path) throws IOException {
        final JsonParser json_parser = new JsonParser();
        final JsonElement jsonElement = json_parser.parse(new FileReader(config_path));
        final JsonObject jsonObject = jsonElement.getAsJsonObject().getAsJsonObject("field_weights");
        for (final Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            final String key = entry.getKey();
            final float value = entry.getValue().getAsFloat();
            field_weights.put(key, value);
        }
        analyzer = new SmartChineseAnalyzer();
        Path indexPath = Paths.get(indexDir);
        reader = DirectoryReader.open(FSDirectory.open(indexPath));
        searcher = new IndexSearcher(reader);
        searcher.setSimilarity(similarity);
        parser = new SimpleQueryParser(analyzer, field_weights);
        anchor_parser = new SimpleQueryParser(analyzer, "anchor");
        // highlighter
        htmlFormatter = new SimpleHTMLFormatter("<span class=\"highlight\">", "</span>");
    }

    public SearchResults searchQuery(String query_str, int offset, int max_num) throws IOException {
        Query query = parser.parse(query_str);
        TopDocs topDocs = searcher.search(query, offset + max_num);
        SearchResults results = new SearchResults();
        if(topDocs.scoreDocs.length < offset + max_num) {
            max_num = topDocs.scoreDocs.length - offset;
        }
        results.documents = new SearchDocument[max_num];
        Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
        for (int i = 0; i < max_num; ++i) {
            int id = topDocs.scoreDocs[i + offset].doc;
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
        results.total = (int) topDocs.totalHits.value;
        return results;
    }


    public static void main(String[] args) throws IOException {
        Scanner scan = new Scanner(System.in);
        THUSearcher thuSearcher = new THUSearcher(args[0], new BM25Similarity(), args[1]);
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
