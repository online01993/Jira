import com.atlassian.jira.component.ComponentAccessor

/* Get Insight IQL Facade from plugin accessor */
Class iqlFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade");
def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(iqlFacadeClass);
def objectsInfosys = iqlFacade.findObjectsByIQLAndSchema(2, "Договор is EMPTY and Key not in (CMDB-3605, CMDB-122977, CMDB-122980, CMDB-3482, CMDB-3603, CMDB-3483, CMDB-121597, CMDB-4538, CMDB-121983, CMDB-3604, CMDB-4718)") 
for (object in objectsInfosys) {
    def asset = Assets.getByKey('CMDB-' + object.getId())
    def assetOrg = asset.getReferences('Организация')[0].getId()
    def assetInfoSysType = asset.getReferences('Тип устройства')[0].getId()
    def objectsSale = iqlFacade.findObjectsByIQLAndSchema(3, "object HAVING inboundReferences(objectTypeId = 423 and object HAVING inboundReferences(objectTypeId = 150 and object HAVING inboundReferences(objectTypeId = 38 and Key = ID-${assetOrg}))) and object HAVING outboundReferences(objecttype = Услуги and object having inboundReferences(objecttype = Резолюции and \"Тип устройства\" in CMDB-${assetInfoSysType}))") 
    //return objectsSale[0].getId()
    if (objectsSale.size() == 1) {
        asset.update { 
            setAttribute('Договор') {
                add('CRM-' + objectsSale[0].getId())
            }
        }
        log.warn('CRM-' + objectsSale[0].getId()) 
    } /*else if (objectsSale.size() > 1) {
        asset.update { 
            setAttribute('Договор') {
                add('CRM-121936')
            }
        }
    }*/
}
