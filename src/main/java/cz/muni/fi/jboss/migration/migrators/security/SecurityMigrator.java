package cz.muni.fi.jboss.migration.migrators.security;

import cz.muni.fi.jboss.migration.*;
import cz.muni.fi.jboss.migration.actions.CliCommandAction;
import cz.muni.fi.jboss.migration.actions.CopyAction;
import cz.muni.fi.jboss.migration.conf.GlobalConfiguration;
import cz.muni.fi.jboss.migration.ex.ActionException;
import cz.muni.fi.jboss.migration.ex.CliScriptException;
import cz.muni.fi.jboss.migration.ex.CopyException;
import cz.muni.fi.jboss.migration.ex.LoadMigrationException;
import cz.muni.fi.jboss.migration.migrators.security.jaxb.*;
import cz.muni.fi.jboss.migration.spi.IConfigFragment;
import cz.muni.fi.jboss.migration.utils.Utils;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Migrator of security subsystem implementing IMigrator
 * 
 * Example AS 5 config:
 * 
        <application-policy name="todo">
            <authentication>
                <login-module code="org.jboss.security.auth.spi.LdapLoginModule" flag="required">
                    <module-option name="password-stacking">useFirstPass</module-option>
                </login-module>
            </authentication>
        </application-policy>
 *
 * @author Roman Jakubco
 */
public class SecurityMigrator extends AbstractMigrator {

    private static final String AS7_CONFIG_DIR_PLACEHOLDER = "${jboss.server.config.dir}";

    
    // Files which must be copied into AS7
    private Set<String> fileNames = new HashSet();


    @Override
    protected String getConfigPropertyModuleName() {
        return "security";
    }


    public SecurityMigrator(GlobalConfiguration globalConfig, MultiValueMap config) {
        super(globalConfig, config);
    }

    /**
     *  Loads the AS 5 data.
     */
    @Override
    public void loadAS5Data(MigrationContext ctx) throws LoadMigrationException {
        try {
            File file = new File(getGlobalConfig().getAS5Config().getConfDir(), "login-config.xml");
            if (!file.canRead()) {
                throw new LoadMigrationException("Can't read: " + file.getAbsolutePath());
            }

            Unmarshaller unmarshaller = JAXBContext.newInstance(SecurityAS5Bean.class).createUnmarshaller();
            SecurityAS5Bean securityAS5 = (SecurityAS5Bean) unmarshaller.unmarshal(file);

            MigrationData mData = new MigrationData();
            mData.getConfigFragments().addAll(securityAS5.getApplicationPolicies());

            ctx.getMigrationData().put(SecurityMigrator.class, mData);
        } catch (JAXBException e) {
            throw new LoadMigrationException(e);
        }
    }

    
    /**
     *  Creates the actions.
     */
    @Override
    public void createActions(MigrationContext ctx) throws ActionException {
        for (IConfigFragment fragment : ctx.getMigrationData().get(SecurityMigrator.class).getConfigFragments()) {
            if (fragment instanceof ApplicationPolicyBean) {
                try {
                    ctx.getActions().addAll(createSecurityDomainCliAction(
                            migrateAppPolicy((ApplicationPolicyBean) fragment, ctx)));
                    continue;
                } catch (CliScriptException e) {
                    throw new ActionException("Migration of application-policy failed: " + e.getMessage(), e);
                }
            }
            throw new ActionException("Config fragment unrecognized by " + this.getClass().getSimpleName() + ": " + fragment);
        }

        for (String fileName : this.fileNames) {
            File src;
            try {
                src = Utils.searchForFile(fileName, getGlobalConfig().getAS5Config().getProfileDir()).iterator().next();
            } catch (CopyException e) {
                throw new ActionException("Copying of file for security failed: " + e.getMessage(), e);
            }

            File target = Utils.createPath(getGlobalConfig().getAS7Config().getDir(), "standalone", "configuration", src.getName());

            // Default value for overwrite => false
            ctx.getActions().add(new CopyAction(src, target, false));
        }

    }

