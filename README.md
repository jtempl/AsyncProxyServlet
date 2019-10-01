# AsyncHttpServlet
Customizable asynchronous (non-blocking) Java proxy servlet based on MITRE ProxyServlet.
Customization follows the MITRE ProxyServlet design, meaning that a number of properties can be configured and
for more advanced use cases a subclass can be created that overrides some of the methds of the base class.
The MITRE ProxyServlet is based on the Apache asynchttpclient and runs under Java 1.8.

Simple test have been performed under Tomcat7.
