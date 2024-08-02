import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.CustomField
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
@WithPlugin("com.riadalabs.jira.plugins.insight")
@PluginModule IQLFacade iqlFacade

/* Custom field for the waitng field */
CustomField resolutionCF = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11106);
//final def searchJql = 'Решение is not EMPTY and Инфосистема in aqlFunction("objectType = Сервер") and "Направление поддержки" = "Консалтинг ИТ (CRM-2529)" and контакт is not EMPTY'// and \\"Тип устройства\\" = \\"Сетевое хранилище NAS\\"")'
final def searchJql = 'Услуги = CRM-182'
def issues = Issues.search(searchJql.toString())
for (issue in issues) {
    def valueresolutionCF = issue.getCustomFieldValue(resolutionCF);               
    /* Get Insight IQL Facade from plugin accessor */
    //Class iqlFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().loadClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade");
    //def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(iqlFacadeClass);

    /* Get IQL search and return true?false       */
    def businessProject = iqlFacade.findObjects(3, 'objectTypeId = 56 AND Проект = ITS and object HAVING inboundReferences(objectTypeId = 33 and object HAVING inboundReferences(objectTypeId = 146 and KEY IN (' + valueresolutionCF[0].getObjectKey().toString() + ')))')
    def supportProject = iqlFacade.findObjects(3, 'objectTypeId = 147 and object HAVING outboundReferences(objectTypeId = 56 AND Проект = ITS) and object HAVING inboundReferences(objectTypeId = 33 and object HAVING inboundReferences(objectTypeid = 146 and KEY IN (' + valueresolutionCF[0].getObjectKey().toString() + ')))')
    def servicesCustomer = iqlFacade.findObjects(3, 'objectTypeId = 33 and object HAVING outboundReferences(objectTypeId = 56 AND Проект = ITS) and object HAVING inboundReferences(objectTypeId = 146 and KEY IN (' + valueresolutionCF[0].getObjectKey().toString() + '))')
    if (valueresolutionCF != null) {
        issue.update { 
            if (!businessProject.isEmpty()) {
                setCustomFieldValue('Бизнеc направление', businessProject[0].getObjectKey())
            } else {
                return issue.getKey()
            }

            if (!supportProject.isEmpty()) {
                setCustomFieldValue('Направление поддержки', supportProject[0].getObjectKey())                
            } else {
                return issue.getKey()
            }
            
            if (!servicesCustomer.isEmpty()) { 
                setCustomFieldValue('Услуги', servicesCustomer[0].getObjectKey())
            } else {
                return issue.getKey()
            }        
        }
        log.warn('issue - ' + issue.getKey())
    }
}
