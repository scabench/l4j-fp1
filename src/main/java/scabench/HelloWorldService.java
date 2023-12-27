package scabench;

import com.sun.tools.attach.VirtualMachine;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.apache.logging.log4j.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple HelloWorld service. Request parameters are logged.
 * @author jens dietrich
 */
@WebServlet(name = "HelloWorld", urlPatterns = {"/hello"})
public class HelloWorldService extends HttpServlet {

    // self-attach agent
    static {
        final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        final long pid = runtime.getPid();
        VirtualMachine self = null;

        try {
            self = VirtualMachine.attach(""+pid);
        } catch (Exception e) {
            System.out.println("Cannot attach to this VM, program must be started with JVM option \"-Djdk.attach.allowAttachSelf=true\"");
            // fail so that vulnerable program cannot be started
            throw new IllegalStateException("cannot attach agent (log4shell sanitiser)",e);
        }
        System.out.println("self attached to: " + self);

        File agentJar = new File("Log4jHotPatch.jar");
        if (!agentJar.exists()) {
            throw new IllegalStateException("agent (log4shell sanitiser) jar not found");
        }

        try {
            self.loadAgent(agentJar.getAbsolutePath());
        } catch (Exception x) {
            System.err.println("error loading agent");
            throw new IllegalStateException("agent (log4shell sanitiser) cannot be loaded",x);
        }
    }


    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        // precondition enforcement -- no parameters expected
        Map<String,String[]> parameters = request.getParameterMap();
        if (!parameters.isEmpty()) {
            String parametersAsString = getParametersAsString(request);
            Logger logger = LogManager.getLogger(HelloWorldService.class);
            logger.error("unexpected parameters: " + parametersAsString);
            out.println("unexpected parameters, the problem has been logged");
        }
        else {
            out.println("hi");
        }

        out.close();
    }

    private String getParametersAsString(HttpServletRequest request) {
        return request.getParameterMap().entrySet().stream()
            .map(e -> e.getKey() + " -> " + Stream.of(e.getValue()).collect(Collectors.joining(",")))
            .collect(Collectors.joining(","));
    }
}
