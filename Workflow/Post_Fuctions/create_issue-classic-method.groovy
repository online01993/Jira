import com.atlassian.jira.issue.IssueInputParametersImpl
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.util.json.JSONObject
import com.atlassian.jira.issue.comments.CommentManager
import com.atlassian.sal.api.component.ComponentLocator
//import com.atlassian.jira.issue.ModifiedValue //get options if necessary outputs
import com.atlassian.jira.issue.customfields.option.Option
import com.atlassian.jira.event.type.EventDispatchOption


def planningWorkType = issue.getCustomFieldValue('Тип выполняемой работы')[0]
def planningWork = issue.getCustomFieldValue('Планируемая работа')
def planningWorkInfosystem = issue.getCustomFieldValue('Инфосистема')[0]
def issueService = ComponentLocator.getComponent(IssueService)
def project = ComponentAccessor.projectManager.getProjectByCurrentKey('ITS')
def loggedInUser = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser()
def adminUser = ComponentAccessor.getUserManager().getUserByName('robot')
def issueType
def inc
def planningWorkTypeIncident
def planningWorkTypeProject
def planningWorkTypeSutask
def description =  """
    h2. *Головная задача (${issue.getKey()}) была обработана.*
    Инженер, запланировавший работу - ${loggedInUser.getDisplayName()};
    """

//refresh parent issue data 
Issues.getByKey(issue.getKey()).reindex() //reindex parentissue to get fresh values from screen "work planning"
Issues.getByKey(issue.getKey()).refresh() //refresh parentissue to get fresh values from screen "work planning"
ComponentAccessor.getIssueManager().updateIssue(adminUser, issue, EventDispatchOption.DO_NOT_DISPATCH, false) //update root Issue if necessary 
//

//set type of creating work issue`s
if (planningWorkType.getObjectKey() == 'ID-3942') { 
    planningWorkTypeIncident = true
 } else if (planningWorkType.getObjectKey() == 'ID-121309') {
    planningWorkTypeProject = true
 } else {
    planningWorkTypeSutask = true
 }

