import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.label.LabelManager
// the issue key of the issue to update its labels
final String issueKey = issue.getKey()
// change to 'false' if you don't want to send a notification for that change
final boolean sendNotification = true
// have this true in order to throw an issue update event, and reindex the index
final boolean causesChangeNotification = true
// a list with the labels we want to add to the issue
final List<String> newLabels = ["доработка"]
def issueManager = ComponentAccessor.issueManager
def labelManager = ComponentAccessor.getComponent(LabelManager)
def currIssue = issueManager.getIssueByCurrentKey(issueKey)
assert currIssue : "Could not find issue with key $issueKey"
def loggedInUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def existingLabels = labelManager.getLabels(currIssue.id)*.label
def labelsToSet = (existingLabels + newLabels).toSet()
labelManager.setLabels(loggedInUser, currIssue.id, labelsToSet, sendNotification, causesChangeNotification)
