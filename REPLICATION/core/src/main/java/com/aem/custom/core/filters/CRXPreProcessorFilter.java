package com.aem.custom.core.filters;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.*;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.metatype.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
/**
 * This service will filter requests to CRX/DE. Based on the felix configuration it will grant access to the CRX repository.
 * By default admin and anonymous can be able open the /crx/de/index.jsp.
 * If user not part of the usernames or is not member of group then service will forbidden(response as 403) the access to CRX.
 * */
@Component(service = { Filter.class },immediate = true, property = {
        HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN+"=/crx/*",
        HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT+"=(osgi.http.whiteboard.context.name=*)",
        HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH+"=/crx",
        HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN+"=/crx/*",
})
@Designate(ocd = CRXPreProcessorFilter.Config.class)
public class CRXPreProcessorFilter implements Filter {
    public static final String[] DEFAULT_USER_NAMES = {"admin", "anonymous"};
    public static final String CRX_DE_PATH = "/crx/de/index.jsp";
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private Config config;
    private ServiceRegistration<Object> configPrinterRegistration;


    @Activate
    @Modified
    private void activate(final BundleContext context, final Config config) {
        this.config = config;
        log("\n ::::::: Entered into QRCRXSecurityProvider: activate ::::::::::::\n");
        this.configPrinterRegistration = this.registerConfigPrinter(context);

    }

