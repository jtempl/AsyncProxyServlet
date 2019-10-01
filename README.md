# AsyncHttpServlet
Customizable asynchronous (non-blocking) Java proxy servlet based on MITRE ProxyServlet.
Customization follows the MITRE ProxyServlet design, which means that a number of properties can be configured.
In addition, a subclass can be created that overrides some of the methods of the base class.
In particular, getTargetUri() can be customized for dynamically computing the target of a request.

The MITRE ProxyServlet is based on the Apache asynchttpclient and runs under Java 1.8.
Simple tests have been carried out under Tomcat7.

This software is licensed under the Apache License, Version 2.0.
