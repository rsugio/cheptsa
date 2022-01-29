package io.rsug.cheptsa

import com.sap.it.nm.spi.node.NodeHabitat
import com.sap.it.nm.spi.store.access.ArtifactAccess
import com.sap.it.nm.spi.store.access.TaskAccess
import com.sap.it.nm.types.deploy.Artifact
import com.sap.it.nm.types.deploy.ArtifactType
import com.sap.it.op.component.check.ServiceComponentRuntimeAccess
import com.sap.it.op.mpl.loglevel.DebugLogLevelConfigurationProvider
import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.spi.InflightRepository
import org.apache.camel.spi.Registry
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.utils.IOUtils
import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.FrameworkUtil
import org.osgi.framework.ServiceReference

import javax.mail.internet.MimeUtility
import javax.servlet.http.HttpServletRequest
import java.nio.ByteBuffer
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.regex.Matcher
import java.util.regex.Pattern

class ChServlet {
    enum Action {
        None, Version, Bundle, Download, Tar, TestBedNeo, TestBedCF, Inflight, Murzilka, Cancel, TestForm, Unknown
    }

    class IR {
        Bundle bundle
        CamelContext ctx
        Exchange exchange
        String SAP_MessageProcessingLogID
        String nodeId, cpiNode
        Date CamelCreatedTimestamp

        IR(Bundle b, InflightRepository.InflightExchange iex) {
            bundle = b
            exchange = iex.exchange
            ctx = exchange.context
            SAP_MessageProcessingLogID = exchange.getProperty("SAP_MessageProcessingLogID")
            nodeId = iex.nodeId
            cpiNode = getNodeId(exchange.exchangeId)
            CamelCreatedTimestamp = exchange.properties.CamelCreatedTimestamp as Date
        }
    }

    String nodeId = null
    Exchange exchange = null
    CamelContext camelCtx = null
    BundleContext osgiCtx = null
    Bundle felix = null, com_sap_it_node_stack_profile = null
    Registry registry

    Map<String, String> inHeaders = [:], queryParams = [:]
    String method, query = null, root = null, requestURI = null
    Action action = Action.None
    boolean mock = false

    int outRC = 0
    StringBuilder out = new StringBuilder()
    InputStream outStream = null
    Map<String, String> outHeaders = [:]

    ChServlet(String root, Exchange exc, boolean mock) {
        this.root = root
        this.mock = mock
        this.exchange = exc
        this.nodeId = getNodeId(exc.exchangeId)
        if (!mock) {
            camelCtx = exchange.context
            registry = camelCtx.registry
            osgiCtx = FrameworkUtil.getBundle(exchange.getClass()).bundleContext
            felix = osgiCtx.getBundle(0)
            assert felix
            com_sap_it_node_stack_profile = osgiCtx.bundles.find { it.symbolicName == "com.sap.it.node.stack.profile" }
            assert com_sap_it_node_stack_profile
        }
    }

    static Pattern nodeIdFromExchangeId = Pattern.compile("^ID-(.+)-([0-9]+)-([0-9]+)-([0-9]+)\$")

    static String getNodeId(String s) {
        Matcher m = nodeIdFromExchangeId.matcher(s)
        if (m.matches()) return m.group(1) else throw new IllegalArgumentException("Given exchange/message id '$s' doesn't match Camel template")
    }

    static Map<String, String> parseQuery(String s) {
        Map<String, String> rez = [:]
        s?.split("&")?.each {
            it = URLDecoder.decode(it, "UTF-8")
            if (it.contains("=")) {
                String[] t = it.split("=")
                rez[t[0]] = t[1]
            } else {
                rez[it] = ""
            }
        }
        return rez
    }

    void parse(HttpServletRequest req) {
        req.headerNames.each {
            inHeaders[it] = req.getHeader(it)
        }
        query = req.queryString
        queryParams = parseQuery(query)
        method = req.method
        requestURI = req.requestURI
        Matcher m = Pattern.compile("$root/?(.*)\$").matcher(req.requestURI)
        if (m.matches() && m.group(1)) {
            action = Action.values().find { it.name().toLowerCase() == m.group(1) } ?: Action.Unknown
        }
    }

