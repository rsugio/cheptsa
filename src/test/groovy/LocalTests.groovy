import com.sap.it.api.msglog.MessageLog
import com.sap.it.op.agent.mpl.MonitoringHeaders
import groovy.util.slurpersupport.NodeChild
import io.rsug.cheptsa.ChServlet
import org.apache.aries.blueprint.ComponentDefinitionRegistry
import org.apache.aries.blueprint.NamespaceHandler
import org.apache.aries.blueprint.container.BlueprintContainerImpl
import org.apache.aries.blueprint.container.BlueprintRepository
import org.apache.aries.blueprint.parser.ComponentDefinitionRegistryImpl
import org.apache.aries.blueprint.parser.NamespaceHandlerSet
import org.apache.aries.blueprint.parser.Parser
import org.apache.aries.blueprint.reflect.ComponentMetadataImpl
import org.apache.aries.util.AriesFrameworkUtil
import org.apache.camel.MessageHistory
import org.apache.olingo.odata2.api.edm.provider.Schema
import org.apache.olingo.odata2.core.edm.provider.EdmxProvider
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.junit.Test
import org.osgi.service.blueprint.container.BlueprintContainer
import org.xml.sax.SAXException

import java.nio.file.Paths
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.regex.Matcher
import java.util.regex.Pattern


class LocalTests {
    class Script {
        String id // CallActivity_5
        String longId // CallActivity_5_63532260388019
        String scriptBundleId // если скрипты отдельно от потока
        String functionName
        String scriptFile, scriptFileType
        String toString() {
            "Script[$id]=[$functionName,$scriptFile,$scriptBundleId]"
        }
    }

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

        //println(ChServlet.parseQuery("SAP_MessageProcessingLogID=AGHz0I-yF-bSgcvTvwTGM7g-b6YH&bundleSymbolicName=NLMK__Receive_Reports"))
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
    void source() {
        Map pr = [:], pr2 = [:]
        com.sap.it.op.agent.mpl.ExchangePropertyHeaderNames.declaredFields.each { f ->
            pr[f.name] = com.sap.it.op.agent.mpl.ExchangePropertyHeaderNames."${f.name}"
        }
        println(MonitoringHeaders.declaredFields)

    }

    @Test
    void "гляделка"() {
        MessageLog a
        com.sap.it.op.agent.mpl.ExchangePropertyHeaderNames b
        MonitoringHeaders mh
    }

    @Test
    void bp() {
        InputStream beans = getClass().getResourceAsStream("beans.xml")
        Parser p = new Parser()
        p.parse(beans)
        ComponentDefinitionRegistry reg = new ComponentDefinitionRegistryImpl()

        NamespaceHandlerSet n = new NamespaceHandlerSet() {
            @Override
            Set<URI> getNamespaces() {
                return null
            }

            @Override
            boolean isComplete() {
                return false
            }

            @Override
            NamespaceHandler getNamespaceHandler(URI uri) {
                return null
            }

            @Override
            javax.xml.validation.Schema getSchema() throws SAXException, IOException {
                return null
            }

            @Override
            javax.xml.validation.Schema getSchema(Map<String, String> map) throws SAXException, IOException {
                return null
            }

            @Override
            void addListener(NamespaceHandlerSet.Listener listener) {

            }

            @Override
            void removeListener(NamespaceHandlerSet.Listener listener) {

            }

            @Override
            void destroy() {

            }
        }
        p.populate(n, reg)



    }

        @Test
    void mapScripts() {
        InputStream beans = getClass().getResourceAsStream("beans.xml")
        NodeChild blueprint = new XmlSlurper(false, true).parse(beans)
        assert blueprint.name() == "blueprint" && blueprint.namespaceURI() == "http://www.osgi.org/xmlns/blueprint/v1.0.0"
        String id = blueprint.camelContext.@id.text()
        println(id)

        // Map<RouteID,Map<ScriptID,Script>
        Map<String, List<Script>> scripts = [:]

        blueprint.camelContext.route.each { cc ->
            List<NodeChild> route = []
            String routeId = cc.@id.text()
            scripts[routeId] = []
            cc.'*'.each { NodeChild nc ->
                route.add(nc)
                if (nc.name() == "bean" && nc.@method.text() == "process" && nc.@ref.text() == "scriptprocessor") {
                    // ищем назад до setHeader[@headerName=='headerName']
                    Script script = new Script()
                    for (int i = route.size() - 2; i >= 0; i--) {
                        NodeChild m = route[i]
                        if (m.name() != "setHeader")
                            break
                        String headerName = m.@headerName.text()
                        String constant = m.constant.text()
                        script."$headerName" = constant
                        if (headerName == "scriptFile") {
                            script.longId = m.@id.text()
                            break
                        }
                    }
                    scripts[routeId].add(script)
                }
            }
        }
        println(scripts)

        InputStream iflow = getClass().getResourceAsStream("io.rsug.cheptsa.gists.iflw.xml")
        NodeChild bpmn = new XmlSlurper(false, true).parse(iflow)
        assert bpmn.name() == "definitions" && bpmn.namespaceURI() == "http://www.omg.org/spec/BPMN/20100524/MODEL"
        bpmn.process.each {NodeChild p ->
            println(p.@id.text())
        }
    }

    static Map<String, String> scriptProperties(List<MessageHistory> mhl) {
        Pattern cpiKubikId = Pattern.compile("^([A-z]+_\\d+)_\\d+\$")
        MessageHistory mh = mhl.reverse().find { it.node.label == "setHeader[scriptFile]" }
        // callActivity == CallActivity_5_1384130749863957
        String callActivity = mh?.node.id
        // routeId == Process_1234
        String routeId = mh?.routeId
        Map<String, String> rez = ["CallActivityRaw": callActivity, "routeId": routeId]
        Matcher m = cpiKubikId.matcher(callActivity)
        if (m.matches()) {
            rez.CallActivity = m.group(1)   // CallActivity_5
        }
        return rez
    }

}
