import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.bc.group.GroupService;
import com.atlassian.jira.user.ApplicationUser;
import com.opensymphony.workflow.InvalidInputException

def currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
if(currentUser.name == "u.user") {
   throw new InvalidInputException("text")
}
