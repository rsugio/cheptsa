import org.apache.camel.*
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.*
import org.apache.camel.model.language.SimpleExpression
import org.junit.Test

class CamelTest {
    Processor logp = new Processor() {
        @Override
        void process(Exchange exchange) throws Exception {
//            println("11:" + exchange.message.messageId)
            println("11:" + exchange.exchangeId)
        }
    }

    @Test
    void a() {
        CamelContext context = new DefaultCamelContext()
        context.start()

        context.addRoutes(new RouteBuilder() {
            void configure() {
                from("direct:Process_27944")
                        .id("Process_27944")
                        .process(logp)
                        .setBody(new SimpleExpression("""property.test=\${property.test} \${in.body}"""))
            }
        })
        ProducerTemplate pt = new DefaultProducerTemplate(context)
        pt.start()
        ConsumerTemplate ct = context.createConsumerTemplate() as DefaultConsumerTemplate
        ct.start()
        Route r = context.getRoute("Process_27944")
        StringBuilder log = new StringBuilder()
        log << """*********************
CamelContext=$context
ProducerTemplate=$pt
ConsumerTemplate=$ct
Route=$r
"""
        Exchange src = r.endpoint.createExchange(ExchangePattern.InOut)
        (1..5).each {
            Exchange rez = src.copy()
            rez.setProperty("test", "$it" as String)
            rez.in.setBody("тестРусскихБукв_${it}", String)
            pt.send(r.endpoint, rez)
            log << rez.in.getBody(String) << "\n"
        }
        ct.stop()
        pt.stop()
        context.stop()
        println(log.toString())
    }
}
