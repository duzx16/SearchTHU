import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FeatureField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.FSDirectory;


import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class THUSearcher {
    protected IndexReader reader;
    private IndexSearcher searcher;
    protected Analyzer analyzer;
    private SimpleQueryParser parser;
    private QueryParser advanced_parser;
    private Map<String, Float> field_weights = new HashMap<>();
    private SimpleHTMLFormatter htmlFormatter;
    private SpellChecker spellChecker;

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
        Path spellIndex = Paths.get("/home/duzx16/SearchTHU/spell_check");
        spellChecker = new SpellChecker(FSDirectory.open(spellIndex));

        spellChecker.indexDictionary(new LuceneDictionary(reader, "title"), new IndexWriterConfig(analyzer), true);
        spellChecker.indexDictionary(new LuceneDictionary(reader, "anchor"), new IndexWriterConfig(analyzer), true);
        spellChecker.indexDictionary(new LuceneDictionary(reader, "content"), new IndexWriterConfig(analyzer), true);

    }

    private String queryCheck(String query) throws IOException {
        String[] results = spellChecker.suggestSimilar(query, 100);
        return results[0];
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
            System.out.println(document.getFields());
            String title = document.get("title");
            String content = document.get("content");
            String url = document.get("url");
            TokenStream titleTokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), id, "title",
                    analyzer);
            try {
                String highlight_title = highlighter.getBestFragment(titleTokenStream, title);
                if (highlight_title != null && !highlight_title.isEmpty()) {
                    title = highlight_title;
                }
            } catch (InvalidTokenOffsetsException e) {
                System.out.println(title);
            }
            TokenStream contentTokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), id, "content",
                    analyzer);
            try {
                String highlight_content = highlighter.getBestFragments(contentTokenStream, content, 3, "...");
                if (highlight_content != null && !highlight_content.isEmpty()) {
                    content = highlight_content;
                } else {
                    if (content.length() > 200) {
                        content = content.substring(0, 200);
                    }
                }
            } catch (InvalidTokenOffsetsException e) {
                System.out.println(content);
            }
            results.documents[i] = new SearchDocument(title, content, url);
        }
        results.total = (int) topDocs.totalHits.value;
        return results;
    }

    private Query addPageRank(Query query) {
        Query pagerank = FeatureField.newSaturationQuery("features", "pagerank");
        Query final_query = new BooleanQuery.Builder().add(query, BooleanClause.Occur.MUST).add(pagerank,
                BooleanClause.Occur.SHOULD).build();
        return final_query;
    }

    public SearchResults searchQuery(String query_str, int offset, int max_num) throws IOException {
        String correct = queryCheck(query_str);
        Query query = parser.parse(query_str);
        TopDocs topDocs = searcher.search(query, offset + max_num);
        query = addPageRank(query);
        SearchResults results = highlightResult(query, topDocs, offset, max_num);
        if (!spellChecker.exist(query_str)) {
            results.correct = correct;
        }
        return results;
    }

    public SearchResults advancedSearch(String exactMatch, String anyMatch, String noneMatch, String position,
                                        String site, String file_type, int offset, int max_num) throws IOException {
        Vector<String> queries = new Vector<>();
        BooleanQuery.Builder query_builder = new BooleanQuery.Builder();
        if (site != null) {
            query_builder.add(new TermQuery(new Term("site", site)), BooleanClause.Occur.MUST);
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
        final_query = addPageRank(final_query);
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