    /**
     * Migrates application-policy from AS5 to AS7
     *
     * @param appPolicy object representing application-policy
     * @param ctx       migration context
     * @return created security-domain
     */
    public SecurityDomainBean migrateAppPolicy(ApplicationPolicyBean appPolicy, MigrationContext ctx) {
        Set<LoginModuleAS7Bean> loginModules = new HashSet();
        SecurityDomainBean securityDomain = new SecurityDomainBean();

        securityDomain.setSecurityDomainName(appPolicy.getApplicationPolicyName());
        securityDomain.setCacheType("default");
        if (appPolicy.getLoginModules() != null) {
            for (LoginModuleAS5Bean lmAS5 : appPolicy.getLoginModules()) {
                loginModules.add(createLoginModule(lmAS5));
            }
        }

        securityDomain.setLoginModules(loginModules);

        return securityDomain;
    }

    
    /**
     *  Migrates the given login module.
     */
    private LoginModuleAS7Bean createLoginModule(LoginModuleAS5Bean lmAS5) {
        LoginModuleAS7Bean lmAS7 = new LoginModuleAS7Bean();

        // Flag
        lmAS7.setLoginModuleFlag( lmAS5.getLoginModuleFlag() );
        
        // Code
        lmAS7.setLoginModuleCode( deriveLoginModuleName( lmAS5.getLoginModule() ) );

        // Module options
        Set<ModuleOptionAS7Bean> moduleOptions = new HashSet();
        lmAS7.setModuleOptions(moduleOptions);
        if( lmAS5.getModuleOptions() == null )
            return lmAS7;

        for( ModuleOptionAS5Bean moAS5 : lmAS5.getModuleOptions() ){
            String value;
            // Take care of specific module options.
            switch( moAS5.getModuleName() ){
                case "rolesProperties":
                case "usersProperties":
                    value = AS7_CONFIG_DIR_PLACEHOLDER + "/" + new File( moAS5.getModuleValue() ).getName();
                    this.fileNames.add(value); // Add to the list of the files to copy. TODO: Use IMigrationActionListener.
                    break;
                default:
                    value = moAS5.getModuleValue();
                    break;
            }
            ModuleOptionAS7Bean moAS7 = new ModuleOptionAS7Bean( moAS5.getModuleName(), value );
            moduleOptions.add( moAS7 );
        }
        return lmAS7;
    }

    /**
     *  AS 7 has few aliases for the distributed login modules.
     *  This methods translates them from AS 5.
     */
    private static String deriveLoginModuleName( String as5moduleName ) {
        
        String type = StringUtils.substringAfterLast(as5moduleName, ".");
        switch( type ) {
            case "ClientLoginModule": return "Client";
            case "BaseCertLoginModule": return "Certificate";
            case "CertRolesLoginModule":  return"CertificateRoles";
            case "DatabaseServerLoginModule": return "Database";
            case "DatabaseCertLoginModule": return "DatabaseCertificate";
            case "IdentityLoginModule": return "Identity";
            case "LdapLoginModule": return "Ldap";
            case "LdapExtLoginModule": return "LdapExtended";
            case "RoleMappingLoginModule": return "RoleMapping";
            case "RunAsLoginModule": return "RunAs";
            case "SimpleServerLoginModule": return "Simple";
            case "ConfiguredIdentityLoginModule": return "ConfiguredIdentity";
            case "SecureIdentityLoginModule": return "SecureIdentity";
            case "PropertiesUsersLoginModule": return "PropertiesUsers";
            case "SimpleUsersLoginModule": return "SimpleUsers";
            case "LdapUsersLoginModule": return "LdapUsers";
            case "Krb5loginModule": return "Kerberos";
            case "SPNEGOLoginModule": return "SPNEGOUsers";
            case "AdvancedLdapLoginModule": return "AdvancedLdap";
            case "AdvancedADLoginModule": return "AdvancedADldap";
            case "UsersRolesLoginModule": return "UsersRoles";
            default: return as5moduleName;
        }
    }

    
    
    /**
     * Creates a list of CliCommandActions for adding a Security-Domain
     *
     * @param domain Security-Domain
     * @return created list containing CliCommandActions for adding the Security-Domain
     * @throws CliScriptException if required attributes for a creation of the CLI command of the Security-Domain
     *                            are missing or are empty (security-domain-name)
     */
    public static List<CliCommandAction> createSecurityDomainCliAction(SecurityDomainBean domain)
            throws CliScriptException {
        String errMsg = " in security-domain must be set.";
        Utils.throwIfBlank(domain.getSecurityDomainName(), errMsg, "Security name");

        List<CliCommandAction> actions = new ArrayList();

        ModelNode domainCmd = new ModelNode();
        domainCmd.get(ClientConstants.OP).set(ClientConstants.ADD);
        domainCmd.get(ClientConstants.OP_ADDR).add("subsystem", "security");
        domainCmd.get(ClientConstants.OP_ADDR).add("security-domain", domain.getSecurityDomainName());

        actions.add(new CliCommandAction(createSecurityDomainScript(domain), domainCmd));

        if (domain.getLoginModules() != null) {
            for (LoginModuleAS7Bean module : domain.getLoginModules()) {
                actions.add(createLoginModuleCliAction(domain, module));
            }
        }

        return actions;
    }

