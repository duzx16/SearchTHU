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
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


public class THUIndexer {
    private Analyzer analyzer;
    private IndexWriter indexWriter;
    private Map<String, Set<String>> anchorMap;

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
                if (f.getName().endsWith("html.json")) {
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
        final JsonParser parser = new JsonParser();
        final JsonElement jsonElement = parser.parse(new FileReader(file_path));
        final JsonObject jsonObject = jsonElement.getAsJsonObject();
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
                if (!anchorMap.containsKey(link)) {
                    anchorMap.put(link, new HashSet<>());
                }
                anchorMap.get(link).add(anchor);
            }
        }
    }

    private void indexJsonFile(String file_path, String url) {
        try {
            Document doc = new Document();
            final JsonParser parser = new JsonParser();
            final JsonElement jsonElement = parser.parse(new FileReader(file_path));
            final JsonObject jsonObject;
            try {
                jsonObject = jsonElement.getAsJsonObject();
            } catch (Exception e) {
                System.out.println(file_path);
                throw e;
            }
            for (final Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                final String key = entry.getKey();
                final JsonElement value = entry.getValue();
                switch (key) {
                    case "title":
                    case "content":
                    case "h1":
                    case "h2":
                    case "h3":
                    case "h4":
                    case "h5":
                    case "h6":
                        doc.add(new TextField(key, value.getAsString(), Field.Store.YES));
                        break;
                }
                if (anchorMap.containsKey(url)) {
                    doc.add(new TextField("anchor", String.join(" ", anchorMap.get(url).toArray(
                            new String[0])), Field.Store.YES));
                }
            }
            doc.add(new TextField("url", url, Field.Store.YES));
            indexWriter.addDocument(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
//        URL url = new URL("http://www.tsinghua.edu.cn");
//        System.out.println(url.getPath());
        THUIndexer indexer = new THUIndexer(args[1]);
        indexer.indexDirectory(args[0], "", true);
        System.out.println("Build anchor finish");
        indexer.indexDirectory(args[0], "", false);
        indexer.indexWriter.close();
    }
}