    void doGET() {
        outRC = 200
        outHeaders."Content-Type" = "text/html; charset=utf-8"
        switch (action) {
            case Action.None:
                out << """<html><head><title>Чепца v${getBuildVersion()}</title></head><body>
<p><a href='$root/version'>Version</a> посмотреть версию и бандлы</p>
<p><a href='$root/tar'>Tar</a> скачать полный тар сипиая
  или <a href='$root/tar?set=АПИ'>набор API для разработчика</a>
  или <a href='$root/tar?set=адаптеры'>все адаптеры</a>
  или <a href='$root/tar?set=толькоСтандарт'>только стандарт</a></p>

<p><a href='$root/testbedneo'>Тест сервисов Neo</a> и <a href='$root/testbedcf'>CF</a> для платформозависимого</p>

<p><a href='$root/inflight'>Запуск 1В595-1</a></p>
<p>Журнал <a href='$root/murzilka'>Мурзилка</a></p>

<hr/>Привет завсегдатаям <a href='https://t.me/sapintegration'>@sapintegration</a>
</body></html>
"""
                break
            case Action.Version:
                version()
                break
            case Action.Bundle:
                bundle()
                break
            case Action.Download:
                if (!query) {
                    outRC = 404
                    out << "<html><body>Не указан файл для скачивания!</body></html>"
                } else {
                    assert queryParams.file && queryParams.name
                    Path p = Paths.get(queryParams.file)
                    if (Files.exists(p)) {
                        outHeaders."Content-Type" = "binary/octet-stream"
                        outHeaders."Content-Description" = "File download"
                        outHeaders."Content-Transfer-Encoding" = "binary"
                        outHeaders."Content-Disposition" = "attachment; filename=${queryParams.name}" as String
                        outStream = Files.newInputStream(p)
                        outRC = 200
                    } else {
                        outRC = 404
                        out << "<html><body>Файл $p не найден!</body></html>"
                    }
                }
                break
            case Action.Tar:
                tar()
                break
            case Action.TestBedCF:
                out << "<html><head><title>Тест для CF</title></head><body><pre>\n"
                out << new TestBedCF(this.exchange).log() << "</pre>"
                out << "\n\n<hr/>"
                out << "\n</body></html>"
                break
            case Action.TestBedNeo:
                out << "<html><head><title>Тест для Neo</title></head><body><pre>\n"
                out << new TestBedNeo(this.exchange).log() << "</pre>"
                out << "\n\n<hr/>"
                out << "\n</body></html>"
                break
            case Action.Inflight:
                inflight()
                break
            case Action.Murzilka:
                murzilka()
                break
            case Action.TestForm:
                testForm()
                break
            case Action.Cancel:
                // выдать редирект 302 на inflight
                inflight()
                break
            case Action.Unknown:
                outRC = 404
                out << """<html><head><title>Сервлет</title></head><body>
<p>$requestURI не предусмотрено пока, вернитесь <a href='$root'>назад</a></p>
<hr/></body></html>"""
                break
        }
    }

