import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.ExchangePattern
import org.apache.camel.ProducerTemplate
import org.apache.camel.Route
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.impl.DefaultProducerTemplate
import org.apache.camel.model.language.SimpleExpression
import org.junit.Test

class CamelTest {
    @Test
    void a() {
        CamelContext context = new DefaultCamelContext()
        context.start()

        context.addRoutes(new RouteBuilder() {
            void configure() {
                from("direct:Process_27944")
                        .id("Process_27944")
                        .setBody(new SimpleExpression("property.test=\${property.test} \${in.body}"))
            }
        })
        ProducerTemplate pt = new DefaultProducerTemplate(context)
        pt.start()
        Route r = context.getRoute("Process_27944")
        StringBuilder log = new StringBuilder()
        log << """*********************
CamelContext=$context
ProducerTemplate=$pt
Route=$r
"""
        (1..5).each {
            Exchange src = r.endpoint.createExchange(ExchangePattern.InOut)
            src.setProperty("test", "$it" as String)
            src.in.setBody("тестРусскихБукв_${it}", String)
            Exchange rez = pt.send(r.endpoint, src)
            log << rez.in.getBody(String) << "\n"
        }
        pt.stop()
        context.stop()
        println(log.toString())
    }
}
