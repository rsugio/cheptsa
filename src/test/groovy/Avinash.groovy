import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.utils.PdfMerger
import org.junit.Test

import java.nio.file.Paths
import java.util.zip.ZipInputStream

class Avinash {
    @Test
    void a() {
        String COVER = "C:\\workspace\\itext7\\hero.jar"
        String RESOURCE = "C:\\workspace\\itext7\\pages.pdf"

        ZipInputStream zis = new ZipInputStream(Paths.get(COVER).newInputStream())
        zis.nextEntry
        ByteArrayOutputStream dest = new ByteArrayOutputStream()
        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(dest))
        PdfDocument cover = new PdfDocument(new PdfReader(zis))
        PdfDocument resource = new PdfDocument(new PdfReader(RESOURCE))

        PdfMerger merger = new PdfMerger(pdfDoc)
        merger.merge(cover, 1, 1)
        merger.merge(resource, 1, resource.getNumberOfPages())
        cover.close()
        resource.close()
        merger.close()

        dest.close()
        println(dest.toByteArray().length)
    }
}