    private void version() {
        out << """<html><head><title>Версия системы и компонент</title></head><body>
<pre>"""
        if (!mock) {
            out << "CPI version = ${com_sap_it_node_stack_profile.version}\n"
            out << "Java version = ${System.getProperty("java.version")}\n"
            out << "Groovy version = ${GroovySystem.version}\n"
            assert exchange
            Bundle camelCore = osgiCtx.bundles.find { it.symbolicName == "org.apache.camel.camel-core" }
            if (camelCore) {
                out << "Camel version = $camelCore.version\n"
            }
            out << """CamelContext:
 .getPropertyPrefixToken() = ${camelCtx.getPropertyPrefixToken()}
 .getPropertySuffixToken() = ${camelCtx.getPropertySuffixToken()}
 .getLanguageNames() = ${camelCtx.getLanguageNames()}
 .getComponentNames() = ${camelCtx.getComponentNames()}
 .getDefaultTracer() = ${camelCtx.getDefaultTracer()}
 .getVersion() = ${camelCtx.getVersion()}
 .getUuidGenerator() = ${camelCtx.getUuidGenerator()}
 .getStatus() = ${camelCtx.getStatus()}
 .getGlobalOptions() = ${camelCtx.getGlobalOptions()}
 .getRegistry() = ${camelCtx.getRegistry()}

***************************************************************
Camel context (идентификатор потока)=$camelCtx
Аптайм потока: ${camelCtx.uptime}
</pre>
"""
            out << "<h2>Бандлы</h2><table><thead><th>ID, версия</th><th>Тип</th><th>Provide-Capability</th></thead><tbody>\n"
            long total = 0, qty = 0

            osgiCtx.bundles.each { Bundle b ->
                out.append("\n<tr><td>")
                out.append("<a href='$root/bundle?$b.symbolicName'>$b</a> $b.version")
                long sz = 0
                String type, cap = "система"

                if (b.bundleId != 0) {
                    CpiBundle cpib = new CpiBundle(b)
                    assert cpib.tRUE() // проверка класслоадера
                    sz = Files.size(cpib.file)
                    type = cpib.kind
                    cap = cpib.headers.ProvideCapability
                } else {
                    type = "Системный"
                }
                total += sz
                qty++
                out.append("</td><td>$type</td><td>$cap</td></tr>")
            }
            out.append("\n</tbody></table>")
            out.append("<b>Всего $qty бандлов общим размером $total байт</b>\n")
            out.append("<h3>Сервисы</h3><pre>")
            ServiceReference[] sref = osgiCtx.getAllServiceReferences(null, null)
            int c = 0
            Set<String> objectClassez = new HashSet<>()
            sref.each { ServiceReference it ->
                out << "\n[$c] "
                it.propertyKeys.each { String k -> out << "$k=${it.getProperty(k)},"
                }
                String[] classez = it.getProperty("objectClass")
                classez.each { objectClassez.add(it) }
                c++
            }
            out << "\n</pre>"
            out << "<h3>Уникальные классы публичных сервисов</h3>\n<ul>"
            objectClassez.each {
                String s
                try {
                    def f = registry.lookupByName(it)
                    s = f.toString()
                } catch (Exception e) {
                    s = e.message
                }
                out << "<li>$it == $s</li>\n"
            }
            out << "</ul>\n"
//            ComponentCommand cc = registry.lookupByName("com.sap.esb.monitoring.component.command.impl.ComponentCommand")
//            IRuntimeDelegate ird = registry.lookupByName("com.sap.gateway.core.api.delegate.IRuntimeDelegate")
//            MessageLogFactory mlf = registry.lookupByName("com.sap.it.api.msglog.MessageLogFactory")
//            ListCommand lc = registry.lookupByName("com.sap.it.commons.cache.commands.ListCommand")
//            ConfigurationReadService crs = registry.lookupByName("com.sap.it.commons.config.read.ConfigurationReadService")
            ServiceComponentRuntimeAccess scra = registry.lookupByName("com.sap.it.op.component.check.ServiceComponentRuntimeAccess")
            out << "<h3>ServiceComponentRuntimeAccess</h3><pre>\n"
            scra.components.each { k, v -> out << "$k: ${v.state}\n"
            }
            out << "</pre>\n"
            DebugLogLevelConfigurationProvider dllcp = registry.lookupByName("com.sap.it.op.mpl.loglevel.DebugLogLevelConfigurationProvider")
        } else {
            out << "Java version = ${System.getProperty("java.version")}\n"
            out << "Groovy version = ${GroovySystem.version}\n"
            out.append("<a href='$root/bundle?test.bundle.com'>test.bundle.com</a>\n")
        }
        out << "</pre><hr/></body></html>"
    }

