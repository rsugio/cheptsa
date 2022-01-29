import io.rsug.cheptsa.ChServlet
import org.apache.olingo.odata2.api.edm.provider.Schema
import org.apache.olingo.odata2.core.edm.provider.EdmxProvider
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.junit.Test

import java.nio.file.Paths
import java.time.ZoneId
import java.time.ZonedDateTime

class LocalTests {
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

        // формат vsa это Нео, гуид это КФ
        ChServlet.getNodeId("ID-vsa9306905-1643290080486-126-2") == "vsa9306905"
        ChServlet.getNodeId("ID-65d7feb1-b6bc-4494-515a-ad57-1643395616303-4-1") == "65d7feb1-b6bc-4494-515a-ad57"

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

}
