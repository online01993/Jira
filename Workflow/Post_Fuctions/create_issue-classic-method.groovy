import com.atlassian.jira.issue.IssueInputParametersImpl
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.util.json.JSONObject
import com.atlassian.jira.issue.comments.CommentManager
import com.atlassian.sal.api.component.ComponentLocator

def issueService = ComponentLocator.getComponent(IssueService)
def projectManager = ComponentAccessor.projectManager
def authenticationContext = ComponentAccessor.jiraAuthenticationContext
def project = projectManager.getProjectByCurrentKey('ITS')
def loggedInUser = authenticationContext.getLoggedInUser()
def issueType = project.issueTypes.find { it.name == 'Сервисный запрос' }
def description =  """
    h2. *Головная SUP задача, по распределению - (${issue.getKey()}).*
    Сотрудник, распределивший головную задачу в ИТ - ${ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser().getDisplayName()};

    Описание ИТ задачи:

    """
def issueInputParameters = new IssueInputParametersImpl()
    .setSummary(issue.summary)
    .setDescription(description + issue.description)
    .setReporterId(loggedInUser.name)
    .setIssueTypeId(issueType.id)
    .setProjectId(project.id)
    .addCustomFieldValue(12505, issue.getKey().toString())
    .addCustomFieldValue(10301, issue.getCustomFieldValue('Организация')[0].getName().toString()) 
    .addCustomFieldValue(10300, issue.getCustomFieldValue('Контакт')[0].getName().toString())
def validationResult = issueService.validateCreate(loggedInUser, issueInputParameters)
if (validationResult.valid) {
    def creationResult = issueService.create(loggedInUser, validationResult)
    if (creationResult.valid) {
        //log.debug(creationResult.issue)
        issue.set { 
            setCustomFieldValue(12506, creationResult.issue)
        }
    } else {
        CommentManager commentManager = ComponentAccessor.getCommentManager()
        def comment = """h2. *Задача ИТ не была автоматически создана, поэтому необходимо повторно провести процедуру распределения либо обратиться к администратору Jira.*
        """
        final SD_PUBLIC_COMMENT = "sd.public.comment"
        def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": true] as Map)]
        def user = ComponentAccessor.getUserManager().getUserByName('robot')
        commentManager.create(issue,user, comment, null, null, new Date(),properties,false)
    }
}