    private void bundle() {
        String bundleName = queryParams.keySet()[0]
        out << "<html><head><title>$bundleName</title></head><body>\n"
        if (!mock) {
            assert exchange
            Bundle b = osgiCtx.bundles.find { it.symbolicName == bundleName }

            if (b == null) {
                out << "<b>ОШИБКА: OSGi-бандл $bundleName не может быть найден</b>\n"
                outRC = 400
                return
            }

            if (b.bundleId == 0) {
                out << "<b>$bundleName - системный</b>\n"
                out << """<pre>location=$b.location, version=$b.version</pre>\n"""
                return
            }
            CpiBundle cb = new CpiBundle(b)
            String dlname = "${bundleName}_${b.version}.jar"
            out << """<h1>$bundleName [$b.bundleId] $b.version</h1>
скачать <a href="$root/download?file=${cb.file}&name=$dlname">$dlname</a><br/>

АПИ=${cb.isAPI}, вид=${cb.kind}, файл=${cb.file}, частьСтандарта=${cb.partOfStandard} $cb

<h2>Заголовки манифеста</h2><pre>
"""
            cb.headers.each { k, v -> out << "\n$k: $v"
            }

            out << "\n</pre>\n<h2>Состав бандла</h2><pre>\n"
            cb.jc.entries.each {
                out << it << "\n"
            }
            out << "\n</pre>\n"
            out << "\n\n<hr/>"
            out << "\n</body></html>"
        } else {
            out << "Mock mode - ничего не реализовано\n"
        }
    }

    void tar() {
        Map<String, Path> lst2 = [:]
        int total = 0
        String filename
        if (mock) {
            filename = "сипиай_проба.tar"
            Path p = Paths.get("C:/Temp")
            DirectoryStream<Path> lst = Files.newDirectoryStream(p)
            lst.each {
                if (Files.isRegularFile(it)) {
                    total += Files.size(it)
                    total += 1024
                    // проверяем как работают имена
                    lst2[it.fileName.toString() + "_123456789"] = it
                }
            }
        } else {
            String set = queryParams.set ?: "полныйФарш"
            assert set in ["полныйФарш", "АПИ", "адаптеры", "толькоСтандарт"]
            filename = "сипиай_${com_sap_it_node_stack_profile.version}_${set}.tar"
            List<CpiBundle> bundles = new CpiBundle().listOfBundles(osgiCtx)
            //osgiCtx.bundles.findAll {it.bundleId!=0}.each {bundles.add(new CpiBundle(it))}

            bundles.each { CpiBundle cb ->
                boolean ad = cb.kind == BundleKind.Adapter && set == "адаптеры"
                boolean api = set == "АПИ" && cb.isAPI
                if (set == "полныйФарш" || ad || api) {
                    total += Files.size(cb.file)
                    total += 1024 // служебные атрибуты блока
                    lst2[cb.fileName] = cb.file
                }
            }
            total += 1024 // для пустого tar без единого блока
        }
        ByteBuffer bb = ByteBuffer.allocate(total)
        ByteBufferOutputStream bbos = new ByteBufferOutputStream(bb)
        TarArchiveOutputStream tas = new TarArchiveOutputStream(bbos, "utf-8")
        lst2.each { name, path ->
            ArchiveEntry te = tas.createArchiveEntry(path, name)
            tas.putArchiveEntry(te)
            IOUtils.copy(Files.newInputStream(path), tas)
            tas.closeArchiveEntry()
        }
        tas.close()
        bbos.close()

        outHeaders."Content-Type" = "application/x-tar"
        outHeaders."Content-Description" = "File download"
        outHeaders."Content-Transfer-Encoding" = "binary"
        outHeaders."Content-Disposition" = "attachment; filename=" + MimeUtility.encodeText(filename)
        outStream = new ByteArrayInputStream(bb.array(), 0, bb.position())
    }

