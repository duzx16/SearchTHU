import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class THUSearcher {
    private IndexReader reader;
    private IndexSearcher searcher;
    private Analyzer analyzer;
    private SimpleQueryParser parser;
    private QueryParser advanced_parser;
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
        advanced_parser = new QueryParser("content", analyzer);
        // highlighter
        htmlFormatter = new SimpleHTMLFormatter("<span class=\"highlight\">", "</span>");
    }

    private SearchResults highlightResult(Query query, TopDocs topDocs, int offset, int max_num) throws IOException {
        SearchResults results = new SearchResults();
        if (topDocs.scoreDocs.length < offset + max_num) {
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

    public SearchResults searchQuery(String query_str, int offset, int max_num) throws IOException {
        Query query = parser.parse(query_str);
        TopDocs topDocs = searcher.search(query, offset + max_num);
        SearchResults results = highlightResult(query, topDocs, offset, max_num);
        return results;
    }

    public SearchResults advancedSearch(String exactMatch, String anyMatch, String noneMatch, String position, String site, String file_type, int offset, int max_num) throws IOException {
        Vector<String> queries = new Vector<>();
        BooleanQuery.Builder query_builder = new BooleanQuery.Builder();
        query_builder.setMinimumNumberShouldMatch(1);
        // todo: this is not right
        if (site != null) {
            query_builder.add(new TermQuery(new Term("url", site)), BooleanClause.Occur.MUST);
            query_builder.setMinimumNumberShouldMatch(2);
        }
        if (!file_type.equals("any")) {
            queries.add(String.format("type: \"%s\"", file_type));
        }
        String[] fileds;
        if (position.equals("any")) {
            fileds = new String[]{"title", "content", "anchor"};
        } else {
            fileds = new String[]{position};
        }
        Vector<String> fields_queries = new Vector<>();
        Vector<String> none_queries = new Vector<>();
        for (String field : fileds) {
            Vector<String> field_queries = new Vector<>();
            if (exactMatch != null) {
                field_queries.add(String.format("\"%s\"", exactMatch));
            }
            if (anyMatch != null) {
                for (String any : anyMatch.split(" ")) {
                    field_queries.add(String.format("\"%s\"", any));
                }
            }
            fields_queries.add(String.format("%s: (%s)", field, String.join(" OR ", field_queries)));
        }
        none_queries.add(String.format("(%s)", String.join(" OR ", fields_queries)));
        if (noneMatch != null) {
            for (String field : fileds) {
                for (String none : noneMatch.split(" ")) {
                    none_queries.add(String.format("%s: \"%s\"", field, none));
                }
            }
        }
        queries.add(String.format("(%s)", String.join(" NOT ", none_queries)));
        String query_str = String.join(" AND ", queries);
        System.out.println(query_str);
        Query query;
        try {
            query = advanced_parser.parse(query_str);
            System.out.println(query);
        } catch (ParseException e) {
            System.out.println(e.expectedTokenSequences);
            return null;
        }
        query_builder.add(query, BooleanClause.Occur.MUST);
        Query final_query = query_builder.build();
        TopDocs topDocs = searcher.search(final_query, offset + max_num);
        return highlightResult(query, topDocs, offset, max_num);
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
