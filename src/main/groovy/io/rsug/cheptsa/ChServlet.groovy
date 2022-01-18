package io.rsug.cheptsa

import org.apache.camel.CamelContext
import org.apache.camel.Exchange
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

class ChServlet {
    enum Action {
        None, Version, Bundle, Download, Tar, TestBedNeo, TestBedCF, Unknown
    }

    Exchange exchange = null
    CamelContext camelCtx = null
    BundleContext osgiCtx = null
    Bundle felix = null, com_sap_it_node_stack_profile = null

    Map<String, String> inHeaders = [:], queryParams = [:]
    String method, query = null, root = null, requestURI = null
    Action action
    boolean mock = false

    int outRC = 0
    StringBuilder out = new StringBuilder()
    InputStream outStream = null
    Map<String, String> outHeaders = [:]

    ChServlet(String root, Exchange exc, boolean mock) {
        this.root = root
        this.mock = mock
        this.exchange = exc
        if (!mock) {
            camelCtx = exchange.context
            osgiCtx = FrameworkUtil.getBundle(exchange.getClass()).bundleContext
            felix = osgiCtx.getBundle(0)
            assert felix
            com_sap_it_node_stack_profile = osgiCtx.bundles.find { it.symbolicName == "com.sap.it.node.stack.profile" }
            assert com_sap_it_node_stack_profile
        }
    }

    void parse(HttpServletRequest req) {
        req.headerNames.each {
            inHeaders[it] = req.getHeader(it)
        }
        query = req.queryString
        String[] s = query?.split("&")
        s.each {
            it = URLDecoder.decode(it, "UTF-8")
            if (it.contains("=")) {
                String[] t = it.split("=")
                queryParams[t[0]] = t[1]
            } else {
                queryParams[it] = ""
            }
        }
        method = req.method

        requestURI = req.requestURI
        if (requestURI == root || requestURI == "$root/")
            action = Action.None
        else if (requestURI == "$root/version")
            action = Action.Version
        else if (requestURI == "$root/bundle")
            action = Action.Bundle
        else if (requestURI == "$root/download")
            action = Action.Download
        else if (requestURI == "$root/tar")
            action = Action.Tar
        else if (requestURI == "$root/neo")
            action = Action.TestBedNeo
        else if (requestURI == "$root/cf")
            action = Action.TestBedCF
        else
            action = Action.Unknown
    }