    public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain) throws IOException, ServletException {
        log("\n ::::::: Entered into QRCRXSecurityProvider: doFilter ::::::::::::\n");
        try {
            if (req instanceof HttpServletRequest) {
                final HttpServletRequest request = (HttpServletRequest) req;
                String requestURI = request.getRequestURI();
                if (StringUtils.equalsIgnoreCase(requestURI, CRX_DE_PATH) && config.enable()) {
                    String remoteUser = request.getRemoteUser();
                    if (StringUtils.isNotEmpty(remoteUser)) {
                        if (ArrayUtils.contains(DEFAULT_USER_NAMES, remoteUser)) {
                            log("::::::::::::: Found default user. User " + remoteUser + " grant to access to CRX :::::::::::");
                            chain.doFilter(req, res);
                        } else {
                            log(" ::::::: User is not listed in config. Checking user rights to access CRX  ::::::::::::");
                            if (!isAllowedMember(remoteUser)) {
                                log("\n ******* User is not authorized to access the CRX ************* \n");
                                final HttpServletResponse response = (HttpServletResponse) res;
                                response.sendError(403);
                            } else {
                                log("********* User has access to CRX *******");
                                chain.doFilter(req, res);
                            }
                        }
                    } else {
                        log("\n  ::::::::::::::::: Remote user is not found in request.Hence grant access to CRX :::::::::::::");
                        chain.doFilter(req, res);
                    }


                } else {
                    chain.doFilter(req, res);
                }
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

    /***
     * This method will check the user is allowed in configured usernames and groupnames.
     * */
    private boolean isAllowedMember(final String remoteUser) throws RepositoryException {
        log(" :::::::::: Entered into isAllowedMember: Check user can be able to access CRX:" + remoteUser + ":::::::::::::::::::::");
        boolean canAccess = false;
        ResourceResolver serviceUtilResourceResolver = null;
        try {
            if (null != serviceUtilResourceResolver) {
                final UserManager userManager = serviceUtilResourceResolver.adaptTo(UserManager.class);
                if (null != userManager) {
                    final Authorizable authorizable = userManager.getAuthorizable(remoteUser);
                    if (canAccessCRX(remoteUser, authorizable)) return true;
                } else {
                    log("\n ********* Warning: UserManager is null due to internal system issues( System user not having ACL permission). So allowing user to access CRX ******* \n");
                    return true;
                }
            } else {
                log("\n ********* Warning: Service Resolver is null. So allowing user to access CRX ******* \n");
                canAccess = true;
            }

        } catch (Exception e) {
            LOGGER.error("isAllowedMember:Exception" + e.getMessage(), e);
            canAccess = true;
        } finally {
            if (null != serviceUtilResourceResolver && serviceUtilResourceResolver.isLive()) {
                serviceUtilResourceResolver.close();
            }
        }

        return canAccess;
    }

    /**
     * This method will check the permissions by the group policies.
     */
    private boolean canAccessCRX(final String remoteUser, final Authorizable authorizable) throws RepositoryException {
        if (null != authorizable) {
            if (ArrayUtils.contains(config.security_user_names(), remoteUser)) {
                log(" ********* User found in system config and is allowed to access the CRX ***********");
                return true;
            } else {
                log(" ********* User not found in system config and checking access by group policies ***********");
                Iterator<Group> groupIterator = authorizable.memberOf();
                while (null != groupIterator && groupIterator.hasNext()) {
                    Group group = groupIterator.next();
                    final String groupId = group.getID();
                    if (ArrayUtils.contains(config.security_group_names(), groupId)) {
                        log(" ********* User grant access to CRX by group policies ************* ");
                        return true;
                    }
                }
            }
        }
        log("\n ################## User not present in system:" + remoteUser + ". Not authorized to access the CRX ################ \n");
        return false;
    }


    public void init(final FilterConfig config) throws ServletException {
    }

    @Deactivate
    public void destroy() {
    }

    @Deactivate
    protected void deactivate() {
        this.configPrinterRegistration.unregister();
    }

    private ServiceRegistration<Object> registerConfigPrinter(final BundleContext bundleContext) {
        log("\n :::::::::: Register config printer for QRCRXSecurityProvider :::::: \n");
        final ConfigurationPrinter cfgPrinter = new ConfigurationPrinter();
        final Dictionary<String, String> serviceProps = (Dictionary<String, String>) new Hashtable();
        serviceProps.put("service.description", "Configuration for the security provider used to verify user credentials and grant access to the CRX based on registered JCR Repository users");
        serviceProps.put("service.vendor", "Qatar Airways");
        serviceProps.put("felix.webconsole.label", "qrcrxsecurityprovider");
        serviceProps.put("felix.webconsole.title", "QR CRX Security Provider");
        serviceProps.put("felix.webconsole.configprinter.modes", "always");
        return (ServiceRegistration<Object>) bundleContext.registerService(Object.class, cfgPrinter, (Dictionary) serviceProps);
    }

    private void log(String message) {
        if (null != config && StringUtils.equalsIgnoreCase(config.logger_level(), "ERROR")) {
            LOGGER.error(message);
        } else if (null != config && StringUtils.equalsIgnoreCase(config.logger_level(), "DEBUG")) {
            LOGGER.debug(message);
        } else {
            LOGGER.info(message);
        }
    }

    @ObjectClassDefinition(name = "QR CRX Security Provider", description = "Configuration for the security provider used to verfiy user credentials and grant access to the CRX based on registered JCR Repository users")
    public @interface Config {

        @AttributeDefinition(name = "Enable/Disable", description = "Enable/Disable CRX security provider feature. By default it is disabled.")
        boolean enable() default false;

        @AttributeDefinition(
                name = "Logger level",
                description = "Choose logger level. By default it will be INFO.",
                options = {
                        @Option(label = "INFO", value = "INFO"),
                        @Option(label = "ERROR", value = "ERROR"),
                        @Option(label = "DEBUG", value = "DEBUG")
                }
        )
        String logger_level() default "INFO";

        @AttributeDefinition(
                name = "User Names",
                description = "Names of users granted full access to the CRX DE." +
                        " By default this lists the \"admin\" user. A maximum of 20 users may be configured." +
                        " Administrators are encouraged to create a group whose members are to be granted access to CRX instead of allowing access to individual users. (users)",
                type = AttributeType.STRING
        )
        String[] security_user_names() default {"admin", "anonymous"};

        @AttributeDefinition(
                name = "Group Names",
                description = "Names of groups whose members are granted full access to the CRX. The default lists no groups." +
                        " Administrators are encouraged to create a group whose members are to be granted access to the Web Console." +
                        " A maximum of 20 groups may be configured. Using groups to control access requires a Jackrabbit based repository. (groups)",
                type = AttributeType.STRING
        )
        String[] security_group_names();


    }


    public class ConfigurationPrinter {
        public void printConfiguration(final PrintWriter pw) {
            pw.println("Custom CRX Security Provider.");
            pw.println();
            pw.println("Configuration details");
            pw.println();
            pw.println("Service enable status :"+config.enable());
            pw.println();
            String userNames = "[";
            for (String userName: config.security_user_names()){
                userNames +=userName+",";
            }
            userNames+=" ]";
            pw.println("UserNames :"+userNames);
            pw.println();
            String groupNames = "[";
            for (String groupName : config.security_group_names()){
                groupNames += groupName+",";
            }
            groupNames+=" ]";
            pw.println("GroupNames :"+groupNames);
            pw.println();
            pw.println("Enabled Logger level :"+config.logger_level());
            pw.println();
        }
    }


}