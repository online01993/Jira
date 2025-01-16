//https://library.adaptavist.com/entity/create-a-back-dated-issue
//import com.atlassian.jira.issue.MutableIssue
//def issue = Issues.getByKey('ITS-104221') as MutableIssue
////
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue

import java.sql.Timestamp
import java.time.LocalDateTime

def PROJECT_KEY = "SUP"
def YEAR = 2012
def MONTH = 12
def DAY = 21
def HOUR = 18 //This is server time (TimeZone)
def MINUTE = 6
def SECOND = 6

def SUMMARY = "The issue summary"
def DESCRIPTION = "The issue description"

def project = ComponentAccessor.projectManager.getProjectObjByKey(PROJECT_KEY)
def loggedInUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser

Timestamp timestamp = Timestamp.valueOf(LocalDateTime.of(YEAR, MONTH, DAY, HOUR, MINUTE, SECOND))

if (project && timestamp) {
    MutableIssue issue = ComponentAccessor.issueFactory.issue
    issue.projectObject = project
    issue.summary = SUMMARY
    issue.created = timestamp
    issue.issueType = project.issueTypes.first()
    issue.description = DESCRIPTION
    ComponentAccessor.issueManager.createIssueObject(loggedInUser, issue)
}
