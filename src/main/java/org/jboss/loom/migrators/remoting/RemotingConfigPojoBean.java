package org.jboss.loom.migrators.remoting;

import org.eclipse.persistence.oxm.annotations.XmlPath;

/**
 * Remoting config in the form of JBoss MicroContainer POJO.
 * 

     <bean name="JBMConnector" class="org.jboss.remoting.transport.Connector">
       <annotation>@org.jboss.aop.microcontainer.aspects.jmx.JMX
          (name="jboss.messaging:service=Connector, transport=bisocket", exposedInterface=org.jboss.remoting.transport.ConnectorMBean.class,
        registerDirectly=true)</annotation>
       <property name="serverConfiguration"><inject bean="JBMConfiguration"/></property>
     </bean>
     
     <!-- Remoting server configuration -->
     <bean name="JBMConfiguration" class="org.jboss.remoting.ServerConfiguration">
       <constructor> <parameter>bisocket</parameter> </constructor>
     
       <!-- Parameters visible to both client and server -->
       <property name="invokerLocatorParameters">
         <map keyClass="java.lang.String" valueClass="java.lang.String">
            <entry>
              <key>serverBindAddress</key>
              <value> <value-factory bean="ServiceBindingManager" method="getStringBinding"> <parameter>JBMConnector</parameter> <parameter>${host}</parameter> </value-factory> </value>
            </entry>
            <entry>
              <key>serverBindPort</key>
              <value>
                <value-factory bean="ServiceBindingManager" method="getStringBinding"> <parameter>JBMConnector</parameter> <parameter>${port}</parameter> </value-factory> </value>
            </entry>
               ...
            <entry><key>marshaller</key> <value>org.jboss.jms.wireformat.JMSWireFormat</value></entry>
            <entry><key>unmarshaller</key> <value>org.jboss.jms.wireformat.JMSWireFormat</value></entry>


            <!-- A socket transport parameter -->
            <entry><key>enableTcpNoDelay</key> <value>true</value></entry>

            <!-- Selected optional parameters: -->

            <!-- Parameters for connecting from outside of a firewall. -->
            <!--entry><key>clientConnectAddress</key> <value>a.b.c.d</value></entry-->
            <!--entry><key>clientConnectPort</key>    <value>7777</value></entry-->

            <!-- Parameter for expressing a set of addresses to which a client can try to connect. -->
            <!-- The server could be, for example, a multihome server behind a firewall.  The      -->
            <!-- "homes4" bean could be defined the same way the "homes2" bean is defined below.   -->
            <!--entry>
               <key>connecthomes</key>
               <value><value-factory bean="homes4" method="toString"/></value>
            </entry-->

            <!-- Socket read timeout.  Defaults to 60000 ms (1 minute) -->
            <!-- on the server, 1800000 ms (30 minutes) on the client. -->
            <!--entry><key>timeout</key> <value>120000</value></entry-->

            <!-- Maximum number of connections in client invoker's    -->
            <!-- connection pool (socket transport).  Defaults to 50. -->
            <!--entry><key>clientMaxPoolSize</key> <value>20</value></entry-->

            <!-- Configures traffic class on underlying sockets (socket transport). -->
            <!-- Default value determined by socket implementation.                 -->
            <!--entry><key>trafficClass</key> <value>2</value></entry-->

         </map>
       </property>
       
       <!-- Parameters visible only to server -->
       <property name="serverParameters">
         <map keyClass="java.lang.String" valueClass="java.lang.String">
           <entry><key>callbackTimeout</key> <value>10000</value></entry>
         </map>
       </property>
        ...
     </bean>
 *
 * @author Ondrej Zizka, ozizka at redhat.com
 */
public class RemotingConfigPojoBean extends RemotingConfigBean {
    
     @XmlPath("bean[@class='org.jboss.remoting.ServerConfiguration']/property[@name='invokerLocatorParameters']/map/entry[key[text()='marshaller']]/value/text()")
     @Override public String getMarshaller() { return super.getMarshaller(); }
     
     @XmlPath("bean[@class='org.jboss.remoting.ServerConfiguration']/property[@name='invokerLocatorParameters']/map/entry[key[text()='unmarshaller']]/value/text()")
     @Override public String getUnmarshaller() { return super.getUnmarshaller(); }

     @XmlPath("bean[@class='org.jboss.remoting.ServerConfiguration']/constructor/parameter/text()")
     @Override public String getProtocol() { return super.getProtocol(); }
     
     @XmlPath("bean[@class='org.jboss.remoting.ServerConfiguration']/property[@name='invokerLocatorParameters']/map/entry[key[text()='serverBindAddress']]/value/value-factory/parameter[2]/text()")
     @Override public String getServerBindAddress() { return super.getServerBindAddress(); }
     // ${host} ;  calls ServiceBindingManager.getStringBinding()
     
     @XmlPath("bean[@class='org.jboss.remoting.ServerConfiguration']/property[@name='invokerLocatorParameters']/map/entry[key[text()='serverBindPort']]/value/value-factory/parameter[2]/text()")
     @Override public String getServerBindPort() { return super.getServerBindPort(); }
     // ${port}   ;  calls ServiceBindingManager.getStringBinding()
     
     
}// class
