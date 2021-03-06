/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 .
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.jboss.loom.migrators.security;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.loom.actions.CliCommandAction;
import org.jboss.loom.actions.CopyFileAction;
import org.jboss.loom.actions.ModuleCreationAction;
import org.jboss.loom.conf.Configuration;
import org.jboss.loom.conf.GlobalConfiguration;
import org.jboss.loom.ctx.MigrationContext;
import org.jboss.loom.ctx.MigratorData;
import org.jboss.loom.ex.CliScriptException;
import org.jboss.loom.ex.LoadMigrationException;
import org.jboss.loom.ex.MigrationException;
import org.jboss.loom.migrators.AbstractMigrator;
import org.jboss.loom.migrators.Origin;
import org.jboss.loom.migrators.security.jaxb.*;
import org.jboss.loom.spi.IConfigFragment;
import org.jboss.loom.spi.ann.ConfigPartDescriptor;
import org.jboss.loom.utils.Utils;
import org.jboss.loom.utils.UtilsAS5;
import org.jboss.loom.utils.XmlUtils;
import org.jboss.loom.utils.as7.CliAddScriptBuilder;
import org.jboss.loom.utils.as7.CliApiCommandBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrator of security subsystem implementing IMigrator
 * 
 *  AS 7.2 docs: https://docs.jboss.org/author/display/AS72/Admin+Guide#AdminGuide-ConfigureSecurityRealms
 * 
 *  EAP 5 docs:  https://access.redhat.com/site/documentation//en-US/JBoss_Enterprise_Application_Platform/5/html/Security_Guide/index.html
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

        <application-policy name="HsqlDbRealm">
            <authentication>
                <login-module code="org.jboss.resource.security.ConfiguredIdentityLoginModule" flag="required">
                    <module-option name="principal">sa</module-option>
                    <module-option name="userName">sa</module-option>
                    <module-option name="password"></module-option>
                    <module-option name="managedConnectionFactoryName">jboss.jca:service=LocalTxCM,name=DefaultDS</module-option>
                </login-module>
            </authentication>
        </application-policy>

 * Example AS 7 config: 
 * 
             <security-realm name="ManagementRealm">
               <plug-ins></plug-ins>
               <server-identities></server-identities>
               <authentication></authentication>
               <authorization></authorization>
            </security-realm>
 *
 * @author Roman Jakubco
 */
@ConfigPartDescriptor(
    name = "Security (JAAS) configuration",
    docLink = "https://access.redhat.com/site/documentation//en-US/JBoss_Enterprise_Application_Platform/5/html/Security_Guide/index.html"
)
public class SecurityMigrator extends AbstractMigrator {
    private static final Logger log = LoggerFactory.getLogger(SecurityMigrator.class);

    @Override protected String getConfigPropertyModuleName() { return "security"; }
    

    private static final String AS7_CONFIG_DIR_PLACEHOLDER = "${jboss.server.config.dir}";
    
    
    /**
     *  Data of SecurityMigrator.
     */
    public class Data extends MigratorData {
        /** For other migrators to look up. */
        private Map<String, SecurityDomainBean> nameToSecDomainMap = new HashMap();
        private Set<ApplicationPolicyBean> policies;
        
        @Override public <T extends IConfigFragment> List<T> getConfigFragments() {
            return new ArrayList( policies );
        }
        
        public SecurityDomainBean findSecurityDomain( String name ) {
            return nameToSecDomainMap.get( name );
        }
        
        public SecurityDomainBean getSecurityDomain( String name ) throws MigrationException {
            SecurityDomainBean sd = findSecurityDomain( name );
            if( sd == null)
                throw new MigrationException("Application policy (security domain) '"+name+"' not found in login-config.xml.");
            return sd;
        }
    }



    public SecurityMigrator(GlobalConfiguration globalConfig) {
        super(globalConfig);
    }

