package io.rsug.cheptsa

import org.apache.felix.framework.cache.JarContent

import org.osgi.framework.Bundle
import org.apache.felix.framework.cache.BundleArchive
import org.osgi.framework.BundleContext

import java.nio.file.Path

enum BundleKind {
    Unknown, OSGi, Adapter, IFLMAP, VM, ScriptsCollection, FlowStep
}

interface CpiBundleI {  // маркерный интерфейс без которого в рантайме CPI не пашет
    boolean tRUE()
    List<CpiBundle> listOfBundles(BundleContext ctx)
}

class CpiBundle implements CpiBundleI {
    Bundle b
    BundleArchive ba
    def jr //JarRevision
    JarContent jc
    // file == /usr/sap/ljs/data/cache/bundle430/version0.0/bundle.jar, не содержит имени
    Path file
    String fileName // для даунлоада
    Map<String, String> headers = [:]
    BundleKind kind = BundleKind.Unknown
    boolean isAPI, partOfStandard

    CpiBundle() {}
    CpiBundle(Bundle b) {
        this.b = b
        ba = b.getArchive() // на крайняк переписать через InvokeMethod
        def jr = ba.currentRevision
        jc = jr.content as JarContent
        file = jc.file.toPath()

        b.getHeaders().each { d ->
            d.keys().each { k ->
                headers[k] = d.get(k)
            }
        }
        String[] s = (headers."Provide-Capability" ?: "-").split(";")
        headers.ProvideCapability = s[0].trim()

        partOfStandard = false
        if (headers.ProvideCapability == "com.sap.it.capability.adapter") {
            kind = BundleKind.Adapter
            partOfStandard = true // проверка стандартный ли адаптер или нет, не делаю
        } else if (headers."ProvideCapability" == "com.sap.it.capability.flowstep") {
            kind = BundleKind.FlowStep
            partOfStandard = true
        } else if (headers."SAP-BundleType" == "IntegrationFlow")
            kind = BundleKind.IFLMAP
        else if (headers."SAP-BundleType" == "ValueMapping")
            kind = BundleKind.VM
        else if (headers."SAP-BundleType" == "ScriptCollection")
            kind = BundleKind.ScriptsCollection
        else {
            kind = BundleKind.OSGi
            partOfStandard = true
        }

        fileName = "${b.symbolicName}_${b.version}.jar"
        isAPI = b.symbolicName in
                ["com.sap.it.public.generic.api",         // публичное Generic API но оно только в CF
                 "com.sap.it.public.adapter.api",         // публичное Adapter API но оно только в CF
                 "com.sap.cloud.integration.script.apis", // публичное скриптовое но его нет в SCPI
                 "com.sap.it.asdk.com.sap.it.api.asdk",   // непубличное АПИ для: датасторы, кийсторы, креды
                 "com.sap.esb.application.services.datastore", // непубличное для датасторов с произвольным доступом
                 // сюда можно дополнять список также интересных API
                ]
    }
    boolean tRUE() {
        return true
    }

    List<CpiBundle> listOfBundles(BundleContext ctx) {
        List<CpiBundle> lst = []
        ctx.bundles.each {
            if (it.bundleId != 0) lst.add(new CpiBundle(it))
        }
        return lst
    }

    String toString() {
        return "CpiBundle($b.symbolicName)"
    }
}
