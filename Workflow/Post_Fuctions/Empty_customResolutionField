import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.bc.group.GroupService;
import com.atlassian.jira.user.ApplicationUser;

//CustomFieldManager customFieldManager = componentManager.getCustomFieldManager();
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def customFieldResolution =  ComponentAccessor.getCustomFieldManager().getCustomFieldObject(10802);// here replace the ID with ID of your custom field.
issue.setCustomFieldValue(customFieldResolution, null)
