//Валидатор проверки блокируемых резолюций ожидания

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.Issue;
import com.opensymphony.workflow.InvalidInputException;


//def issue = Issues.getByKey('ITS-87885')
/* Custom field with the value to filter on (blocking)*/
CustomField cfblocking = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12401);
def valueCFcfblocking = issue.getCustomFieldValue(cfblocking);        
/* Custom field for the waitng field */
CustomField cfwaitinginsight = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12400);
def valuecfwaitinginsight = issue.getCustomFieldValue(cfwaitinginsight);               
/* Get Insight Object Facade from plugin accessor */
Class objectFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade");  
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectFacadeClass);
/* Get Insight IQL Facade from plugin accessor */
Class iqlFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade");
def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(iqlFacadeClass);

/*exit if valuecfwaitinginsight is empty      */
if (valuecfwaitinginsight == null) {
    throw new InvalidInputException("Ожидание", "Ожидание должно быть заполнено")
}

/* Get IQL search and return true?false       */
def objects = iqlFacade.findObjectsByIQLAndSchema(9, "Блокируемые = true and Key = ID-" + valuecfwaitinginsight[0].getId()); 
if ((!objects.isEmpty()) && (valueCFcfblocking == null)) {
    throw new InvalidInputException("Блокируется", "Для данной категории ожидающих резолюции требуется указание блокирующих задач к текущей из-за которых невозможно дальнейшее продолжение работ")
   }
return true
