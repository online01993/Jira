import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.workflow.TransitionOptions

def adminUser = ComponentAccessor.getUserManager().getUserByName('robot')
def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
def parrentIssue = issue.parentObject

if (parrentIssue.status.name == 'Открыть' || parrentIssue.status.name == 'Запрос обработан') {
    def waitingVal = issue.getCustomFieldValue('Ожидание')
    def blockingVal = issue.getCustomFieldValue('Блокируется')
    IssueManager issueManager = ComponentAccessor.issueManager
    CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()
    IssueService issueService = ComponentAccessor.getIssueService()
    def issueInputParameters = issueService.newIssueInputParameters()
    if (waitingVal != null) {
        issueInputParameters.addCustomFieldValue(12400, waitingVal[0].getObjectKey().toString())
        }
    if (blockingVal != null) {
        issueInputParameters.addCustomFieldValue(12401, blockingVal[0].getKey())
    }
    def transitionOptions= new TransitionOptions.Builder()
    .skipConditions()
    .skipPermissions()
    .skipValidators()
    .build()
    int actionId = 111//transition ID ("ожидание")
    def transitionValidationResult = issueService.validateTransition(adminUser, parrentIssue.id, actionId, issueInputParameters,transitionOptions)
    if (transitionValidationResult.isValid()) {
        def transitionResult = issueService.transition(adminUser, transitionValidationResult)
        if (transitionResult.isValid()){
            //log.debug("Transitioned issue $issue through action $actionId")
        }
    else {
        log.debug("Transition result is not valid") }
    }
    else {
        log.debug("The transitionValidation is not valid")
    }
}
