//Комментируем переход в ожидание
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.Issue;


//def issue = Issues.getByKey('ITS-87885')
/* Custom field with the value to filter on (blocking)*/
CustomField cfblocking = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12401);
def valueCFcfblocking = issue.getCustomFieldValue(cfblocking);
if (valueCFcfblocking == null) {
    valueCFcfblocking = issue.getCustomFieldValue(cfblocking);
} else {
    valueCFcfblocking = issue.getCustomFieldValue(cfblocking).flatten().join(", ");
}   
/* Custom field for the waitng field */
CustomField cfwaitinginsight = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12400);
def valuecfwaitinginsight = issue.getCustomFieldValue(cfwaitinginsight);               
/* Get Insight Object Facade from plugin accessor */
Class objectFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade");  
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectFacadeClass);
/* Get Insight IQL Facade from plugin accessor */
Class iqlFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade");
def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(iqlFacadeClass);

 
/* Get IQL search and return true?false       */
def objects = iqlFacade.findObjectsByIQLAndSchema(9, "Блокируемые = true and Key = ID-" + valuecfwaitinginsight[0].getId()); 
if (objects.isEmpty()) {
        issue.set { 
        setComment("""
            *Задача переведена в статус Ожидание - ${valuecfwaitinginsight[0].getName()}*
        """)
        }
   } else if (!objects.isEmpty()) {
        issue.set { 
        setComment("""
            *Задача переведена в статус Ожидание - ${valuecfwaitinginsight[0].getName()}*
            *Задачи, которые блокируют выполненение текущего обращения - ${valueCFcfblocking}*
        """)
        }    
   }
