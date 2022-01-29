import io.rsug.cheptsa.ChServlet

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class TestServlet extends HttpServlet {
    ChServlet servlet

    TestServlet(ChServlet s) {
        this.servlet = s
    }

    @Override
    protected void service(HttpServletRequest request,
                           HttpServletResponse response) throws IOException {
        ChServlet n2 = servlet.clone()
        n2.parse(request)
        String text = request.inputStream?.text
        if (n2.method == "GET")
            n2.doGET()
        else if (n2.method == "POST") {
            n2.doPOST(text)
        } else {
            response.setStatus(400)
            response.setContentType("text/plain")
            response.setCharacterEncoding("utf-8")
            response.writer.println("Wrong method")
            return
        }
        response.setStatus(n2.outRC)
        n2.outHeaders.each { k, v -> response.setHeader(k, v) }
        response.setCharacterEncoding("utf-8")
        if (n2.outStream != null) {
            byte[] buf = new byte[16384]
            int n = n2.outStream.read(buf)
            while (n > 0) {
                response.outputStream.write(buf, 0, n)
                n = n2.outStream.read(buf)
            }
            n2.outStream.close()
        } else
            response.writer.print(n2.out.toString())
    }
}
