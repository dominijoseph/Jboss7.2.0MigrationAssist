package cz.muni.fi.jboss.migration.migrators.trash;

import cz.muni.fi.jboss.migration.Configuration;
import cz.muni.fi.jboss.migration.GlobalConfiguration;
import cz.muni.fi.jboss.migration.MigrationContext;
import cz.muni.fi.jboss.migration.Migrator;
import cz.muni.fi.jboss.migration.migrators.connectionFactories.ResAdapterMigrator;
import cz.muni.fi.jboss.migration.migrators.dataSources.DatasourceMigrator;
import cz.muni.fi.jboss.migration.migrators.logging.LoggingMigrator;
import cz.muni.fi.jboss.migration.migrators.security.SecurityMigrator;
import cz.muni.fi.jboss.migration.migrators.server.ServerMigrator;
import cz.muni.fi.jboss.migration.spi.IMigrator;
import javafx.util.Pair;
import org.apache.commons.collections.map.MultiValueMap;
import org.w3c.dom.Node;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Roman Jakubco
 *         Date: 8/26/12
 *         Time: 3:15 PM
 */
public class Main {
    public static void main(String[] args) {
        // Basic implementation for testing
        try {
//            final JAXBContext context = JAXBContext.newInstance(DataSourcesBean.class) ;
//            final JAXBContext context1 = JAXBContext.newInstance(DatasourcesSub.class);
//            final JAXBContext context2=JAXBContext.newInstance(ResourceAdaptersSub.class);
//            final JAXBContext context3=JAXBContext.newInstance(ConnectionFactoriesBean.class);
//            final JAXBContext context4=JAXBContext.newInstance(ServerAS5Bean.class);
//            final JAXBContext context5=JAXBContext.newInstance(ServerSub.class);
//            final JAXBContext context6=JAXBContext.newInstance(SocketBindingGroup.class);
//            final JAXBContext context7=JAXBContext.newInstance(LoggingAS5Bean.class);
//            final JAXBContext context8=JAXBContext.newInstance(LoggingAS7.class);
//            final JAXBContext context9=JAXBContext.newInstance(SecurityAS7.class);
//            final JAXBContext context10=JAXBContext.newInstance(SecurityAS5Bean.class);
//
//            Unmarshaller unmarshaller=context.createUnmarshaller();
//            Unmarshaller unmarshaller3=context3.createUnmarshaller();
//            Unmarshaller unmarshaller4=context4.createUnmarshaller();
//            Unmarshaller unmarshaller7=context7.createUnmarshaller();
//            Unmarshaller unmarshaller10=context10.createUnmarshaller();
//
//            Collection<DataSourcesBean> dataSourcesCollection = new ArrayList<>();
//
//            DataSourcesBean dataSources=(DataSourcesBean)unmarshaller.unmarshal(new File("datasources.xml"));
//            dataSourcesCollection.add(dataSources);
//
//            ServerAS5Bean serverAS5=(ServerAS5Bean)unmarshaller4.unmarshal(new File("server.xml"));
//            LoggingAS5Bean loggingAS5= (LoggingAS5Bean)unmarshaller7.unmarshal(new File("logging.xml"));
//            SecurityAS5Bean securityAS5=(SecurityAS5Bean)unmarshaller10.unmarshal(new File("security.xml"));
//            ConnectionFactoriesBean connectionFactories=(ConnectionFactoriesBean)unmarshaller3.unmarshal(new File("resourceAdapters.xml"));

//            Collection<ConnectionFactoriesBean> cont = new ArrayList<>();
//            cont.add(connectionFactories);

            Configuration conf = new Configuration();
            GlobalConfiguration global = new GlobalConfiguration();
            global.setDirAS5("/home/Reon/Programing/jboss-5.1.0.GA/server/");
            global.setDirAS7("/home/Reon/Programing/jboss-as-7.2.0.Alpha1-SNAPSHOT");

            global.setStandalonePath();

            Map<Class<? extends IMigrator>, MultiValueMap> moduleOtions = new HashMap<>();
            MultiValueMap temp = new MultiValueMap();
            moduleOtions.put(DatasourceMigrator.class, temp);
            moduleOtions.put(ServerMigrator.class, temp);
            moduleOtions.put(LoggingMigrator.class, temp);
            moduleOtions.put(SecurityMigrator.class, temp);
            moduleOtions.put(ResAdapterMigrator.class, temp);


            conf.setModuleOtions(moduleOtions);
            conf.setOptions(global);

            MigrationContext ctx = new MigrationContext();
            ctx.createBuilder();
            Migrator migrator = new Migrator(conf, ctx);

            migrator.loadAS5Data();
            //migrator.apply();
            for (String s : migrator.getCLIScripts()) {
                System.out.println(s);
            }


            List<Node> nodeList = migrator.getDOMElements();
            for (Node n : nodeList) {
                StringWriter out = new StringWriter();
                Transformer xform = TransformerFactory.newInstance().newTransformer();
                xform.setOutputProperty(OutputKeys.INDENT, "yes");
                xform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                xform.transform(new DOMSource(n), new StreamResult(out));
                System.out.println(out.toString());
            }


//            Migration migration=new MigrationImpl(true);
//
//            final StringWriter writer=new StringWriter();
//
//            // Datasource Marshaller
//            Marshaller marshaller=context1.createMarshaller();
//            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
//            marshaller.marshal(migration.datasourceSubMigration(dataSourcesCollection),writer);
//            writer.write("\n\n");
//
//            // Server config Marshaller
//            Marshaller marshaller4=context5.createMarshaller();
//            marshaller4.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
//            marshaller4.marshal(migration.serverMigration(serverAS5) ,writer);
//            writer.write("\n\n");
//
//            // SocketBindingBean marshaller
//            Marshaller marshaller1 = context6.createMarshaller();
//            marshaller1.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
//            marshaller1.marshal(migration.getSocketBindingGroup(),writer);
//            writer.write("\n\n");
//
//            // Logging marshaller
//            Marshaller marshaller7=context8.createMarshaller();
//            marshaller7.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
//            marshaller7.marshal(migration.loggingMigration(loggingAS5) ,writer);
//             writer.write("\n\n");
//
//            // Security Marshaller
//            Marshaller marshaller10=context9.createMarshaller();
//            marshaller10.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
//            marshaller10.marshal(migration.securityMigration(securityAS5) ,writer);
//            writer.write("\n\n");
//
//            // Resource adapters Marshaller
//            Marshaller marshaller3=context2.createMarshaller();
//            marshaller3.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
//            marshaller3.marshal(migration.resourceAdaptersMigration(cont) ,writer);
//
//            System.out.println(writer.toString());
//
//            CliScript cliScript= new CliScriptImpl();
//            DatasourcesSub datasourcesSub= migration.datasourceSubMigration(dataSourcesCollection);
//            for(DatasourceAS7Bean datasourceAS7 : datasourcesSub.getDatasource()){
//                System.out.println(cliScript.createDatasourceScriptOld(datasourceAS7));
//            }
//            for(XaDatasourceAS7Bean xaDatasourceAS7 : datasourcesSub.getXaDatasource()){
//                System.out.println(cliScript.createXaDatasourceScriptOld(xaDatasourceAS7));
//            }
//            //ResourceAdaptersSub connectionFactoriesSub = migration.connectionFactoriesMigration(connectionFactories);
//            //for(ResourceAdapterBean connectionFactoryAS7 : connectionFactoriesSub.getResourceAdapters()){
//            //   System.out.println(cliScript.createResAdapterScript(connectionFactoryAS7));
//            //}
//            LoggingAS7 loggingAS7 = migration.loggingMigration(loggingAS5);
//            for(LoggerBean logger : loggingAS7.getLoggers()){
//                System.out.println(cliScript.createLoggerScript(logger));
//
//            }
//            SecurityAS7 securityAS7 = migration.securityMigration(securityAS5);
//            for(SecurityDomainBean securityDomain : securityAS7.getSecurityDomains()){
//                System.out.println(cliScript.createSecurityDomainScript(securityDomain));
//            }
//            ServerSub serverSub = migration.serverMigration(serverAS5);
//            for(ConnectorAS7Bean connectorAS7 : serverSub.getConnectors()){
//                System.out.println(cliScript.createConnectorScript(connectorAS7));
//            }
//            for(VirtualServerBean virtualServer : serverSub.getVirtualServers()){
//                System.out.println(cliScript.createVirtualServerScript(virtualServer));
//            }
//            SocketBindingGroup socketBindingGroup= migration.getSocketBindingGroup();
//
//
//            for(SocketBindingBean socketBinding : socketBindingGroup.getSocketBindings()){
//                System.out.println(cliScript.createSocketBinding(socketBinding));
//            }
//
//            System.out.println(cliScript.createHandlersScript(loggingAS7));


//        } catch (JAXBException e) {
//            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}