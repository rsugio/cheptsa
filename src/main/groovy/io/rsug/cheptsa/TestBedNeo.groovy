package io.rsug.cheptsa

import com.sap.esb.datastore.DataStore
import com.sap.esb.datastore.MetaDataProvider
import com.sap.it.api.ITApiFactory
import com.sap.it.api.asdk.datastore.DataStoreService
import com.sap.it.api.asdk.runtime.Factory
import com.sap.it.api.securestore.SecureStoreService
import com.sap.it.api.securestore.UserCredential
import com.sap.it.nm.node.NodeLocal
import com.sap.it.nm.types.component.Component
import com.sap.it.nm.types.deploy.ArtifactDescriptor
import com.sap.it.nm.types.node.Node
import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.spi.Registry

class TestBedNeo {
    CamelContext ctx
    Registry registry

    DataStoreService dss
    SecureStoreService sss
    UserCredential uc
    DataStore dataStore
    MetaDataProvider metaDataProvider
    def configurationAdmin
    def componentMonitor // com.sap.it.nm.core.monitor.deadlock.DeadlockMonitor
    def managedComponentMonitor // com.sap.it.op.component.check.ManagedGenericMonitor
    NodeLocal nodeLocal // com.sap.it.nm.core.agent.ds.NodeAgentComponent
    Node node

    TestBedNeo(Exchange exchange) {
        sss = ITApiFactory.getService(SecureStoreService.class, null)
        dss = new Factory(DataStoreService.class).getService() as DataStoreService

        ctx = exchange.context
        registry = ctx.getRegistry()

        dataStore = registry.lookupByName(DataStore.class.name) as DataStore
        metaDataProvider = registry.lookupByName(MetaDataProvider.class.name) as MetaDataProvider
        configurationAdmin = registry.lookupByName("org.osgi.service.cm.ConfigurationAdmin")
        componentMonitor = registry.lookupByName("com.sap.it.nm.component.ComponentMonitor")
        managedComponentMonitor = registry.lookupByName("com.sap.it.nm.component.ManagedComponentMonitor")
        nodeLocal = registry.lookupByName("com.sap.it.nm.node.NodeLocal") as NodeLocal
        node = nodeLocal.getLocalNode()
    }

    String toString() {
        "TestBedNeo works"
    }

    String log() {
        StringBuilder out = new StringBuilder()
        out << """
███    ██ ███████  ██████  
████   ██ ██      ██    ██ 
██ ██  ██ █████   ██    ██ 
██  ██ ██ ██      ██    ██ 
██   ████ ███████  ██████  
                           
                           
Общедоступное: DataStoreService = $dss
SecureStoreService = $sss

Чуть скрытое:
DataStore = $dataStore
MetaDataProvider = $metaDataProvider
configurationAdmin = $configurationAdmin
  .listConfigurations(null) = ${configurationAdmin.listConfigurations(null)}
componentMonitor = $componentMonitor
managedComponentMonitor = $managedComponentMonitor
NodeLocal = $nodeLocal
  .getOperationsUri() = ${nodeLocal.getOperationsUri()}
Node
  .version = ${node.version}
  .type = ${node.nodeType}
  .state = ${node.state}
  .tags = ${node.tags}
  .tenant = ${node.tenant}
  .tenant.name = ${node.tenant.name}

## Компоненты
"""
        node.components.each { Component cmp ->
            out << "${cmp.type}\t${cmp.name}\n"
        }
        out << "\n\n## Задеплоенные артефакты\n"
        node.deployedArtifacts.each { ArtifactDescriptor ad ->
            out << "${ad.type}\t${ad.name}\n"
        }

        return out.toString()
    }
}