    /**
     *  Loads the AS 5 data.
     */
    @Override
    public void loadSourceServerConfig(MigrationContext ctx) throws MigrationException {
        try {
            File file = new File(getGlobalConfig().getAS5Config().getConfDir(), "login-config.xml");
            if( ! file.canRead() )
                throw new LoadMigrationException( "Can't read: " + file.getAbsolutePath() );

            //Unmarshaller unmarshaller = JAXBContext.newInstance(SecurityAS5Bean.class).createUnmarshaller();
            //SecurityAS5Bean securityAS5 = (SecurityAS5Bean) unmarshaller.unmarshal(file);
            //XmlUtils.unmarshallBeans( file, "/policy/application-policy", ApplicationPolicyBean.class );
            final SecurityAS5Bean securityAS5 = XmlUtils.unmarshallBean( file, SecurityAS5Bean.class );

            final Data mData = new Data();
            final Origin origin = new Origin( file );
            mData.policies = securityAS5.getApplicationPolicies();
            for( ApplicationPolicyBean policy : mData.policies )
                policy.setOrigin( origin );
            
            ctx.getMigrationData().put( SecurityMigrator.class, mData );
        } catch( Exception ex ) {
            throw new LoadMigrationException(ex);
        }
    }

    
    /**
     *  Creates the actions.
     */
    @Override
    public void createActions(MigrationContext ctx) throws MigrationException {
        SecurityMigResource resource = new SecurityMigResource();
        
        Data data = (Data) ctx.getMigrationData().get(SecurityMigrator.class);

        // Config fragments
        for( IConfigFragment fragment : data.getConfigFragments()) {
            // Unknown
            if( ! (fragment instanceof ApplicationPolicyBean) )
                throw new MigrationException("Config fragment unrecognized by " + this.getClass().getSimpleName() + ": " + fragment);
            
            final ApplicationPolicyBean policy = (ApplicationPolicyBean) fragment;
            log.debug("    Processing policy " + policy);
            
            // ApplicationPolicy
            try {
                SecurityDomainBean secDomain = migrateAppPolicy( policy, ctx, resource);
                final List<CliCommandAction> actions = createSecurityDomainCliActions( secDomain );
                if( ! actions.isEmpty() )
                    data.nameToSecDomainMap.put( secDomain.getSecurityDomainName(), secDomain );
                ctx.getActions().addAll( actions );
            } catch (CliScriptException ex) {
                throw new MigrationException("Migration of <application-policy> failed: " + ex.getMessage(), ex);
            }
        }
    }

    
    
    
    /**
     * Migrates application-policy from AS5 to AS7.
     */
    public SecurityDomainBean migrateAppPolicy(ApplicationPolicyBean appPolicy, MigrationContext ctx,
                                               SecurityMigResource resource) throws MigrationException{
        Set<LoginModuleAS7Bean> loginModules = new HashSet();
        SecurityDomainBean securityDomain = new SecurityDomainBean();

        securityDomain.setSecurityDomainName( appPolicy.getApplicationPolicyName() );
        securityDomain.setCacheType( "default" );
        if( appPolicy.getLoginModules() != null ) {
            for( LoginModuleBean lmAS5 : appPolicy.getLoginModules() ) {
                loginModules.add( createLoginModule( lmAS5, resource, ctx ) );
            }
        }

        securityDomain.setLoginModules( loginModules );

        return securityDomain;
    }

    
    /**
     *  Migrates the given login module.
     */
    private LoginModuleAS7Bean createLoginModule(LoginModuleBean lmAS5, SecurityMigResource resource, MigrationContext ctx )
            throws MigrationException{
        LoginModuleAS7Bean lmAS7 = new LoginModuleAS7Bean();

        // Flag
        lmAS7.setFlag( lmAS5.getFlag() );
        
        // Code
        String lmName = deriveLoginModuleName( lmAS5.getCode() );
        lmAS7.setCode( lmName );
        if( lmName.equals( lmAS5.getCode() ) ) {
            ModuleCreationAction action = createModuleActionForLogMod( lmAS7, lmName, resource );
            if(action != null)
                ctx.getActions().add( action );
        }

        // Module options
        
        if( lmAS5.getOptions() == null )
            return lmAS7;
        
        // Can't just copy - we have to take care of specific module options.
        Set<LoginModuleOptionBean> moduleOptions = new HashSet();
        for( LoginModuleOptionBean moAS5 : lmAS5.getOptions() ){
            String value;
            switch( moAS5.getName() ){
                case "rolesProperties":
                case "usersProperties":
                    String fName = new File( moAS5.getValue() ).getName();
                    value = AS7_CONFIG_DIR_PLACEHOLDER + "/" + fName;
                    if(resource.getFileNames().add(fName)){
                        CopyFileAction action = createCopyActionForFile(resource, fName);
                        if( action != null)
                            ctx.getActions().add( action );
                    }
                    break;
                default:
                    value = moAS5.getValue();
                    break;
            }
            moduleOptions.add( new LoginModuleOptionBean( moAS5.getName(), value ) );
        }
        lmAS7.setOptions(moduleOptions);

        return lmAS7;
    }

