import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.event.type.EventDispatchOption;
 
 
/* Custom field with the value to filter on */
CustomField valueCF = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11800);        
 
if (valueCF == null || valueCF.getValue(issue) == null) {
    return true;
}
 
/* Insight custom field to set */
CustomField insightCF = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(10301);       
 
if (insightCF == null) {
    return true;
}
 
/* Get Insight IQL Facade from plugin accessor */
Class iqlFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade"); 
def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(iqlFacadeClass);
 
/* Specify the schema id as well as the IQL that will fetch objects. In this case all objects with Name matching the valueCF, be sure to include " around value */
def objects = iqlFacade.findObjectsByIQLAndSchema(9, "objectType = Компании and object HAVING inboundReferences(objectType = Сотрудники and \"" + "JIRA User\" = " + valueCF.getValue(issue) + ")");
if (!objects.isEmpty()) {
   MutableIssue mi = (MutableIssue) issue;
    mi.setCustomFieldValue(insightCF, objects);
    ComponentAccessor.getIssueManager().updateIssue(currentUser, mi, EventDispatchOption.DO_NOT_DISPATCH, false);
}
else {
    objects = iqlFacade.findObjectsByIQLAndSchema(9, "objectType = Компании and object HAVING inboundReferences(objectType = Сотрудники and \"" + "Почтовый адрес\" = " + valueCF.getValue(issue) + ")");
	if (!objects.isEmpty()) {
		MutableIssue mi = (MutableIssue) issue;
		mi.setCustomFieldValue(insightCF, objects);
		ComponentAccessor.getIssueManager().updateIssue(currentUser, mi, EventDispatchOption.DO_NOT_DISPATCH, false);
	}
}
return true;   
