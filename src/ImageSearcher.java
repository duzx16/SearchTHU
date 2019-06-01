import java.io.*;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.Weight;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.wltea.analyzer.lucene.IKAnalyzer;

public class ImageSearcher {
    private IndexReader reader;
    private IndexSearcher searcher;
    private Analyzer analyzer;
    private float avgLength = 1.0f;

    public ImageSearcher(String indexdir) {
        analyzer = new IKAnalyzer();
        try {
            System.out.println("Index " + indexdir);
            reader = IndexReader.open(FSDirectory.open(new File(indexdir)));
            searcher = new IndexSearcher(reader);
            searcher.setSimilarity(new SimpleSimilarity());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TopDocs searchQuery(String queryString, String field, int maxnum) {
        try {
            Term term = new Term(field, queryString);
            Query query = new SimpleQuery(term, avgLength);
            query.setBoost(1.0f);
            //Weight w=searcher.createNormalizedWeight(query);
            //System.out.println(w.getClass());
            TopDocs results = searcher.search(query, maxnum);
            System.out.println(results);
            return results;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Document getDoc(int docID) {
        try {
            return searcher.doc(docID);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void loadGlobals(String filename) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
            String line = reader.readLine();
            avgLength = Float.parseFloat(line);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public float getAvg() {
        return avgLength;
    }

    public static void main(String[] args) {
        ImageSearcher search = new ImageSearcher("forIndex/index");
        search.loadGlobals("forIndex/global.txt");
        System.out.println("avg length = " + search.getAvg());

        TopDocs results = search.searchQuery("�����", "abstract", 100);
        ScoreDoc[] hits = results.scoreDocs;
        for (int i = 0; i < hits.length; i++) { // output raw format
            Document doc = search.getDoc(hits[i].doc);
            System.out.println("doc=" + hits[i].doc + " score="
                    + hits[i].score + " picPath= " + doc.get("picPath"));
        }
    }
}
