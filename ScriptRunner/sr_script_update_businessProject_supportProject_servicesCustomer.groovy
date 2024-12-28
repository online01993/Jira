import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.CustomField
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
@WithPlugin("com.riadalabs.jira.plugins.insight")
@PluginModule IQLFacade iqlFacade

/* Custom field for the waitng field */
CustomField resolutionCF = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11106);
final def searchJql = 'Организация is not EMPTY and Контакт is not EMPTY and Офис_Подразделение is not EMPTY and resolution is not EMPTY and Решение is not EMPTY and project = its and ("Бизнеc направление" is EMPTY OR "Направление поддержки" is EMPTY OR Услуги is EMPTY)'
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
        setEventDispatchOption(EventDispatchOption.DO_NOT_DISPATCH)
        setSendEmail(false)
        }
        log.warn('issue - ' + issue.getKey())
    }
}