    void doGET() {
        outRC = 200
        outHeaders."Content-Type" = "text/html; charset=utf-8"
        switch (action) {
            case Action.None:
                out << "<html><head><title>Чепца v${getBuildVersion()}</title></head><body>\n"
                out << "<p><a href='$root/version'>Version</a> посмотреть версию и бандлы</p>\n"
                out << "<p><a href='$root/tar'>Tar</a> скачать полный тар сипиая"
                out << " или <a href='$root/tar?set=АПИ'>набор API для разработчика</a>"
                out << " или <a href='$root/tar?set=адаптеры'>все адаптеры</a>"
                out << " или <a href='$root/tar?set=толькоСтандарт'>только стандарт</a>"
                out << "</p>\n"
                out << "<p><a href='$root/neo'>Тест сервисов Neo</a> и <a href='$root/cf'>CF</a> для платформозависимого</p>\n"
                out << "<hr/>Привет завсегдатаям <a href='https://t.me/sapintegration'>@sapintegration</a>"
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
                    out << "<html><body>Не указан файл для скачивания!\n"
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
                        out << "<html><body>Файл $p не найден!\n"
                    }
                }
                break
            case Action.Tar:
                tar()
                break
            case Action.TestBedCF:
                out << "<html><head><title>Тест для CF</title></head><body><pre>\n"
                out << new TestBedCF(this.exchange).log() << "</pre>"
                break
            case Action.TestBedNeo:
                out << "<html><head><title>Тест для Neo</title></head><body><pre>\n"
                out << new TestBedNeo(this.exchange).log() << "</pre>"
                break
            case Action.Unknown:
                out << "<html><head><title>Сервлет</title></head><body>\n"
                out << "<p>Не предусмотрено пока, вернитесь <a href='$root'>назад</a></p>\n"
                break
        }
        if (!outStream) {
            out << "\n\n<hr/>"
            out << "\n</body></html>"
        }
    }

    private void version() {
        out << "<html><head><title>Версия системы и компонент</title></head><body>\n"

        out << "<pre>\n"
        if (!mock) {
            out << "CPI version = ${com_sap_it_node_stack_profile.version}\n"
            out << "Java version = ${System.getProperty("java.version")}\n"
            out << "Groovy version = ${GroovySystem.version}\n"
            assert exchange
            Bundle camelCore = osgiCtx.bundles.find { it.symbolicName == "org.apache.camel.camel-core" }
            if (camelCore) {
                out << "Camel version = $camelCore.version\n"
            }
            Bundle scriptAPI = osgiCtx.bundles.find { it.symbolicName == "com.sap.it.script.script.engine.api" }
            out << "Script API version = ${scriptAPI.version}\n"
            CamelContext ctx = exchange.context

            out << """CamelContext:
 .getPropertyPrefixToken() = ${ctx.getPropertyPrefixToken()}
 .getPropertySuffixToken() = ${ctx.getPropertySuffixToken()}
 .getLanguageNames() = ${ctx.getLanguageNames()}
 .getComponentNames() = ${ctx.getComponentNames()}
 .getDefaultTracer() = ${ctx.getDefaultTracer()}
 .getVersion() = ${ctx.getVersion()}
 .getUuidGenerator() = ${ctx.getUuidGenerator()}
 .getStatus() = ${ctx.getStatus()}
 .getGlobalOptions() = ${ctx.getGlobalOptions()}
 .getRegistry() = ${ctx.getRegistry()}

"""
/* .getInterceptStrategies() = ${ctx.getInterceptStrategies()}
 .getDataFormats() = ${ctx.getDataFormats()}
 .getTransformers() = ${ctx.getTransformers()}
 .getTransformerRegistry() = ${ctx.getTransformerRegistry()}
 .getValidators() = ${ctx.getValidators()}
 .getDefaultFactoryFinder() = ${ctx.getDefaultFactoryFinder()}
 .getProducerServicePool() = ${ctx.getProducerServicePool()}
 .getNodeIdFactory() = ${ctx.getNodeIdFactory()}
 .getManagementStrategy() = ${ctx.getManagementStrategy()}
 .getInflightRepository() = ${ctx.getInflightRepository()}
*/
            out << "***************************************************************\n"
            out.append("Camel context (идентификатор потока)=$camelCtx\n")
            out.append("Аптайм потока: ${camelCtx.uptime}\n")
            out << "</pre>\n"

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
            int cc = 0
            Set<String> objectClassez = new HashSet<>()
            sref.each {ServiceReference it ->
                out << "\n[$cc] "
                it.propertyKeys.each {String k ->
                    out << "$k=${it.getProperty(k)},"
                }
                String[] classez = it.getProperty("objectClass")
                classez.each {objectClassez.add(it)}
                cc++
            }
            out << "\n</pre>"
            out << "<h3>Уникальные классы публичных сервисов</h3>\n<ul>"
            Registry registry = exchange.context.registry
            objectClassez.each {
                String s
                try {
                    def f = registry.lookupByName(it)
                    s = f.toString()
                } catch(Exception e) {
                    s = e.message
                }
                out << "<li>$it == $s</li>\n"}
            out << "</ul>\n"

        } else {
            out << "Java version = ${System.getProperty("java.version")}\n"
            out << "Groovy version = ${GroovySystem.version}\n"
            out.append("<a href='$root/bundle?test.bundle.com'>test.bundle.com</a>\n")
        }
        out << "</pre>\n"
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
            cb.headers.each { k, v ->
                out << "\n$k: $v"
            }

            out << "\n</pre>\n<h2>Состав бандла</h2><pre>\n"
            cb.jc.entries.each {
                out << it << "\n"
            }
            out << "\n</pre>\n"
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

    void doPOST(String body) {
        outRC = 200
        outHeaders."Content-Type" = "text/plain"
        out << "POST\n\n${inHeaders}\n\n$body"
    }

    String getBuildVersion() {
        String ver
        if (this.class.package)
            ver = this.class.package.getImplementationVersion()
        else {
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
