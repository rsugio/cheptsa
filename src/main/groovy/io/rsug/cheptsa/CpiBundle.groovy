package io.rsug.cheptsa

import org.apache.felix.framework.cache.JarContent
//import org.apache.felix.framework.cache.JarRevision
import org.osgi.framework.Bundle
import org.apache.felix.framework.cache.BundleArchive
import org.osgi.framework.BundleContext

import java.nio.file.Path

class CpiBundle {
    Bundle b
    BundleArchive ba
    def jr //JarRevision
    JarContent jc
    // file == /usr/sap/ljs/data/cache/bundle430/version0.0/bundle.jar, не содержит имени
    Path file
    String fileName // для даунлоада

    CpiBundle(Bundle b) {
        this.b = b
        ba = b.getArchive()
        def jr = ba.currentRevision
        jc = jr.content as JarContent
        file = jc.file.toPath()

        fileName = "${b.symbolicName}_${b.version}.jar"
    }

    /**
     * не возвращает нулевой системный бандл
     * @param osgiCtx
     * @return
     */
    static List<CpiBundle> bundles(BundleContext osgiCtx) {
        List rez = []
        osgiCtx.bundles.each {Bundle b ->
            if (b.bundleId!=0) {
                CpiBundle cb = new CpiBundle(b)
                rez.add(cb)
            }
        }
        return rez
    }


    String toString() {
        "io.rsug.pozim.CpiBundle($b.symbolicName)"
    }

}
