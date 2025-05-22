import com.atlassian.jira.issue.IssueInputParametersImpl
import com.atlassian.sal.api.component.ComponentLocator
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.util.json.JSONObject
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.util.json.JSONObject
import com.atlassian.jira.issue.comments.CommentManager
import com.atlassian.jira.issue.MutableIssue

def triggerIdZabbix = issue.getCustomFieldValue('triggerID_Zabbix')
def triggerIdJira = issue.getCustomFieldValue('triggerID_Jira')
def counter
if (triggerIdZabbix != null && triggerIdJira != null) {
    //get issue problem counter
    counter = triggerIdJira[0].getAttributeValues('Счётчик')[0]
    if (!counter || counter == null) {
        //get template counter
        def templateId = triggerIdJira[0].getReferences('Категория триггера')[0]
        if (templateId != null && templateId != '') {
            def whileBreak = false
            while (templateId != null && templateId != '' && !whileBreak) {
                if (templateId.getAttributeValues('Счётчик')) {
                    counter = templateId.getAttributeValues('Счётчик')[0].getIntegerValue()
                    whileBreak = true
                } else {
                    templateId = templateId.getReferences('Категория триггера')[0]             
                }
            }
            if (whileBreak == false) {
                // set default value for counter = 3
                counter = 3
            }
        } else {
            // set default value for counter = 3
            counter = 3
        }        
    }

    // Check zero counter to exit program
    if (counter == 0) {
        //exit program
        return
    }

    // get issue problem with current triggerIdJira
    def searchJql = "project = ITS AND issuetype = Проблема AND triggerID_Jira = ${triggerIdJira[0].getObjectKey()}"
    def issues = Issues.search(searchJql)*.key as List
    int actualCount = issues.size()
    if (actualCount != 0) {
        // problem issue exist - reopen this
        def issueProblem = Issues.getByKey(issues[0]) as MutableIssue
        issueProblem.update { 
            setCustomFieldValue('Инциденты') {
                add(issue)
            }            
        }
        def comment = """Проявление проблемы, новый инцидент -- ${issue}
            Дата нового инцидента -- ${issue.getCreated().toLocalDateTime().format('dd.MM.yyyy - HH:mm:ss')}
            """
        final SD_PUBLIC_COMMENT = "sd.public.comment"
        def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": false] as Map)]
        def user = ComponentAccessor.getUserManager().getUserByName('zabbix-robot')
        CommentManager commentManager = ComponentAccessor.getCommentManager()
        commentManager.create(issueProblem,user, comment, null, null, new Date(),properties,true)
    } else {
        // issue problem not exist, check counter whith incidents.count and create issue problem
        searchJql = "project = ITS AND issuetype = Инцидент AND triggerID_Jira = ${triggerIdJira[0].getObjectKey()}"
        issues = Issues.search(searchJql)*.key
        def issueKeysString = Issues.search(searchJql)
        def actualIssues = issues as List
        actualCount = actualIssues.size() 
        if (actualCount >= (int)counter) {
            // create issue problem
            def adminUser = ComponentAccessor.getUserManager().getUserByName('robot')
            def project = ComponentAccessor.projectManager.getProjectByCurrentKey('ITS')
            def issueType = project.issueTypes.find { it.name == 'Проблема' }
            def issueService = ComponentLocator.getComponent(IssueService)
            def currentDescription = """
            h3. +*В рамках текущей задачи требуется устранить проблему вызывающую инциденты*+
            Список инцидентов по этой проблеме в поле Инциденты.
            """
            
            /*
            // Get inginer line
            def componetsType
            def ingType = triggerIdJira[0].getReferences('Категория инженера')[0]
            if (ingType) {
                if (ingType.getObjectKey() == "ID-606") {
                    componetsType = project.getComponents().find() {it.name == 'Первая линия ТП'}.getId()
                } else {
                    if (ingType.getObjectKey() == "ID-607") {
                        componetsType = project.getComponents().find() {it.name == 'Вторая линия ТП'}.getId()
                    } else {
                        // set second line to default
                        componetsType = project.getComponents().find() {it.name == 'Вторая линия ТП'}.getId()
                    }
                }                
            } else {
                // ingtype is empty set second line to default
                componetsType = project.getComponents().find() {it.name == 'Вторая линия ТП'}.getId()
            }
            */

            // Create issue problem
            def issueInputParameters = new IssueInputParametersImpl()
                .setSummary("Устранение проблемы -- ${issue.summary}.")
                .setDescription(currentDescription)
                //.setComponentIds(componetsType)
                .setPriorityId('3')           
                .setReporterId(adminUser.name)
                .setIssueTypeId(issueType.id)           
                .setProjectId(project.id)
                .addCustomFieldValue(10301, issue.getCustomFieldValue('Организация')[0].getObjectKey().toString()) 
                .addCustomFieldValue(10300, issue.getCustomFieldValue('Контакт')[0].getObjectKey().toString())
                .addCustomFieldValue(10401, ComponentAccessor.customFieldManager.getCustomFieldObjectByName('Платная задача').getValue(issue).optionId.toString())
                .addCustomFieldValue(10400, ComponentAccessor.customFieldManager.getCustomFieldObjectByName('Доп материалы').getValue(issue).optionId.toString())
                .addCustomFieldValue(10402, ComponentAccessor.customFieldManager.getCustomFieldObjectByName('Требуется выезд').getValue(issue).optionId.toString())
                .addCustomFieldValue(11105, issue.getCustomFieldValue('Инфосистема')[0].getObjectKey().toString()) 
                .addCustomFieldValue(12603, issueKeysString*.key as String[]) 
                .addCustomFieldValue(13000, issue.getCustomFieldValue('triggerID_Zabbix').toString())
                .addCustomFieldValue(12901, issue.getCustomFieldValue('triggerID_Jira').toString())
            if (issue.getCustomFieldValue('Офис_Подразделение') == null) {
                issueInputParameters.addCustomFieldValue(10501, issue.getCustomFieldValue('Инфосистема')[0].getReferences('Офис')[0].getObjectKey().toString())
            } else {
                issueInputParameters.addCustomFieldValue(10501, issue.getCustomFieldValue('Офис_Подразделение')[0].getObjectKey().toString())
            }
            def validationResult = issueService.validateCreate(adminUser, issueInputParameters)
            if (validationResult.valid) {
                ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(adminUser)
                def creationResult = issueService.create(adminUser, validationResult)
                if (creationResult.valid) {
                    return creationResult.issue
                } else {
                    CommentManager commentManager = ComponentAccessor.getCommentManager()
                    def comment = """h2. *Задача проблема не была автоматически создана, поэтому необходимо повторно закрыть инцидент либо обратиться к администратору Jira.*
                    Код ошибки -- ${creationResult.getErrorCollection()}
                    """
                    final SD_PUBLIC_COMMENT = "sd.public.comment"
                    def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": true] as Map)]
                    def user = ComponentAccessor.getUserManager().getUserByName('robot')
                    commentManager.create(issue,user, comment, null, null, new Date(),properties,true)
                }
            } else {
                CommentManager commentManager = ComponentAccessor.getCommentManager()
                def comment = """h2. *Задача проблема не была автоматически создана, поэтому необходимо повторно закрыть инцидент либо обратиться к администратору Jira.*
                Код ошибки -- ${validationResult.getErrorCollection()}
                """
                final SD_PUBLIC_COMMENT = "sd.public.comment"
                def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": true] as Map)]
                def user = ComponentAccessor.getUserManager().getUserByName('robot')
                commentManager.create(issue,user, comment, null, null, new Date(),properties,true)
            }
        }
    }
}
