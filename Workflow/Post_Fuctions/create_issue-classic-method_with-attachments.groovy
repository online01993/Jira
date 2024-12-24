import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.IssueInputParametersImpl
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.util.json.JSONObject
import com.atlassian.jira.issue.comments.CommentManager
import com.atlassian.sal.api.component.ComponentLocator
import com.atlassian.jira.config.util.JiraHome
import com.atlassian.jira.service.services.file.FileService
import com.atlassian.jira.issue.attachment.CreateAttachmentParamsBean

def jiraHome = ComponentAccessor.getComponent(JiraHome)
def userAttacher = ComponentAccessor.userManager.getUserByName('robot')

if (!issue.getCustomFieldValue('Корневая задача ИТ')) {
    def issueService = ComponentLocator.getComponent(IssueService)
    def projectManager = ComponentAccessor.projectManager
    def authenticationContext = ComponentAccessor.jiraAuthenticationContext
    def project = projectManager.getProjectByCurrentKey('ITS')
    def loggedInUser = authenticationContext.getLoggedInUser()
    def adminUser = ComponentAccessor.getUserManager().getUserByName('robot')
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
        .addCustomFieldValue(10301, issue.getCustomFieldValue('Организация')[0].getObjectKey().toString()) 
        .addCustomFieldValue(10300, issue.getCustomFieldValue('Контакт')[0].getObjectKey().toString())
    //def validationResult = issueService.validateCreate(loggedInUser, issueInputParameters)
    def validationResult = issueService.validateCreate(adminUser, issueInputParameters)
    if (validationResult.valid) {
        //def creationResult = issueService.create(loggedInUser, validationResult)
        def creationResult = issueService.create(adminUser, validationResult)
        if (creationResult.valid) {
            //log.debug(creationResult.issue)
            issue.set { 
                setCustomFieldValue(12506, creationResult.issue)
            }
            def itsIssue = creationResult.issue as MutableIssue
            def attachment = issue.getAttachments()
            for (def i = 0; i < attachment.size(); i++) {
                def destinationAttach = new File(jiraHome.home, FileService.MAIL_DIR).absoluteFile
				def fileAttach = new File("${destinationAttach}/" + attachment[i].getFilename())
				fileAttach.createNewFile()
				def outAttach = new FileOutputStream(fileAttach)
				def attachContent = attachment[i].getDownloadUrl()
				outAttach.write(attachContent.getBytes())                		
				def attachmentParamsAttach = new CreateAttachmentParamsBean.Builder("${destinationAttach}/" + attachment[i].getFilename() as File, attachment[i].getFilename(), '', userAttacher, itsIssue).build()
				ComponentAccessor.attachmentManager.createAttachment(attachmentParamsAttach)
				fileAttach.delete()
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
} else {
    CommentManager commentManager = ComponentAccessor.getCommentManager()
    def comment = """h2. *Задача ИТ уже создана и указана в поле "Корневая задача ИТ".*
    Никаких действий по автоматическому созданию задачи не требуется. 
    """
    final SD_PUBLIC_COMMENT = "sd.public.comment"
    def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": true] as Map)]
    def user = ComponentAccessor.getUserManager().getUserByName('robot')
    commentManager.create(issue,user, comment, null, null, new Date(),properties,false)
}
