package index;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.lucene.document.*;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jgrapht.Graph;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;


public class THUIndexer {
    private Analyzer analyzer;
    private IndexWriter indexWriter;
    private Map<String, Set<String>> anchorMap;
    private Graph<String, DefaultEdge> linkGraph;
    private Map<String, Double> pagerank_scores;
    private String[] must_fields;

    public THUIndexer(String indexDir) {
        anchorMap = new HashMap<>();
        analyzer = new SmartChineseAnalyzer();
        try {
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            Path indexPath = Paths.get(indexDir);
            if (!Files.exists(indexPath)) {
                Files.createDirectory(indexPath);
            }
            Directory dir = FSDirectory.open(indexPath);
            indexWriter = new IndexWriter(dir, iwc);
            indexWriter.commit();
        } catch (IOException e) {
            e.printStackTrace();
        }
        linkGraph = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
        must_fields = new String[]{"title", "content"};
    }

    public void indexDirectory(String dir_path, String url, boolean anchor) throws FileNotFoundException, MalformedURLException {
        File dir = new File(dir_path);
        if (!url.isEmpty()) {
            url = url + "/";
        }
        for (File f : Objects.requireNonNull(dir.listFiles())) {
            if (f.isDirectory()) {
                this.indexDirectory(Paths.get(dir_path, f.getName()).toString(), url + f.getName(), anchor);
            } else {
                if (f.getName().endsWith(".json")) {
                    String sub_url = url + f.getName().substring(0, f.getName().length() - 5);
                    if (anchor) {
                        saveAnchorFile(Paths.get(dir_path, f.getName()).toString(), sub_url);
                    } else {
                        indexJsonFile(Paths.get(dir_path, f.getName()).toString(), sub_url);
                    }
                }
            }
        }
    }

    private void saveAnchorFile(String file_path, String url_str) throws FileNotFoundException, MalformedURLException {
        URL url = new URL("http://" + url_str);
        final JsonObject jsonObject;
        try {
            final JsonParser parser = new JsonParser();
            final JsonElement jsonElement = parser.parse(new FileReader(file_path));
            jsonObject = jsonElement.getAsJsonObject();
        } catch (IllegalStateException e) {
            System.out.println(file_path);
            return;
        }
        if (jsonObject.has("links")) {
            JsonArray links = jsonObject.getAsJsonArray("links");
            for (JsonElement element : links) {
                JsonObject linkObject = element.getAsJsonObject();
                String anchor = linkObject.get("text").getAsString();
                String link = linkObject.get("href").getAsString();
                if (link.startsWith("#") || link.startsWith("javascript")) {
                    link = url_str;
                } else if (link.startsWith("/")) {
                    link = url.getHost() + link;
                } else {
                    try {
                        URL link_url = new URL(link);
                        String path = link_url.getPath();
                        if (path.isEmpty() || path.equals("/")) {
                            path = "/index.html";
                        }
                        link = link_url.getHost() + path;
                    } catch (MalformedURLException e) {
                    }
                }
                int pos = link.lastIndexOf('#');
                if (pos != -1) {
                    link = link.substring(0, pos);
                }
                if (!linkGraph.containsVertex(url_str)) {
                    linkGraph.addVertex(url_str);
                }
                if (!linkGraph.containsVertex(link)) {
                    linkGraph.addVertex(link);
                }
                linkGraph.addEdge(url_str, link);
                if (!anchorMap.containsKey(link)) {
                    anchorMap.put(link, new HashSet<>());
                }
                anchorMap.get(link).add(anchor);
            }
        }
    }

    private void indexJsonFile(String file_path, String url_str) {
        URL url = null;
        try {
            url = new URL("http://" + url_str);
        } catch (MalformedURLException e) {
            System.out.println(url_str);
        }
        try {
            Document doc = new Document();
            Path path = Paths.get(file_path);
            final JsonObject jsonObject;
            try {
                final JsonParser parser = new JsonParser();
                final JsonElement jsonElement = parser.parse(new FileReader(file_path));
                jsonObject = jsonElement.getAsJsonObject();
            } catch (IllegalStateException e) {
                System.out.println(file_path);
                return;
            }
            for(String key: must_fields) {
                String text = "";
                if(jsonObject.has(key)) {
                    text = jsonObject.get(key).getAsString();
                }
                doc.add(new TextField(key, text, Field.Store.YES));
            }
            for (final Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                final String key = entry.getKey();
                final JsonElement value = entry.getValue();
                switch (key) {
                    case "title":
                    case "content":
                        break;
                    case "h1":
                    case "h2":
                    case "h3":
                    case "h4":
                    case "h5":
                    case "h6":
                        doc.add(new TextField(key, value.getAsString(), Field.Store.YES));
                        break;
                }
            }
            if (anchorMap.containsKey(url_str)) {
                doc.add(new TextField("anchor", String.join(" ", anchorMap.get(url_str).toArray(
                        new String[0])), Field.Store.YES));
            }
            float pagerank = 0.0f;
            if (pagerank_scores.containsKey(url_str)) {
                pagerank = pagerank_scores.get(url_str).floatValue();
                System.out.println(url_str + pagerank);
            }
            doc.add(new FeatureField("features", "pagerank", pagerank));
            if (url != null) {
                doc.add(new StringField("site", url.getHost(), Field.Store.YES));
            }
            doc.add(new StoredField("url", url_str));
            String file_type;
            if (file_path.endsWith(".html.json") || file_path.endsWith(".htm.json")) {
                file_type = "html";
            } else if (file_path.endsWith(".doc.json") || file_path.endsWith("docx.json")) {
                file_type = "doc";
            } else if (file_path.endsWith("pdf.json")) {
                file_type = "pdf";
            } else {
                file_type = "";
            }
            doc.add(new StringField("type", file_type, Field.Store.YES));
            indexWriter.addDocument(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        THUIndexer indexer = new THUIndexer(args[1]);
        indexer.indexDirectory(args[0], "", true);
        PageRank<String, DefaultEdge> pageRank = new PageRank<>(indexer.linkGraph);
        indexer.pagerank_scores = pageRank.getScores();
        System.out.println("Build anchor finish");
        indexer.indexDirectory(args[0], "", false);
        indexer.indexWriter.close();
    }
}
