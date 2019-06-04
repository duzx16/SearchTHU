import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
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
    }

    public TopDocs searchQuery(String query_str, int max_num) throws IOException {
        Query query = parser.parse(query_str);
        return searcher.search(query, max_num);
    }

    public static void main(String[] args) throws IOException {
        Scanner scan = new Scanner(System.in);
        THUSearcher thuSearcher = new THUSearcher(args[0], new BM25Similarity());
        String query = scan.nextLine();
        if(query.length() > 1) {
            TopDocs topDocs = thuSearcher.searchQuery(query, 10);
            System.out.println(topDocs.totalHits);
            for(int i = 0;i < topDocs.scoreDocs.length; ++i) {
                Document document = thuSearcher.searcher.doc(topDocs.scoreDocs[i].doc);
                System.out.println("doc=" + document + " score="+ topDocs.scoreDocs[i].score);
            }
        }
        else {
            for (int i = 1; i < 100; ++i) {
                System.out.println("doc=" + thuSearcher.searcher.doc(i));
            }
        }
    }
}
