import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.bc.group.GroupService;
import com.atlassian.jira.user.ApplicationUser;

//CustomField
def customFieldOgr =  ComponentAccessor.getCustomFieldManager().getCustomFieldObject(10301);// here replace the ID with ID of your custom field.
def customFieldContact =  ComponentAccessor.getCustomFieldManager().getCustomFieldObject(10300);// here replace the ID with ID of your custom field.
 
def valueOrg = (String)issue.getCustomFieldValue(customFieldOgr);
def valueContact = (String)issue.getCustomFieldValue(customFieldContact);
def groupManager = ComponentAccessor.getGroupManager();

def user = ((ApplicationUser) issue.getReporter())
def group = groupManager.getGroup("jira-servicedesk-customers");

def cf = ComponentAccessor.customFieldManager.getCustomFieldObjectByName("Customer Request Type")
def requestType = issue.getCustomFieldValue(cf)
//return requestType - return UUID of custom issue type
//its/da0f2ac1-5182-445c-a4a0-c140801ff824 - Подтверждение учетной записи

 
//and now check
if((valueOrg == "[Service Desk portal ORG (ID-185)]") && (valueContact == "[Service Desk portal User (ID-336)]") && (requestType.toString() == "its/da0f2ac1-5182-445c-a4a0-c140801ff824")) {
    groupManager.addUserToGroup(user,group);
    return true;
}
else
    {
        return false;  
        } 
