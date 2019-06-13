import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.BM25Similarity;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.Gson;
import org.apache.lucene.store.FSDirectory;

@WebServlet(name = "THUServer")
public class THUServer extends HttpServlet {
    private static final int RESULTS_PER_PAGE = 10;
    private static final String indexDir = "/home/duzx16/SearchTHU/test";
    private static final String suggestDir = "/home/duzx16/SearchTHU/suggest";
    private THUSearcher searcher;
    private THUSuggester suggester;
    private Gson gson;

    public THUServer() throws IOException {
        super();
        searcher = new THUSearcher(indexDir, new BM25Similarity(), "configuration.json");
        gson = new Gson();
        suggester = new THUSuggester(FSDirectory.open(Paths.get(suggestDir)), searcher.analyzer, searcher.reader);
    }

    public ScoreDoc[] showList(ScoreDoc[] results, int page) {
        if (results == null || results.length < (page - 1) * RESULTS_PER_PAGE) {
            return null;
        }
        int start = Math.max((page - 1) * RESULTS_PER_PAGE, 0);
        int docnum = Math.min(results.length - start, RESULTS_PER_PAGE);
        ScoreDoc[] ret = new ScoreDoc[docnum];
        for (int i = 0; i < docnum; i++) {
            ret[i] = results[start + i];
        }
        return ret;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        response.setContentType("application/json;charset=utf-8");
        request.setCharacterEncoding("utf-8");
        String pageString = request.getParameter("page");
        if (request.getServletPath().endsWith("_query")) {
            int page = 1;
            if (pageString != null) {
                page = Integer.parseInt(pageString);
            }
            int offset = RESULTS_PER_PAGE * (page - 1);
            if (request.getServletPath().equals("/servlet/THUSearch/_query")) {
                PrintWriter out = response.getWriter();
                String queryString = request.getParameter("query");

                System.out.println(queryString);
                System.out.println(request.getServletPath());
                if (queryString == null) {
                    System.out.println("null query");
                    //request.getRequestDispatcher("/Image.jsp").forward(request, response);
                } else {
                    SearchResults results = searcher.searchQuery(queryString, offset, RESULTS_PER_PAGE);
                    out.write(gson.toJson(results));
                }
                out.close();
            } else if (request.getServletPath().equals("/servlet/THUSearch/_advanced_query")) {
                PrintWriter out = response.getWriter();
                String exactMatch = request.getParameter("exact");
                String anyMatch = request.getParameter("any");
                String noneMatch = request.getParameter("none");
                String position = request.getParameter("position");
                if (position != null) {
                    switch (position) {
                        case "any":
                        case "title":
                        case "content":
                            break;
                        case "link":
                            position = "anchor";
                            break;
                        default:
                            System.out.println("Invalid position: " + position);
                            position = "any";
                            break;
                    }
                } else {
                    position = "any";
                }
                String site = request.getParameter("site");
                String file_type = request.getParameter("file_type");
                if (file_type != null) {
                    switch (file_type) {
                        case "any":
                        case "pdf":
                        case "html":
                            break;
                        case "doc/docx":
                            file_type = "doc";
                            break;
                        default:
                            System.out.println("Invalid file type: " + file_type);
                            file_type = "any";
                            break;
                    }
                } else {
                    file_type = "any";
                }
                SearchResults results = searcher.advancedSearch(exactMatch, anyMatch, noneMatch, position, site,
                        file_type, offset, RESULTS_PER_PAGE);
                out.write(gson.toJson(results));
                out.close();
            }
        } else if (request.getServletPath().equals("/servlet/THUSearch/_completion")) {
            String query = request.getParameter("query");
            if (query != null) {
                SuggestResults results = suggester.suggest(query);
                PrintWriter out = response.getWriter();
                out.write(gson.toJson(results));
                out.close();
            }

        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        this.doGet(request, response);
    }
}