    /**
     * Creates CliCommandAction for adding a Login-Module of the specific Security-Domain
     *
     * @param domain Security-Domain containing Login-Module
     * @param module Login-Module
     * @return created CliCommandAction for adding the Login-Module
     */
    public static CliCommandAction createLoginModuleCliAction(SecurityDomainBean domain, LoginModuleAS7Bean module) {
        ModelNode request = new ModelNode();
        request.get(ClientConstants.OP).set(ClientConstants.ADD);
        request.get(ClientConstants.OP_ADDR).add("subsystem", "security");
        request.get(ClientConstants.OP_ADDR).add("security-domain", domain.getSecurityDomainName());
        request.get(ClientConstants.OP_ADDR).add("authentication", "classic");

        ModelNode moduleNode = new ModelNode();
        ModelNode list = new ModelNode();

        if (module.getModuleOptions() != null) {
            ModelNode optionNode = new ModelNode();
            for (ModuleOptionAS7Bean option : module.getModuleOptions()) {
                optionNode.get(option.getModuleOptionName()).set(option.getModuleOptionValue());
            }
            moduleNode.get("module-options").set(optionNode);
        }

        CliApiCommandBuilder builder = new CliApiCommandBuilder(moduleNode);
        builder.addProperty("flag", module.getLoginModuleFlag());
        builder.addProperty("code", module.getLoginModuleCode());

        // Needed for CLI because parameter login-modules requires LIST
        list.add(builder.getCommand());

        request.get("login-modules").set(list);

        return new CliCommandAction(createLoginModuleScript(domain, module), request);
    }

    /**
     * Creates a CLI script for adding Security-Domain to AS7
     *
     * @param securityDomain object representing migrated security-domain
     * @return created string containing the CLI script for adding the Security-Domain
     * @throws CliScriptException if required attributes are missing
     */
    private static String createSecurityDomainScript(SecurityDomainBean securityDomain)
            throws CliScriptException {
        String errMsg = " in security-domain must be set.";
        Utils.throwIfBlank(securityDomain.getSecurityDomainName(), errMsg, "Security name");

        CliAddScriptBuilder builder = new CliAddScriptBuilder();
        StringBuilder resultScript = new StringBuilder("/subsystem=security/security-domain=");

        resultScript.append(securityDomain.getSecurityDomainName()).append(":add(");
        builder.addProperty("cache-type", securityDomain.getCacheType());

        resultScript.append(builder.asString()).append(")");

        return resultScript.toString();
    }

    /**
     * Creates a CLI script for adding a Login-Module of the specific Security-Domain
     *
     * @param domain Security-Domain containing Login-Module
     * @param module Login-Module
     * @return created string containing the CLI script for adding the Login-Module
     */
    private static String createLoginModuleScript(SecurityDomainBean domain, LoginModuleAS7Bean module) {
        StringBuilder resultScript = new StringBuilder("/subsystem=security/security-domain=" +
                domain.getSecurityDomainName());
        resultScript.append("/authentication=classic:add(login-modules=[{");

        if ((module.getLoginModuleCode() != null) || !(module.getLoginModuleCode().isEmpty())) {
            resultScript.append("\"code\"=>\"").append(module.getLoginModuleCode()).append("\"");
        }
        if ((module.getLoginModuleFlag() != null) || !(module.getLoginModuleFlag().isEmpty())) {
            resultScript.append(", \"flag\"=>\"").append(module.getLoginModuleFlag()).append("\"");
        }

        if ((module.getModuleOptions() != null) || (!module.getModuleOptions().isEmpty())) {
            StringBuilder modulesBuilder = new StringBuilder();
            for (ModuleOptionAS7Bean moduleOptionAS7 : module.getModuleOptions()) {
                modulesBuilder.append(", (\"").append(moduleOptionAS7.getModuleOptionName()).append("\"=>");
                modulesBuilder.append("\"").append(moduleOptionAS7.getModuleOptionValue()).append("\")");
            }

            String modules = modulesBuilder.toString().replaceFirst(",", "");
            modules = modules.replaceFirst(" ", "");

            if (!modules.isEmpty()) {
                resultScript.append(", \"module-option\"=>[").append(modules).append("]");
            }
        }

        return resultScript.toString();
    }
}
