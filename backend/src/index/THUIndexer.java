package index;

import java.io.*;
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

    public THUIndexer(String indexDir) {
        analyzer = new SmartChineseAnalyzer();
        try {
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            Path indexPath = Paths.get(indexDir);
            if (!Files.exists(indexPath)) {
                Files.createDirectory(indexPath);
            }
            Directory dir = FSDirectory.open(indexPath);
            indexWriter = new IndexWriter(dir, iwc);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void indexDirectory(String dir_path, String url) {
        File dir = new File(dir_path);
        for (File f : Objects.requireNonNull(dir.listFiles())) {
            if (f.isDirectory()) {
                this.indexDirectory(Paths.get(dir_path, f.getName()).toString(), url + "/" + f.getName());
            } else {
                if (f.getName().endsWith("html.json")) {
                    indexJsonFile(Paths.get(dir_path, f.getName()).toString(), url + "/" + f.getName().substring(0, f.getName().length() - 5));
                }

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
            }
            catch (Exception e) {
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
                    case "links":
                        final JsonArray jsonArray = value.getAsJsonArray();
                        for(JsonElement element: jsonArray) {
                            JsonObject linkObject = element.getAsJsonObject();
                            String anchor = linkObject.get("text").getAsString();
                            doc.add(new TextField("anchor", anchor, Field.Store.YES));
                        }
                        break;
                }
            }
            doc.add(new StoredField("url", url));
            indexWriter.addDocument(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        THUIndexer indexer = new THUIndexer(args[1]);
        indexer.indexDirectory(args[0], "");
    }
}
