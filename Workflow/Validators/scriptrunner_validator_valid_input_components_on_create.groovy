import com.atlassian.jira.component.ComponentAccessor;
//import com.atlassian.jira.issue.Issue;
import com.opensymphony.workflow.InvalidInputException;


//def issue = Issues.getByKey('ITS-89790')
def groupManager = ComponentAccessor.getGroupManager();
def currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

for (int i = 0; i < issue.getComponents().size(); i++) {
    if ((groupManager.isUserInGroup(currentUser, "it-dept-common") == false ) && ((issue.getComponents()[i].getName() == "Плановые ПК") || (issue.getComponents()[i].getName() == "Плановые ИТ") || (issue.getComponents()[i].getName() == "Поручения"))) {
        throw new InvalidInputException("Компоненты", "Можно выбрать только Компонент Поддержка ПК или Поддержка ИТ или Поддержка VOIP или АУДИТЫ")
    }
}
return true
