import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.util.json.JSONObject
import com.atlassian.jira.issue.comments.CommentManager

if (issue.getCustomFieldValue(12506) == null) {
    final def originalIssueKey = issue.getKey()
    final def orgField = 'Организация'
    final def contactField = 'Контакт'
    final def reporter = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
    def originalIssue = Issues.getByKey(originalIssueKey) as MutableIssue
    def description =  """
        h2. *Головная SUP задача, по распределению - (${issue.getKey()}).*
        Сотрудник, распределивший головную задачу в ИТ - ${ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser().getDisplayName()};

        Описание ИТ задачи:

        """

    def itsCreateIssueKey = Issues.create('ITS', 'Сервисный запрос') {
        setSummary(originalIssue.summary)        
        setReporter(reporter)        
        setPriority('Medium')  
        setAssignee('')  
        setDescription(description + originalIssue.description)
        setCustomFieldValue(12505, originalIssueKey.toString())
        setCustomFieldValue(orgField, originalIssue.getCustomFieldValue(orgField).toString())
        setCustomFieldValue(contactField, originalIssue.getCustomFieldValue(contactField).toString())
    }

    //set root sup task to creating its task
    if (itsCreateIssueKey != null) {
        issue.set { 
            setCustomFieldValue(12506, itsCreateIssueKey.getKey().toString())
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
} else {
    CommentManager commentManager = ComponentAccessor.getCommentManager()
        def comment = """h2. *Задача ИТ была  ранее создана. Вот значение поля "Корневая задача ИТ - ${issue.getCustomFieldValue(12506).getKey()}."*
        """
        final SD_PUBLIC_COMMENT = "sd.public.comment"
        def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": true] as Map)]
        def user = ComponentAccessor.getUserManager().getUserByName('robot')
        commentManager.create(issue,user, comment, null, null, new Date(),properties,false)
}
