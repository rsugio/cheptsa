package io.rsug.cheptsa

import com.sap.esb.datastore.DataStore
import com.sap.esb.datastore.MetaDataProvider
import com.sap.it.action.types.ActionRequest
import com.sap.it.api.ITApiFactory
import com.sap.it.api.asdk.datastore.DataStoreService
import com.sap.it.api.asdk.runtime.Factory
import com.sap.it.api.securestore.SecureStoreService
import com.sap.it.api.securestore.UserCredential
import com.sap.it.co.api.types.deploy.artifact.Artifact
import com.sap.it.co.api.types.deploy.artifact.ArtifactType
import com.sap.it.linker.api.ArtifactAccess
import com.sap.it.linker.repo.artifact.ArtifactRepositoryComponent
import com.sap.it.linker.spi.EndpointQuery
import com.sap.it.linker.spi.WorkerHabitat
import com.sap.it.nm.node.NodeLocal
import com.sap.it.nm.security.SecureStore
import com.sap.it.nm.types.node.Node
import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.spi.Registry

class TestBedCF {
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
    ArtifactRepositoryComponent ar // com.sap.it.linker.repo.artifact.ArtifactRepositoryComponent
    EndpointQuery eq
    WorkerHabitat wh
    SecureStore ss
    ArtifactAccess aa
    com.sap.it.linker.spi.CommandProcessor cp

    TestBedCF(Exchange exchange) {
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
        ar = registry.lookupByName("com.sap.it.co.api.artifact.ArtifactRepository")
        aa = registry.lookupByName("com.sap.it.linker.api.ArtifactAccess")
        eq = registry.lookupByName("com.sap.it.linker.spi.EndpointQuery")
        wh = registry.lookupByName('com.sap.it.linker.spi.WorkerHabitat')
        ss = registry.lookupByName('com.sap.it.nm.security.SecureStore')
        cp = registry.lookupByName("com.sap.it.linker.spi.CommandProcessor")
    }

    String toString() {
        "TestBedCF works"
    }

    String log() {
        StringBuilder out = new StringBuilder()
        boolean doOut = true
        out << """
 ██████ ███████ 
██      ██      
██      █████   
██      ██      
 ██████ ██      

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
  .tenant.namespace = ${node.nodeCoordinate}
"""

        out << """\n\n## CF:
com.sap.it.co.api.artifact.ArtifactRepository = $ar
com.sap.it.linker.api.ArtifactAccess = $aa
com.sap.it.linker.spi.WorkerHabitat = $wh
com.sap.it.rt.jdbc.ds.artifact.reader.TenantInfoProvider = ${registry.lookupByName('com.sap.it.rt.jdbc.ds.artifact.reader.TenantInfoProvider')}
"""
        String tenantName = node.tenant.name

        out << """
WorkerHabitat: $wh
  .systemId = ${wh.systemId}
  .accountName = ${wh.accountName}
  .tenantInfo = ${wh.tenantInfo}
  .tenantInfo.status = ${wh.tenantInfo.status}
  .workerId = ${wh.workerId}
  .workerBasicInfo = ${wh.workerBasicInfo}
  .workerIndex = ${wh.workerIndex}
  .workerSetId = ${wh.workerSetId}
  
  неймспейсы см. в com.sap.it.co.api.artifact.ArtifactRepositoryNamespaces
"""
        int cc = 1
        out << "\n\n=====================================\nНеймспейс artifacts/dta\n"
        if (doOut) ar.getArtifactIds(tenantName, "artifacts/dta").each {
            out << "[$cc]\t$it"
            Artifact a = ar.getArtifact(tenantName, "artifacts/dta", it)
            out << "\t${a.artifactType}\t${a.symbolicName}\n"
            cc++
        }

        cc = 1
        out << "\n\n=====================================\nНеймспейс artifacts/${wh.workerSetId}\n"
        if (doOut) ar.getArtifactIds(tenantName, "artifacts/${wh.workerSetId}").each {
            out << "[$cc]\t$it"
            Artifact a = ar.getArtifact(tenantName, "artifacts/${wh.workerSetId}", it)
            out << "\t${a.artifactType}\t${a.symbolicName}\n"
            cc++
        }

        out << "\nArtifactAccess(BUNDLE):\n"
        cc=1
        if (doOut) aa.getArtifactsByType(ArtifactType.BUNDLE).each {a->
            out << "[$cc]\t${a.symbolicName}\n"
            cc++
        }

        out << "\nArtifactAccess(NUMBER_RANGE_CONFIGURATION):\n"
        cc=1
        if (doOut) aa.getArtifactsByType(ArtifactType.NUMBER_RANGE_CONFIGURATION).each {a->
            out << "[$cc]\t${a.symbolicName}\n"
            cc++
        }

        out << "\nArtifactAccess(SUBSYSTEM):\n"
        cc=1
        if (doOut) aa.getArtifactsByType(ArtifactType.SUBSYSTEM).each {a->
            out << "[$cc]\t${a.symbolicName}\n"
            cc++
        }

        //cp.process(new ActionRequest("",""))


        if (doOut) { //AS4
            com.sap.it.api.adapter.monitoring.AdapterEndpointInformationService aeis = registry.lookupByName("com.sap.it.api.adapter.monitoring.AdapterEndpointInformationService")

            aeis.adapterEndpointInformation.each { aei ->
                out << "${aei.integrationFlowId}\t\t${aei.adapterEndpointInstances}\n"
            }
        }
        return out.toString()
    }
}