    List<IR> getInProcessing() {
        List<IR> inProcessing = []
        ServiceReference[] srs = osgiCtx.getServiceReferences(CamelContext.class.getName(), null)
        srs.each { sr ->
            CamelContext c2 = (CamelContext) osgiCtx.getService(sr)
            c2.inflightRepository.browse().each {
                inProcessing.add(new IR(sr.bundle, it))
            }
        }
        return inProcessing
    }

    void inflight() {
        // https://blogs.sap.com/2019/11/07/how-to-stop-messages-in-cpi-manually/
        List<IR> inProcessing = getInProcessing()

        out << """<html><head>
<title>Бортовой самописец 1В595-1</title></head><body>
<h1>Дежурная смена, отчёт от ${nodeId}</h1>
<table border="1px"><thead><tr><td>#</td><td>Узел связи</td><td>MPL ID</td><td>В карауле с</td><td>Полк</td><td>Приказ</td><td>Застряло в</td></tr></thead>
<tbody>"""
        int cx = 1
        inProcessing.each {
            Date cct = it.CamelCreatedTimestamp
            ZonedDateTime zdt = ZonedDateTime.ofInstant(cct.toInstant(), ZoneId.of("Europe/Moscow"))
            out << """<tr>
<td>${cx++}</td>
<td>${it.cpiNode}</td>
<td>${it.SAP_MessageProcessingLogID}</td>
<td>${zdt.toOffsetDateTime()}</td>
<td>${it.bundle.symbolicName}</td>
"""
            boolean own = it.exchange == exchange
            if (own) out << "<td>не стрелять, свои</td>" else out << """<td>
<form action='$root/cancel' method='POST'>
<input type='hidden' name="SAP_MessageProcessingLogID" value="${it.SAP_MessageProcessingLogID}"/>
<input type='hidden' name="bundleSymbolicName" value="${it.bundle.symbolicName}"/>
<input type='submit' value='отставить'/></form></td>
"""
            out << "<td>${it.nodeId}</td></tr>"
        }
        out << "</tbody></table></body></html>"
    }

    void murzilka() {
        NodeHabitat nh = registry.lookupByName("com.sap.it.nm.spi.node.NodeHabitat")
        ArtifactAccess aa = registry.lookupByName("com.sap.it.nm.spi.store.access.ArtifactAccess")
        TaskAccess ta = registry.lookupByName("com.sap.it.nm.spi.store.access.TaskAccess")
        //com.sap.it.nm.core.store.entity.ds.ArtifactAccessComponent aac = aa
        out << """<html><head><title>Полистать Мурзилку за 2384г</title></head><body>
<h1>...</h1>
<table><thead><tr>
<td>ИД</td><td>Имя</td><td>Версия</td><td>Кто задеплоил</td><td>Пакет</td><td>Предыдущий ИД</td></tr></thead>
<tbody>"""
//        out << "XXXXXX="+aa.findDescriptorsById(["b4d36a25-1e31-40f9-921b-edaf8effa441"].toSet())

        ArtifactType.values().each { at ->
            out << "<tr><td colspan='4'>$at</td></tr>\n"
            aa.findBy(nh.tenantId, at).each { ad, Artifact a ->
                String packid = ad.tags.find { it.name == "artifact.package.id" }?.value
                String previd = ad.tags.find { it.name == "previous.artifact.id" }?.value
                out << """<tr>
<td>${ad.id}</td>
<td>${ad.symbolicName}</td>
<td>${ad.version}</td>
<td>${ad.deployedBy}</td>
<td>${packid}</td>
<td>${previd}</td>
</tr>"""
            }
        }
        out << "</tbody></table>"
//        NodeLocal nodeLocal = registry.lookupByName("com.sap.it.nm.node.NodeLocal")
//        TenantQuery tq = registry.lookupByName("com.sap.it.nm.TenantQuery") бессмысленный запрос
        // tsa == com.sap.it.nm.core.store.entity.RestrictedTenantStoreAccessImpl
        //TenantStoreAccess tsa = registry.lookupByName("com.sap.it.nm.spi.store.access.TenantStoreAccess")

        out << """<pre>NodeHabitat:
  .account = ${nh.account}
  .application = ${nh.application}
  .tenantId = ${nh.tenantId}
  .tenantName = ${nh.tenantName}
  .orchestratorUrl = ${nh.orchestratorUrl}
  .globalHost = ${nh.globalHost}
  .internalDomain = ${nh.internalDomain}
  .platformRuntimeVersion = ${nh.platformRuntimeVersion}
  .baseUri = ${nh.baseUri}
  .dispatcherUri = ${nh.dispatcherUri}
  .nodeId = ${nh.nodeId}
  .landscapeType = ${nh.landscapeType}
  .availabilityZone = ${nh.availabilityZone}
LandscapeInfo  
  .domain = ${nh.domain}
  .name = ${nh.name}
  .regionName = ${nh.regionName}
  .landscapeInternal = ${nh.landscapeInternal}

"""
        // results non-catched exception
//        com.sap.core.config.runtime.api.ConfigurationService cs = registry.lookupByName("com.sap.core.config.runtime.api.ConfigurationService")
//        com.sap.core.config.runtime.ConfigurationServiceImpl csi = cs
//        out << """ConfigurationService $csi
//"""

        out << "</pre></body></html>"
    }

