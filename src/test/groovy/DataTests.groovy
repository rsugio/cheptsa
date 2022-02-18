import org.apache.camel.CamelContext
import org.junit.Test

import javax.sql.DataSource

class DataTests {
    @Test
    void dataStore() {
        CamelContext camelCtx = null
//        DataStore dataStore = (DataStore)camelCtx.registry.findByTypeWithName(DataStore)
//        DataStoreImpl dataStoreImpl = dataStore
//        javax.sql.DataSource dataSource = null
//        // Читаем
//        byte[] payload = new byte[16]
//        Data data
//        ExtendedData edata = new ExtendedData(0, "1", "", "", "mplID", payload, [:], 1, new Date(), new Date(), new Date())
//        BaseData baseData = new BaseData("store", "qualifier", "id")
//
//        edata = dataStoreImpl.get(1L)   //tid, transaction id
//        edata = dataStoreImpl.get("store", "entryName")
//        edata = dataStoreImpl.get("store", "entryName", "id")
//        edata = dataStoreImpl.get(dataSource, "store", "entryName", "id")
//        payload = edata.getDataAsArray()
//
//        dataStoreImpl.put(data, false, 0L, 1L)
//        dataStoreImpl.put(data, true, false, 0L, 1L)    //+overwrite
//        dataStoreImpl.put(dataSource, data, true, false, 0L, 1L)    //+overwrite
//        dataStoreImpl.put(data, baseData, false,  0L, 1L)
//        dataStoreImpl.put(datasource, data, baseData, false,  0L, 1L)
//
//        dataStoreImpl.delete(1L) //tid
//        dataStoreImpl.delete("store", "entryName")
//        dataStoreImpl.delete("store", "qualifier", "id")
//        dataStoreImpl.delete(dataSource, "store", "qualifier", "id")
//        dataStoreImpl.delete(dataSource, 1L) //tid
//
//        dataStoreImpl.deleteStore("store", "qualifier", 100)
//        dataStoreImpl.deleteStore("store", "qualifier", 100, ["mplId"])
//        dataStoreImpl.deleteExpired(100)    //
//
//        dataStoreImpl.activate()
//        MetaData md = dataStoreImpl.getMetaData(null, "store", "qualifier", "id")
//
//        dataStoreImpl.countEntries("storeName")
//        dataStoreImpl.countStores(false, null)
//
//        DataStoreTable dst = dataStoreImpl.getDataStoreTable()
//        println(dst.tableName)

    }
}
