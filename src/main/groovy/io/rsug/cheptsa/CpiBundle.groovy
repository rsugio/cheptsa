package io.rsug.cheptsa

import org.apache.felix.framework.cache.JarContent

//import org.apache.felix.framework.cache.JarRevision
import org.osgi.framework.Bundle
import org.apache.felix.framework.cache.BundleArchive
import org.osgi.framework.BundleContext

import java.nio.file.Path

enum BundleKind {
    Unknown, OSGi, Adapter, IFLMAP, VM, ScriptsCollection, FlowStep
}

class CpiBundle {
    Bundle b
    BundleArchive ba
    def jr //JarRevision
    JarContent jc
    // file == /usr/sap/ljs/data/cache/bundle430/version0.0/bundle.jar, не содержит имени
    Path file
    String fileName // для даунлоада
    Map<String, String> headers = [:]
    BundleKind kind = BundleKind.Unknown

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

        if (headers.ProvideCapability == "com.sap.it.capability.adapter")
            kind = BundleKind.Adapter
        else if (headers."ProvideCapability" == "com.sap.it.capability.flowstep")
            kind = BundleKind.FlowStep
        else if (headers."SAP-BundleType" == "IntegrationFlow")
            kind = BundleKind.IFLMAP
        else if (headers."SAP-BundleType" == "ValueMapping")
            kind = BundleKind.VM
        else if (headers."SAP-BundleType" == "ScriptCollection")
            kind = BundleKind.ScriptsCollection
        else
            kind = BundleKind.OSGi


        fileName = "${b.symbolicName}_${b.version}.jar"
    }

    /**
     * не возвращает нулевой системный бандл
     * @param osgiCtx
     * @return
     */
    static List<CpiBundle> bundles(BundleContext osgiCtx) {
        List rez = []
        osgiCtx.bundles.each { Bundle b ->
            if (b.bundleId != 0) {
                CpiBundle cb = new CpiBundle(b)
                rez.add(cb)
            }
        }
        return rez
    }


    String toString() {
        "CpiBundle($b.symbolicName)"
    }

}
