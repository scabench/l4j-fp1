## log4shell false positive

This is a simple web project with a sanitised log4shell vulnerability. For an unsanitised version, see [https://github.com/scabench/l4j-tp1/](https://github.com/scabench/l4j-tp1/).
The project defines a simple `scabench.HelloWorldService` get service returning a plain text string `hello world`.
The service does not expect parameters, and if parameters are encountered, an error 
is logged. 

The vulnerable dependency is [org.apache.logging.log4j:log4j-core:2.14.1](https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core/2.14.1), the vulnerability is [CVE-2021-44228](https://nvd.nist.gov/vuln/detail/CVE-2021-44228). 

### Sanitisation

The project uses an [agent build by Amazon](https://github.com/corretto/hotpatch-for-apache-log4j2) when *log4shell* emerged and no patch was available.
The agent will disable the vulnerable class `org.apache.logging.log4j.core.lookup.JndiLookup`. 

Using the agent is enforced by installing (aka attaching) the agent dynamically when `scabench.HelloWorldService` is loaded (in the static block of the class), 
if this fails, the application crashes. This requires that the JVM enables agent self-attachment.


### Running the Application

1. enable JVM agent self attachment: `export MAVEN_OPTS="-Djdk.attach.allowAttachSelf=true"`
2. start the embedded web server: `mvn jetty:run`
3. start the included the ldap server: `java -jar dodgy-ldap-server.jar` (the vulnerable copde will download Java code from this server)
4. point the browser to `http://localhost:8080/`, this site contains a pre-populated form with a malicious payload `${jndi:ldap://127.0.0.1/exe}`
5. submit this form
6. this will *NOT* create the file `foo` on the server (as the [un-sanitised version]((https://github.com/scabench/l4j-tp1/)) does)


Note that when running the application, the following line appears on the console:

`Transforming org/apache/logging/log4j/core/lookup/JndiLookup `.

### Demonstrating the Sanitisation using a Test

This requires unix or macos. It is easy to port this project to windows.
A unit test is provided to demonstrate the vulnerability, the setup is the same used in 
[https://github.com/scabench/l4j-tp1/](https://github.com/scabench/l4j-tp1/). Due to santitisation,
the test now *fails*. 

### Running Software Composition Analyses

There are several sh scripts to run different analyses, result resports can be found in `scan-results`.

### Generating the SBOM

The `pom.xml` has a plugin to generate a [SBOM](https://www.cisa.gov/sbom) in [CycloneDX](https://cyclonedx.org/) format. 
To do this, run `mvn cyclonedx:makePackageBom`, the SBOM can be found in 
`target/` in `json` and `xml` format.

