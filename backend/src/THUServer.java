import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;

@WebServlet(name = "THUServer")
public class THUServer extends HttpServlet {
    public static final int PAGE_RESULT = 10;
    public static final String indexDir = "forIndex";
    public static final String picDir = "";

    public THUServer() {
        super();
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
        response.setContentType("text/html;charset=utf-8");
        request.setCharacterEncoding("utf-8");
        String queryString = request.getParameter("query");
        String pageString = request.getParameter("page");
        int page = 1;
        if (pageString != null) {
            page = Integer.parseInt(pageString);
        }
        if (queryString == null) {
            System.out.println("null query");
            //request.getRequestDispatcher("/Image.jsp").forward(request, response);
        } else {
            System.out.println(queryString);
            System.out.println(URLDecoder.decode(queryString, "utf-8"));
            System.out.println(URLDecoder.decode(queryString, "gb2312"));
            String[] tags = null;
            String[] paths = null;
            request.setAttribute("currentQuery", queryString);
            request.setAttribute("currentPage", page);
            request.setAttribute("imgTags", tags);
            request.setAttribute("imgPaths", paths);
            request.getRequestDispatcher("/imageshow.jsp").forward(request,
                    response);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doGet(request, response);
    }
}
