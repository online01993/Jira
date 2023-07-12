import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.user.ApplicationUser;

Issue issueKey = issue;
def user = ((ApplicationUser) issueKey.getAssignee()).getDisplayName();

// Get access to the Jira comment and component manager
CommentManager commentManager = ComponentAccessor.getCommentManager();

// Get the last comment entered in on the issue to a String
def comment = "Переоткрытие задачи, последний исполнитель - " + user;


// Check if the issue is not null
if(issueKey){
        // Create a comment on the issue
        commentManager.create(issueKey, "robot",comment, true);
}