    /**
     * Creates CopyFileAction for File referenced in migrated Module-Options
     *
     * @param resource  Helper class containing all resources of the SecurityMigrator
     * @param fileName  File to be copied into AS7
     * @return  Null if the file is already set for copying; otherwise, the created CopyFileAction
     */
    private  CopyFileAction createCopyActionForFile( SecurityMigResource resource, String fileName ) {

        if( ! resource.getFileNames().add(fileName) ) return null;

        File src;
        try {
            // TODO: MIGR-54 The paths in AS 5 config relate to some base dir. Find out which and use that, instead of searching.
            //       Then, create the actions directly in the code creating this "files to copy" collection.
            src = Utils.searchForFile(fileName, getGlobalConfig().getAS5Config().getProfileDir()).iterator().next();
        } catch( FileNotFoundException ex ) {
            //throw new ActionException("Failed copying a security file: " + ex.getMessage(), ex);
            // Some files referenced in security may not exist. (?)
            log.warn("Couldn't find file referenced in AS 5 security config: " + fileName);
            return null;
        }

        File target = Utils.createPath( getGlobalConfig().getAS7Config().getConfigDir(), src.getName() );
        CopyFileAction action = new CopyFileAction( this.getClass(), src, target, CopyFileAction.IfExists.WARN );

        return action;
    }