    void testForm() {
        out << """<html><body><form name='a' action='$root/testform' method='POST'><pre>
Тестовая форма <input type='hidden' name='input1' value='A123&amp;456&gt;&lt;'>
sddffgddgfgfgfg
<input type='hidden' name='input2' value=' русские буквы и пробелЪ '>
<input type='submit' value='отставить'/>
</pre></form></body></html>"""
    }

    void testForm(String body) {
        out << "<html><pre>"
        inHeaders.each { k, v -> out << "$k\t$v\n" }
        out << "<hr/>$body</pre></html>"
    }

    void doPOST(String body) {
        outRC = 200
        outHeaders."Content-Type" = "text/html; charset=utf-8"
        String ct = inHeaders.find { it.key.equalsIgnoreCase("content-type") }?.value
        Map<String, String> form = [:]
        if (ct == "application/x-www-form-urlencoded") {
            form = parseQuery(body)
        }
        switch (action) {
            case Action.Cancel:
                boolean cancelled = false
                String text
                List<IR> inProcessing = getInProcessing()
                def ie= inProcessing.find{it.SAP_MessageProcessingLogID==form.SAP_MessageProcessingLogID}
                if (ie) {
                    assert ie.exchange
                    ie.exchange.setException(new InterruptedException("Galya, we have a cancellation!"))
                    ie.exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE)
                    text = "Приказ в отношении '${form.SAP_MessageProcessingLogID}' отправлен"
                    cancelled = true
                } else {
                    text = "Нарушитель '${form.SAP_MessageProcessingLogID}' пока не найден"
                }
                out << """<html><pre>
Предположительно успешно: $cancelled но может потребовать немного времени ожидания
$text
</pre></html>"""
                break
            case Action.TestForm:
                testForm(body)
                break
            default:
                outRC = 404
                out << "<html>Неизвестная команда</html>"
        }
    }

    String getBuildVersion() {
        String ver
        if (this.class.package) ver = this.class.package.getImplementationVersion() else {
            // здесь /META-INF/MANIFEST.MF будет грувишный а не данного jar
//            Properties prop = new Properties()
//            prop.load(this.getClass().getResourceAsStream("/META-INF/MANIFEST.MF"))
//            ver = prop.getProperty("Implementation-Version") ?: "UnknownVersion"
            ver = ChServlet.getResourceAsStream("/version.txt").text
        }
        return ver
    }

    ChServlet clone() {
        return new ChServlet(root, exchange, mock)
    }
}