import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import com.google.gson.Gson;

@WebServlet(name = "THUServer")
public class THUServer extends HttpServlet {
    public static final int PAGE_RESULT = 10;
    public static final String indexDir = "/home/duzx16/SearchTHU/test";
    private THUSearcher searcher;
    private Gson gson;

    public THUServer() throws IOException {
        super();
        searcher = new THUSearcher(indexDir, new BM25Similarity(), "configuration.json");
        gson = new Gson();
    }

    public ScoreDoc[] showList(ScoreDoc[] results, int page) {
        if (results == null || results.length < (page - 1) * PAGE_RESULT) {
            return null;
        }
        int start = Math.max((page - 1) * PAGE_RESULT, 0);
        int docnum = Math.min(results.length - start, PAGE_RESULT);
        ScoreDoc[] ret = new ScoreDoc[docnum];
        for (int i = 0; i < docnum; i++) {
            ret[i] = results[start + i];
        }
        return ret;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=utf-8");
        request.setCharacterEncoding("utf-8");
        PrintWriter out = response.getWriter();
        String queryString = request.getParameter("query");
        String pageString = request.getParameter("page");
        int page = 1;
        if (pageString != null) {
            page = Integer.parseInt(pageString);
        }
        System.out.println(queryString);
        if (queryString == null) {
            System.out.println("null query");
            //request.getRequestDispatcher("/Image.jsp").forward(request, response);
        } else {
            int offset = PAGE_RESULT * (page - 1);
            SearchResults results = searcher.searchQuery(queryString, offset, PAGE_RESULT);
            out.write(gson.toJson(results));
        }
        out.close();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doGet(request, response);
    }
}
