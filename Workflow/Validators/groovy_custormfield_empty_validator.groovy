import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.bc.group.GroupService;
import com.atlassian.jira.user.ApplicationUser;
import com.opensymphony.workflow.InvalidInputException

//CustomField
def customFieldIssueType =  ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11101);// here replace the ID with ID of your custom field.
def customFieldSystem =  ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11105);// here replace the ID with ID of your custom field.
def customFieldResolution =  ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11106);// here replace the ID with ID of your custom field.

def valueIssueType = (String)issue.getCustomFieldValue(customFieldIssueType);
def valueSystem = (String)issue.getCustomFieldValue(customFieldSystem);
def valueResolution = (String)issue.getCustomFieldValue(customFieldResolution);

//and now check
if ((valueIssueType == null) | (valueSystem == null) | (valueResolution == null))
{
    throw new InvalidInputException("Необходимо заполнитель поля (Тип выполненной заявки, Инфосистема, Решение) по которым была обработана заявка.")
    }