    /**
     * Creates ModuleCreationAction for the custom made class for the Login-Module, which should be deployed as module.
     *
     * @param lmAS7      Login module containing this class
     * @param className  Custom made class, which should be deployed into AS7
     * @param resource   Contains all resources of the SecurityMigrator
     * @return  null if the JAR file containing the given class is already set for the creation of the module;
     *          the created ModuleCreationAction otherwise.
     */
    private ModuleCreationAction createModuleActionForLogMod( LoginModuleAS7Bean lmAS7, String className, SecurityMigResource resource )
            throws MigrationException {
        File fileJar;
        try {
            fileJar = UtilsAS5.findJarFileWithClass(className, getGlobalConfig().getAS5Config().getDir(),
                    getGlobalConfig().getAS5Config().getProfileName());
        } catch( IOException ex ) {
            throw new MigrationException("Failed finding jar with class " + className + ": " + ex.getMessage(), ex);
        }

        if( resource.getModules().containsKey(fileJar) ) {
            lmAS7.setModule( resource.getModules().get(fileJar) );
            return null;
        }

        String moduleName = "security.loginModule" + resource.getIncrement();

        // Handler jar is new => create ModuleCreationAction, new module and CLI script
        lmAS7.setModule( moduleName );
        resource.getModules().put(fileJar, moduleName);

        // TODO: Dependencies are little unknown. This two are the best possibilities from example of custom login module
        String[] deps = new String[]{"javax.api", "org.picketbox", null}; // null = next is optional.

        ModuleCreationAction moduleAction = new ModuleCreationAction( this.getClass(), moduleName, deps, fileJar, Configuration.IfExists.OVERWRITE);

        return moduleAction;
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
     * Creates a list of CliCommandActions for adding a security domain.
     */
    private List<CliCommandAction> createSecurityDomainCliActions(SecurityDomainBean domain)
            throws CliScriptException {
        String errMsg = " in security-domain must be set.";
        Utils.throwIfBlank(domain.getSecurityDomainName(), errMsg, "Security name");

        List<CliCommandAction> actions = new LinkedList();
        
        // CLI ADD command
        ModelNode domainCmd = new ModelNode();
        domainCmd.get(ClientConstants.OP).set(ClientConstants.ADD);
        domainCmd.get(ClientConstants.OP_ADDR).add("subsystem", "security");
        domainCmd.get(ClientConstants.OP_ADDR).add("security-domain", domain.getSecurityDomainName());
        // Action
        CliCommandAction action = new CliCommandAction( SecurityMigrator.class, createSecurityDomainScript(domain), domainCmd);
        action.setIfExists( this.getIfExists() );
        actions.add( action );

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
     * @param domain  Security domain containing a login module.
     * @param module  Contained login-module.
     */
    private static CliCommandAction createLoginModuleCliAction(SecurityDomainBean domain, LoginModuleAS7Bean module) {
        ModelNode request = new ModelNode();
        request.get(ClientConstants.OP).set(ClientConstants.ADD);
        request.get(ClientConstants.OP_ADDR).add("subsystem", "security");
        request.get(ClientConstants.OP_ADDR).add("security-domain", domain.getSecurityDomainName());
        request.get(ClientConstants.OP_ADDR).add("authentication", "classic");

        ModelNode moduleNode = new ModelNode();
        ModelNode list = new ModelNode();

        if( module.getOptions() != null ) {
            ModelNode optionNode = new ModelNode();
            for( LoginModuleOptionBean option : module.getOptions() ) {
                optionNode.get( option.getName() ).set( option.getValue() );
            }
            moduleNode.get( "module-options" ).set( optionNode );
        }

        CliApiCommandBuilder builder = new CliApiCommandBuilder(moduleNode);
        builder.addPropertyIfSet("flag", module.getFlag());
        builder.addPropertyIfSet("code", module.getCode());

        // Needed for CLI because parameter login-modules requires LIST
        list.add(builder.getCommand());

        request.get("login-modules").set(list);

        return new CliCommandAction( SecurityMigrator.class, createLoginModuleScript(domain, module), request);
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
        Utils.throwIfBlank( securityDomain.getSecurityDomainName(), errMsg, "Security name" );

        CliAddScriptBuilder builder = new CliAddScriptBuilder();
        StringBuilder resultScript = new StringBuilder("/subsystem=security/security-domain=");

        resultScript.append( securityDomain.getSecurityDomainName() ).append(":add(");
        builder.addProperty("cache-type", securityDomain.getCacheType() );

        resultScript.append( builder.formatAndClearProps() ).append(")");

        return resultScript.toString();
    }

    /**
     * Creates a CLI script for adding a login module of the specific Security-Domain
     *
     * @param domain Security-Domain containing login module
     * @param module login module
     * @return created string containing the CLI script for adding the login module
     * 
     * TODO: Rewrite using ModuleNode.
     */
    private static String createLoginModuleScript(SecurityDomainBean domain, LoginModuleAS7Bean module) {
        
        StringBuilder resultScript = new StringBuilder( "/subsystem=security/security-domain=" + domain.getSecurityDomainName() );
        resultScript.append("/authentication=classic:add(login-modules=[{");

        if( (module.getCode() != null) && ! module.getCode().isEmpty() ) {
            resultScript.append("\"code\"=>\"" ).append( module.getCode() ).append("\"");
        }
        if( (module.getFlag() != null) && ! module.getFlag().isEmpty() ) {
            resultScript.append(", \"flag\"=>\"").append( module.getFlag() ).append("\"");
        }

        if( (module.getOptions() != null) && ! module.getOptions().isEmpty() ) {
            StringBuilder sbModules = new StringBuilder();
            for( LoginModuleOptionBean moduleOptionAS7 : module.getOptions() ) {
                sbModules.append(", (\"").append( moduleOptionAS7.getName() ).append("\"=>");
                sbModules.append("\"").append( moduleOptionAS7.getValue() ).append("\")");
            }

            String modules = sbModules.toString().replaceFirst(",", "").replaceFirst(" ", "");
            if( ! modules.isEmpty() )
                resultScript.append(", \"module-option\"=>[").append(modules).append("]");
        }

        return resultScript.toString();
    }
    
}// class
