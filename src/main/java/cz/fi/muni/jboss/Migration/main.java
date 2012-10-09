package cz.fi.muni.jboss.Migration;

import cz.fi.muni.jboss.Migration.ConnectionFactories.ConnectionFactories;
import cz.fi.muni.jboss.Migration.ConnectionFactories.ResourceAdapter;
import cz.fi.muni.jboss.Migration.ConnectionFactories.ResourceAdaptersSub;
import cz.fi.muni.jboss.Migration.DataSources.DataSources;
import cz.fi.muni.jboss.Migration.DataSources.DatasourceAS7;
import cz.fi.muni.jboss.Migration.DataSources.DatasourcesSub;
import cz.fi.muni.jboss.Migration.DataSources.XaDatasourceAS7;
import cz.fi.muni.jboss.Migration.Logging.Logger;
import cz.fi.muni.jboss.Migration.Logging.LoggingAS5;
import cz.fi.muni.jboss.Migration.Logging.LoggingAS7;
import cz.fi.muni.jboss.Migration.Security.SecurityAS5;
import cz.fi.muni.jboss.Migration.Security.SecurityAS7;
import cz.fi.muni.jboss.Migration.Security.SecurityDomain;
import cz.fi.muni.jboss.Migration.Server.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: Roman Jakubco
 * Date: 8/26/12
 * Time: 3:15 PM
 */
public class main {
    public static void main(String[] args) {
        //basic implementation for testing
        try {
            final JAXBContext context = JAXBContext.newInstance(DataSources.class) ;
            final JAXBContext context1 = JAXBContext.newInstance(DatasourcesSub.class);
            final JAXBContext context2=JAXBContext.newInstance(ResourceAdaptersSub.class);
            final JAXBContext context3=JAXBContext.newInstance(ConnectionFactories.class);
            final JAXBContext context4=JAXBContext.newInstance(ServerAS5.class);
            final JAXBContext context5=JAXBContext.newInstance(ServerSub.class);
            final JAXBContext context6=JAXBContext.newInstance(SocketBindingGroup.class);
            final JAXBContext context7=JAXBContext.newInstance(LoggingAS5.class);
            final JAXBContext context8=JAXBContext.newInstance(LoggingAS7.class);
            final JAXBContext context9=JAXBContext.newInstance(SecurityAS7.class);
            final JAXBContext context10=JAXBContext.newInstance(SecurityAS5.class);

            Unmarshaller unmarshaller=context.createUnmarshaller();
            Unmarshaller unmarshaller3=context3.createUnmarshaller();
            Unmarshaller unmarshaller4=context4.createUnmarshaller();
            Unmarshaller unmarshaller7=context7.createUnmarshaller();
            Unmarshaller unmarshaller10=context10.createUnmarshaller();

            Collection<DataSources> dataSourcesCollection = new ArrayList<>();

            DataSources dataSources=(DataSources)unmarshaller.unmarshal(new File("datasources.xml"));
            dataSourcesCollection.add(dataSources);
            ServerAS5 serverAS5=(ServerAS5)unmarshaller4.unmarshal(new File("server.xml"));
            LoggingAS5 loggingAS5= (LoggingAS5)unmarshaller7.unmarshal(new File("logging.xml"));
            SecurityAS5 securityAS5=(SecurityAS5)unmarshaller10.unmarshal(new File("security.xml"));
            ConnectionFactories connectionFactories=(ConnectionFactories)unmarshaller3.unmarshal(new File("resourceAdapters.xml"));
            Migration migration=new MigrationImpl();

            final StringWriter writer=new StringWriter();

            //datasource Marshaller
            Marshaller marshaller=context1.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
            marshaller.marshal(migration.datasourceSubMigration(dataSourcesCollection),writer);
            writer.write("\n\n");

            //Server config Marshaller
            Marshaller marshaller4=context5.createMarshaller();
            marshaller4.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
            marshaller4.marshal(migration.serverMigration(serverAS5) ,writer);
            writer.write("\n\n");

            //SocketBinding marshaller
            Marshaller marshaller1 = context6.createMarshaller();
            marshaller1.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
            marshaller1.marshal(migration.getSocketBindingGroup(),writer);
            writer.write("\n\n");

            //Logging marshaller
            Marshaller marshaller7=context8.createMarshaller();
            marshaller7.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
            marshaller7.marshal(migration.loggingMigration(loggingAS5) ,writer);
             writer.write("\n\n");

            //Security Marshaller
            Marshaller marshaller10=context9.createMarshaller();
            marshaller10.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
            marshaller10.marshal(migration.securityMigration(securityAS5) ,writer);
            writer.write("\n\n");

            //resource adapters Marshaller
            Marshaller marshaller3=context2.createMarshaller();
            marshaller3.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
            marshaller3.marshal(migration.connectionFactoriesMigration(connectionFactories) ,writer);

            System.out.println(writer.toString());

            CliScript cliScript= new CliScriptImpl();
            DatasourcesSub datasourcesSub= migration.datasourceSubMigration(dataSourcesCollection);
            for(DatasourceAS7 datasourceAS7 : datasourcesSub.getDatasource()){
                System.out.println(cliScript.createDatasourceScript(datasourceAS7));
            }
            for(XaDatasourceAS7 xaDatasourceAS7 : datasourcesSub.getXaDatasource()){
                System.out.println(cliScript.createXaDatasourceScript(xaDatasourceAS7));
            }
              ResourceAdaptersSub connectionFactoriesSub= migration.connectionFactoriesMigration(connectionFactories);
             for(ResourceAdapter connectionFactoryAS7 : connectionFactoriesSub.getResourceAdapters()){
                 System.out.println(cliScript.createResourceAdapterScript(connectionFactoryAS7));
             }
            LoggingAS7 loggingAS7 = migration.loggingMigration(loggingAS5);
            for(Logger logger : loggingAS7.getLoggers()){
                System.out.println(cliScript.createLoggerScript(logger));

            }
            SecurityAS7 securityAS7 = migration.securityMigration(securityAS5);
            for(SecurityDomain securityDomain : securityAS7.getSecurityDomains()){
                System.out.println(cliScript.createSecurityDomainScript(securityDomain));
            }
            ServerSub serverSub = migration.serverMigration(serverAS5);
            for(ConnectorAS7 connectorAS7 : serverSub.getConnectors()){
                System.out.println(cliScript.createConnectorScript(connectorAS7));
            }
            for(VirtualServer virtualServer : serverSub.getVirtualServers()){
                System.out.println(cliScript.createVirtualServerScript(virtualServer));
            }
            SocketBindingGroup socketBindingGroup= migration.getSocketBindingGroup();


            for(SocketBinding socketBinding : socketBindingGroup.getSocketBindings()){
                System.out.println(cliScript.createSocketBinding(socketBinding));
            }

            System.out.println(cliScript.createHandlersScript(loggingAS7));

            System.out.println(cliScript.createDriverScript(datasourcesSub));


        } catch (JAXBException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
