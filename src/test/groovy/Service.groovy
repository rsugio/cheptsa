import io.rsug.cheptsa.ChServlet
import org.apache.camel.Exchange
import org.apache.olingo.odata2.api.edm.provider.Schema
import org.apache.olingo.odata2.core.edm.provider.EdmxProvider
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.log.Log
import org.eclipse.jetty.util.log.StdErrLog
import org.junit.Test

import javax.mail.internet.MimeUtility
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.regex.Matcher
import java.util.regex.Pattern

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

class Service {
    @Test
    void simpledimple() {
        String root = "/http/viewer"
        Server server = new Server(8090)
        server.setStopAtShutdown(true)
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS)
        context.setContextPath("/")
        ChServlet n = new ChServlet(root, null, true)
        context.addServlet(new ServletHolder(new TestServlet(n)), "$root/*")
        server.setHandler(context)
        server.start()
        server.join()
    }

    @Test
    void formats() {
        println(URLDecoder.decode("%D0%90%D0%9F%D0%98", "UTF-8"))
        ZonedDateTime zdt = ZonedDateTime.ofInstant(new Date().toInstant(), ZoneId.of("Europe/Moscow"))
        assert zdt.toOffsetDateTime()
        String s = "input1=A123%26456%3E%3C&input2=222222222222"
        assert ChServlet.parseQuery(s) == ["input1": "A123&456><", "input2": "222222222222"]
        String from, encoded
        from = "а= пробел "
        encoded = URLEncoder.encode(from, "UTF-8")
        assert encoded == "%D0%B0%3D+%D0%BF%D1%80%D0%BE%D0%B1%D0%B5%D0%BB+"
        assert ChServlet.parseQuery(encoded) == ["а": " пробел "]
        ChServlet.getNodeId("ID-vsa9306905-1643290080486-126-2") == "vsa9306905"
        println(ChServlet.parseQuery("SAP_MessageProcessingLogID=AGHz0I-yF-bSgcvTvwTGM7g-b6YH&bundleSymbolicName=NLMK__Receive_Reports"))
    }

    @Test
    void edmx() {
        InputStream e = Paths.get("C:\\workspace\\task_odata_translator\\SF_Dev_Metadata.xml").newInputStream()
        EdmxProvider p = new EdmxProvider()
        p.parse(e, true)
        p.schemas.each { Schema sh ->
            sh.entityTypes.each { et ->

            }
        }
    }

    @Test
    void parseNodeId() {

    }
}
