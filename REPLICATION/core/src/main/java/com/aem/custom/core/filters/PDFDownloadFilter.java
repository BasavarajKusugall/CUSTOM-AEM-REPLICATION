package com.aem.custom.core.filters;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;

@Component(service = {Filter.class}, immediate = true, property = {
        HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN + "=/content/dam/*",
        HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT + "=(osgi.http.whiteboard.context.name=*)",
        HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH + "=/content/dam/",
        HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=/content/dam/*",
})
public class PDFDownloadFilter implements Filter {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private ServiceRegistration<Object> configPrinterRegistration;


    @Activate
    @Modified
    private void activate(final BundleContext context) {
        this.configPrinterRegistration = this.registerConfigPrinter(context);

    }

    public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain) throws IOException, ServletException {
        try {
            if (req instanceof HttpServletRequest) {
                final HttpServletRequest request = (HttpServletRequest) req;
                String requestURI = request.getRequestURI();

            } else {
                chain.doFilter(req, res);
            }
        } catch (IOException e) {
            LOGGER.error("doFilter:IOException" + e.getMessage(), e);
            chain.doFilter(req, res);
        } catch (ServletException e) {
            LOGGER.error("doFilter:ServletException" + e.getMessage(), e);
            chain.doFilter(req, res);
        } catch (Exception e) {
            LOGGER.error("doFilter:Exception" + e.getMessage(), e);
            chain.doFilter(req, res);
        }

    }


    public void init(final FilterConfig config) throws ServletException {
    }

    @Deactivate
    public void destroy() {
        this.configPrinterRegistration.unregister();
    }

    //Todo: Dynamically registering the context paths from this here
    private ServiceRegistration<Object> registerConfigPrinter(final BundleContext bundleContext) {
        final ConfigurationPrinter cfgPrinter = new ConfigurationPrinter();
        final Dictionary<String, String> serviceProps = (Dictionary<String, String>) new Hashtable();
        serviceProps.put("felix.webconsole.label", "pdffilter");
        serviceProps.put("felix.webconsole.configprinter.modes", "always");
        return (ServiceRegistration<Object>) bundleContext.registerService(Object.class, cfgPrinter, (Dictionary) serviceProps);
    }


    public class ConfigurationPrinter {
        public void printConfiguration(final PrintWriter pw) {
            pw.println("Configuration details");

        }
    }


}