////Creating tasks
//Subtasks
if (planningWorkTypeSutask == true) {    
    issueType = project.issueTypes.find { it.name == 'Подзадача' }
    def creationResult
    def currentSubTaskDescription
    for (inc = 0; inc < planningWork.size(); inc++) {
        currentSubTaskDescription = """
        h3. +*В рамках текущей задачи требуется выполнить:*+
        Тип выполняемой работы - *[${planningWorkType.getName()}|https://help.it-russia.com/secure/insight/assets/${planningWorkType.getObjectKey()}];*
        Выполняемая работа - *[${planningWork[inc].getName()}|https://help.it-russia.com/secure/insight/assets/${planningWork[inc].getObjectKey()}];*
        С какой инфосистемой связана - *[${planningWorkInfosystem.getName()}|https://help.it-russia.com/secure/insight/assets/${planningWorkInfosystem.getObjectKey()}];*
        Требуется ли выезд по задаче - *${issue.getCustomFieldValue('Требуется выезд')}.*
        """
        def issueInputParameters = new IssueInputParametersImpl()
           .setSummary(issue.summary + '. Работа - ' + planningWork[inc].getName() + '. Инфосистема - ' + planningWorkInfosystem + '.')
           .setDescription(description + currentSubTaskDescription)
           //.setPriorityId('3')           
           .setReporterId(loggedInUser.name)
           .setIssueTypeId(issueType.id)           
           .setProjectId(project.id)
           .addCustomFieldValue(10301, issue.getCustomFieldValue('Организация')[0].getObjectKey().toString()) 
           .addCustomFieldValue(10501, issue.getCustomFieldValue('Офис_Подразделение')[0].getObjectKey().toString()) 
           .addCustomFieldValue(10300, issue.getCustomFieldValue('Контакт')[0].getObjectKey().toString())
           .addCustomFieldValue(10402, ComponentAccessor.customFieldManager.getCustomFieldObjectByName('Требуется выезд').getValue(issue).optionId.toString())
           //log.warn("ModifiedValue -- " + new ModifiedValue(issue.getCustomFieldValue('Требуется выезд').getValue(), options.get(0))) //get options if necessary outputs
           .addCustomFieldValue(11105, issue.getCustomFieldValue('Инфосистема')[0].getObjectKey().toString()) 
           .addCustomFieldValue(12503, issue.getCustomFieldValue('Тип выполняемой работы')[0].getObjectKey().toString()) 
           .addCustomFieldValue(12501, issue.getCustomFieldValue('Планируемая работа')[inc].getObjectKey().toString()) 
        def validationResult = issueService.validateSubTaskCreate(adminUser, issue.id, issueInputParameters)
        if (validationResult.valid) {
            ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(adminUser)
            creationResult = issueService.create(adminUser, validationResult)
            if (creationResult.valid) {
                ComponentAccessor.subTaskManager.createSubTaskIssueLink(issue, creationResult.issue, adminUser) //link subtask to parent task
                ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(loggedInUser)
            } else {
                CommentManager commentManager = ComponentAccessor.getCommentManager()
                def comment = """h2. *Подзадача не была автоматически создана, поэтому необходимо повторно провести процедуру распределения либо обратиться к администратору Jira.*
                Код ошибки -- ${creationResult.getErrorCollection()}
                """
                final SD_PUBLIC_COMMENT = "sd.public.comment"
                def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": true] as Map)]
                def user = ComponentAccessor.getUserManager().getUserByName('robot')
                commentManager.create(issue,user, comment, null, null, new Date(),properties,false)
            }
        }
    }
}
//Incidents
if (planningWorkTypeIncident == true) {
    issueType = project.issueTypes.find { it.name == 'Инцидент' }
    def componetsType = project.getComponents().find() {it.name == 'Поддержка ПК'}.getId()
    def creationResult
    def currentDescription
    for (inc = 0; inc < planningWork.size(); inc++) {
        currentDescription = """
        h3. +*В рамках текущей задачи требуется устранить инцидент:*+
        Тип выполняемой работы - *[${planningWorkType.getName()}|https://help.it-russia.com/secure/insight/assets/${planningWorkType.getObjectKey()}];*
        Выполняемая работа - *[${planningWork[inc].getName()}|https://help.it-russia.com/secure/insight/assets/${planningWork[inc].getObjectKey()}];*
        С какой инфосистемой связана - *[${planningWorkInfosystem.getName()}|https://help.it-russia.com/secure/insight/assets/${planningWorkInfosystem.getObjectKey()}];*
        Требуется ли выезд по задаче - *${issue.getCustomFieldValue('Требуется выезд')}.*
        """
        def issueInputParameters = new IssueInputParametersImpl()
           .setSummary(issue.summary + '. Работа - ' + planningWork[inc].getName() + '. Инфосистема - ' + planningWorkInfosystem + '.')
           .setDescription(description + currentDescription)
           .setComponentIds(componetsType)
           .setPriorityId('3')           
           .setReporterId(loggedInUser.name)
           .setIssueTypeId(issueType.id)           
           .setProjectId(project.id)
           .addCustomFieldValue(10301, issue.getCustomFieldValue('Организация')[0].getObjectKey().toString()) 
           .addCustomFieldValue(10501, issue.getCustomFieldValue('Офис_Подразделение')[0].getObjectKey().toString()) 
           .addCustomFieldValue(10300, issue.getCustomFieldValue('Контакт')[0].getObjectKey().toString())
           .addCustomFieldValue(10401, ComponentAccessor.customFieldManager.getCustomFieldObjectByName('Платная задача').getValue(issue).optionId.toString())
           .addCustomFieldValue(10400, ComponentAccessor.customFieldManager.getCustomFieldObjectByName('Доп материалы').getValue(issue).optionId.toString())
           .addCustomFieldValue(10402, ComponentAccessor.customFieldManager.getCustomFieldObjectByName('Требуется выезд').getValue(issue).optionId.toString())
           //log.warn("ModifiedValue -- " + new ModifiedValue(issue.getCustomFieldValue('Требуется выезд').getValue(), options.get(0))) //get options if necessary outputs
           .addCustomFieldValue(11105, issue.getCustomFieldValue('Инфосистема')[0].getObjectKey().toString()) 
           .addCustomFieldValue(12503, issue.getCustomFieldValue('Тип выполняемой работы')[0].getObjectKey().toString()) 
           .addCustomFieldValue(12501, issue.getCustomFieldValue('Планируемая работа')[inc].getObjectKey().toString()) 
           .addCustomFieldValue(12606, issue.getKey().toString()) 
        def validationResult = issueService.validateCreate(adminUser, issueInputParameters)
        if (validationResult.valid) {
            ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(adminUser)
            creationResult = issueService.create(adminUser, validationResult)
            if (creationResult.valid) {
                //NEED TO BO SET not UPDATE
                issue.set { 
                    setCustomFieldValue('Инциденты') {
                        add(creationResult.issue)
                    }
                }                
                //refresh parent issue data 
                Issues.getByKey(issue.getKey()).reindex() //reindex parentissue to get fresh values from screen "work planning"
                Issues.getByKey(issue.getKey()).refresh() //refresh parentissue to get fresh values from screen "work planning"
                ComponentAccessor.getIssueManager().updateIssue(adminUser, issue, EventDispatchOption.DO_NOT_DISPATCH, false) //update root Issue if necessary 
                //
                ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(loggedInUser)
            } else {
                CommentManager commentManager = ComponentAccessor.getCommentManager()
                def comment = """h2. *Инцидент(-ы) не был(-и) автоматически создан(-ы), поэтому необходимо повторно провести процедуру распределения либо обратиться к администратору Jira.*
                Код ошибки -- ${creationResult.getErrorCollection()}
                """
                final SD_PUBLIC_COMMENT = "sd.public.comment"
                def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": true] as Map)]
                def user = ComponentAccessor.getUserManager().getUserByName('robot')
                commentManager.create(issue,user, comment, null, null, new Date(),properties,false)
            }
        }
    }
}
//Projects
if (planningWorkTypeProject == true) {
    issueType = project.issueTypes.find { it.name == 'Запрос на обслуживание с заверениями' }
    def creationResult
    def currentDescription
    for (inc = 0; inc < planningWork.size(); inc++) {
        currentDescription = """
        h3. +*В рамках текущей задачи требуется выполнить проектную работу:*+
        Тип выполняемой работы - *[${planningWorkType.getName()}|https://help.it-russia.com/secure/insight/assets/${planningWorkType.getObjectKey()}];*
        Выполняемая работа - *[${planningWork[inc].getName()}|https://help.it-russia.com/secure/insight/assets/${planningWork[inc].getObjectKey()}];*
        С какой инфосистемой связана - *[${planningWorkInfosystem.getName()}|https://help.it-russia.com/secure/insight/assets/${planningWorkInfosystem.getObjectKey()}];*
        Требуется ли выезд по задаче - *${issue.getCustomFieldValue('Требуется выезд')}.*
        """
        def issueInputParameters = new IssueInputParametersImpl()
           .setSummary(issue.summary + '. Работа - ' + planningWork[inc].getName() + '. Инфосистема - ' + planningWorkInfosystem + '.')
           .setDescription(description + currentDescription)
           //.setPriorityId('3')           
           .setReporterId(loggedInUser.name)
           .setIssueTypeId(issueType.id)           
           .setProjectId(project.id)
           .addCustomFieldValue(10301, issue.getCustomFieldValue('Организация')[0].getObjectKey().toString()) 
           .addCustomFieldValue(10300, issue.getCustomFieldValue('Контакт')[0].getObjectKey().toString())
           .addCustomFieldValue(11105, issue.getCustomFieldValue('Инфосистема')[0].getObjectKey().toString()) 
           .addCustomFieldValue(12503, issue.getCustomFieldValue('Тип выполняемой работы')[0].getObjectKey().toString()) 
           .addCustomFieldValue(12501, issue.getCustomFieldValue('Планируемая работа')[inc].getObjectKey().toString()) 
           .addCustomFieldValue(12606, issue.getKey().toString()) 
        def validationResult = issueService.validateCreate(adminUser, issueInputParameters)
        if (validationResult.valid) {
            ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(adminUser)
            creationResult = issueService.create(adminUser, validationResult)
            if (creationResult.valid) {
                //NEED TO BO SET not UPDATE
                issue.set { 
                    setCustomFieldValue('Проекты') {
                        add(creationResult.issue)
                    }
                }
                //refresh parent issue data 
                Issues.getByKey(issue.getKey()).reindex() //reindex parentissue to get fresh values from screen "work planning"
                Issues.getByKey(issue.getKey()).refresh() //refresh parentissue to get fresh values from screen "work planning"
                ComponentAccessor.getIssueManager().updateIssue(adminUser, issue, EventDispatchOption.DO_NOT_DISPATCH, false) //update root Issue if necessary 
                //
                ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(loggedInUser)
            } else {
                CommentManager commentManager = ComponentAccessor.getCommentManager()
                def comment = """h2. *Проект(-ы) не был(-и) автоматически создан(-ы), поэтому необходимо повторно провести процедуру распределения либо обратиться к администратору Jira.*
                Код ошибки -- ${creationResult.getErrorCollection()}
                """
                final SD_PUBLIC_COMMENT = "sd.public.comment"
                def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": true] as Map)]
                def user = ComponentAccessor.getUserManager().getUserByName('robot')
                commentManager.create(issue,user, comment, null, null, new Date(),properties,false)
            }
        }
    }
}
