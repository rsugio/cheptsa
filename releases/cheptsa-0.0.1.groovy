import com.sap.gateway.ip.core.customdev.util.Message as CpiMsg
import javax.servlet.http.HttpServletRequest
import io.rsug.cheptsa.ChServlet

CpiMsg viewer(CpiMsg msg) {
    ChServlet servlet = new ChServlet("/http"+msg.properties.home, msg.exchange, false)
    HttpServletRequest request = msg.headers.CamelHttpServletRequest
    try {
        servlet.parse(request)
        if (servlet.method=="GET")
            servlet.doGET()
        else if (servlet.method=="POST")
            servlet.doPOST(request.inputStream.text)
        else 
            throw new IllegalStateException("http method is empty")
    
        msg.headers.CamelHttpResponseCode = servlet.outRC
        servlet.outHeaders.each {k, v -> msg.headers[k] = v}
        if (servlet.outStream!=null) {
            msg.setBody(servlet.outStream)
        } else
            msg.setBody(servlet.out.toString())

    } catch (Exception e) {
        msg.headers.CamelHttpResponseCode = 500
        msg.headers."Content-Type" = "text/plain; charset=utf-8"
        String log = """Исключение $e.message

${e.stackTrace.join("\n")}
"""        
        msg.setBody(log)
    }
    return msg
}
