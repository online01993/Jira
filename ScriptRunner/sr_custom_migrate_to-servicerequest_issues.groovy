import com.atlassian.jira.issue.MutableIssue
//def issue = Issues.getByKey('ITS-104825') as MutableIssue
////
////
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.UpdateIssueRequest;
import com.atlassian.jira.issue.IssueInputParametersImpl
import com.atlassian.jira.issue.label.LabelManager
import com.atlassian.jira.workflow.TransitionOptions
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.util.json.JSONObject
import com.atlassian.jira.issue.comments.CommentManager
import com.atlassian.sal.api.component.ComponentLocator
import com.atlassian.jira.issue.fields.CustomField
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
import java.time.LocalDateTime

@WithPlugin("com.riadalabs.jira.plugins.insight")
//@PluginModule IQLFacade iqlFacade

/* Custom field for the waitng field */

CustomField resolutionCF = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11106);
final def searchJql = 'key != ITS-104825 and issuetype = "Техническая консультация" AND resolution is not EMPTY'
def issues = Issues.search(searchJql.toString())
def adminUser = ComponentAccessor.getUserManager().getUserByName('robot')
ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(adminUser)
def projectManager = ComponentAccessor.projectManager
def project = projectManager.getProjectByCurrentKey('ITS')
IssueFactory issueFactory = ComponentAccessor.getComponent(IssueFactory.class)
IssueService issueService = ComponentAccessor.getIssueService()
def issueManager = ComponentAccessor.issueManager
def labelManager = ComponentAccessor.getComponent(LabelManager)
final List<String> newLabels = ["ITS-104325_migrated_issues"]

for (i in issues) {
    ////create subtasks to service request
    def issue = Issues.getByKey(i.key) as MutableIssue
    def issueWorks = issue.getCustomFieldValue('Решение')
    def planningWorkInfosystemsSize
    if (issue.getCustomFieldValue('Инфосистема')) {
        planningWorkInfosystemsSize = issue.getCustomFieldValue('Инфосистема').size()
    } else {
        planningWorkInfosystemsSize = 0
    }
    def description =  """
    h2. *Головная задача (${issue.getKey()}) была обработана.*
    Инженер, запланировавший работу - ${adminUser.getDisplayName()};
    """
    ////
    
    //// create subtasks for IF cyrcle
    if (issueWorks != null && planningWorkInfosystemsSize == 1) {
        ////move issue to service request
        //def loggedInUser = authenticationContext.getLoggedInUser()
        def planningWorkInfosystem = issue.getCustomFieldValue('Инфосистема')[0]
        def createUser = issue.getReporter()
        def assignee = issue.getAssignee()
        def issueType = project.issueTypes.find { it.name == 'Сервисный запрос' }
        def currSubtaskIssueType = project.issueTypes.find { it.name == 'Подзадача' }
        issue.setIssueType(issueType)
        def existingLabels = labelManager.getLabels(issue.id)*.label
        def labelsToSet = (existingLabels + newLabels).toSet()
        labelManager.setLabels(adminUser, issue.id, labelsToSet, false, true)
        ComponentAccessor.issueManager.updateIssue(adminUser, issue, EventDispatchOption.DO_NOT_DISPATCH, false)
        ////

        ////work with issues and works
        for (def j = 0; j < issueWorks.size(); j++) {
            ////modify corrent subtask of issue
            if (!issue.getSubTaskObjects().isEmpty()) {
                def currSubtask
                for (def currSubtaskSeq = 0; currSubtaskSeq < issue.getSubTaskObjects().size(); currSubtaskSeq++) {
                    //
                    currSubtask = issue.getSubTaskObjects()[currSubtaskSeq] as MutableIssue
                    currSubtask.setIssueType(currSubtaskIssueType)
                    ComponentAccessor.issueManager.updateIssue(adminUser, currSubtask, EventDispatchOption.DO_NOT_DISPATCH, false)
                    if (!currSubtask.getCustomFieldValue('Тип выполненной заявки') || !currSubtask.getCustomFieldValue('Решение')) {
                        return 'error, current subtask is empty -- ' + currSubtask
                    }
                    currSubtask.update { 
                        //set CF
                        setEventDispatchOption(EventDispatchOption.DO_NOT_DISPATCH)                        
                        setLabels('SLA_exclude')
                        setCustomFieldValue(12503, currSubtask.getCustomFieldValue('Тип выполненной заявки')[0].getObjectKey().toString()) 
                        setCustomFieldValue(12501, currSubtask.getCustomFieldValue('Решение')[0].getObjectKey().toString())
                    }
                }
            }
            ////
            
            ////create subtask for current work
            def planningWorkType = issue.getCustomFieldValue('Тип выполненной заявки')[0]            
            def currentwork = issueWorks[j]
            def currentSubTaskDescription = """
            h3. +*В рамках текущей задачи требуется выполнить:*+
            Тип выполняемой работы - *[${planningWorkType.getName()}|https://help.it-russia.com/secure/insight/assets/${planningWorkType.getObjectKey()}];*
            Выполняемая работа - *[${currentwork.getName()}|https://help.it-russia.com/secure/insight/assets/${currentwork.getObjectKey()}];*
            С какой инфосистемой связана - *[${planningWorkInfosystem.getName()}|https://help.it-russia.com/secure/insight/assets/${planningWorkInfosystem.getObjectKey()}];*
            Требуется ли выезд по задаче - *${issue.getCustomFieldValue('Требуется выезд')}.*
            """
            //issueType = project.issueTypes.find { it.name == 'Подзадача' }
            issueType = project.issueTypes.find { it.name == 'Поддержка-подзадача' }
            description = description + currentSubTaskDescription
            def originalIssueCreated = issue.getCreated().toTimestamp()
            MutableIssue issueNew = issueFactory.getIssue()
            issueNew.projectObject = project
            issueNew.setParentId(issue.getId())
            def summaryGenerated = currentwork.getName() + ' - ' + planningWorkInfosystem.getName() + ' (' + issue.summary + ')'
            if (summaryGenerated.length() > 254) {
                issueNew.summary = 'work.' + ' (' + issue.summary + ')'
            } else {
                issueNew.summary = summaryGenerated
            }            
            issueNew.setReporter(createUser)
            issueNew.setAssignee(assignee)
            issueNew.created = originalIssueCreated
            issueNew.issueType = issueType            
            issueNew.setComponent(issue.getComponents())
            issueNew.description = description + issue.description
            issueNew.setResolutionId(issue.getResolutionId())
            issueNew.setResolutionDate(issue.getResolutionDate())
            ComponentAccessor.getIssueManager().createIssueObject(adminUser, issueNew)
            log.warn('issue is -- ' + issueNew)
            ComponentAccessor.getSubTaskManager().createSubTaskIssueLink(issue, issueNew, adminUser) 
            ////

            ////transition issue to close status and resolution
            def transitionOptions= new TransitionOptions.Builder()
            .skipConditions()
            .skipPermissions()
            .skipValidators()
            .build()
            int actionId = 171//transition ID ("закрыть автоматически") у поддержки-подзадачи
            //int actionId = 191//transition ID ("закрыть автоматически") у подзадачи
            def issueInputParametersTransition = new IssueInputParametersImpl()
            .setResolutionId(issue.getResolutionId())
            .setResolutionDate(issue.getResolutionDate().toTimestamp().toString())
            .addCustomFieldValue(10402, ComponentAccessor.customFieldManager.getCustomFieldObjectByName('Требуется выезд').getValue(issue).optionId.toString())
            .addCustomFieldValue(10401, ComponentAccessor.customFieldManager.getCustomFieldObjectByName('Платная задача').getValue(issue).optionId.toString())
            .addCustomFieldValue(10400, ComponentAccessor.customFieldManager.getCustomFieldObjectByName('Доп материалы').getValue(issue).optionId.toString())
            def transitionValidationResult = issueService.validateTransition(adminUser, issueNew.id, actionId, issueInputParametersTransition, transitionOptions)
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
            ////

            ////change issue-type to подзадача
            issueNew.setIssueType(project.issueTypes.find { it.name == 'Подзадача' }) 
            ComponentAccessor.issueManager.updateIssue(adminUser, issueNew, EventDispatchOption.DO_NOT_DISPATCH, false)
            ////

            ////update CF for issue
            issueNew.update { 
                //set CF
                setEventDispatchOption(EventDispatchOption.DO_NOT_DISPATCH)
                setResolution(issue.resolution.name)
                setResolutionDate(issue.resolutionDate.toString())
                setLabels('SLA_exclude')
                setCustomFieldValue(10301, issue.getCustomFieldValue('Организация')[0].getObjectKey().toString())
                setCustomFieldValue(10501, issue.getCustomFieldValue('Офис_Подразделение')[0].getObjectKey().toString())
                setCustomFieldValue(10300, issue.getCustomFieldValue('Контакт')[0].getObjectKey().toString())
                setCustomFieldValue(11105, issue.getCustomFieldValue('Инфосистема')[0].getObjectKey().toString())
                setCustomFieldValue(12503, issue.getCustomFieldValue('Тип выполненной заявки')[0].getObjectKey().toString()) 
                setCustomFieldValue(12501, currentwork.getObjectKey().toString())
                setCustomFieldValue(11101, issue.getCustomFieldValue('Тип выполненной заявки')[0].getObjectKey().toString()) 
                setCustomFieldValue(11106, currentwork.getObjectKey().toString())                
                setCustomFieldValue(10402, issue.getCustomFieldValue('Требуется выезд'))
                setCustomFieldValue(10400, issue.getCustomFieldValue('Доп материалы'))
                setCustomFieldValue(10401, issue.getCustomFieldValue('Платная задача'))
            }
            ////

            ////Clear current work subtask targets
            description = '' //clear description for next subtask
            ////
            
            ////end of cyrcle issue subtusk work
            //return  issueNew
        }        
        ////end for cyrcle of creating subtasks
        
        ////generate issue summary for service reqvest
        @WithPlugin("com.riadalabs.jira.plugins.insight")
        @PluginModule IQLFacade iqlFacade

        def organizationDescription = ''
        def officeDescription = ''
        def contactDescription = ''
        def workDescription = ''
        def infosysTypeValue = ''
        def infosysTypeDescription = ''
        def infosysDescription = ''
        def infosysContractValue = ''
        def worklogSumDescription = ''
        def issueAssignee = ''
        def summaryDescription
        def subTasks = issue.getSubTaskObjects()
        if (!subTasks.isEmpty()) {
            summaryDescription =  """
                    h2. *Корневой запрос ИТ решён.*
                    Текущая дата формирования отчёта - ${LocalDateTime.now().format("dd.MM.yyyy - HH:mm:ss")}.

                    h3. Выполненные работы по запросу:
                    ||Тикет||Резолюция||Организация||Офис||Контакт||Выполненная работа||Тип инфосистемы||Инфосистема||Договор инфосистемы||Инженер||Журнал работ, минуты||Платность||Выезд||Доп материалы||
                    """
            //adding subtasks to task summary
            for (subTask in subTasks) {
                organizationDescription = "[" + subTask.getCustomFieldValue('Организация')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + subTask.getCustomFieldValue('Организация')[0].getId() + "]"
                officeDescription = "[" + subTask.getCustomFieldValue('Офис_Подразделение')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + subTask.getCustomFieldValue('Офис_Подразделение')[0].getId() + "]"
                contactDescription = "[" + subTask.getCustomFieldValue('Контакт')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + subTask.getCustomFieldValue('Контакт')[0].getId() + "]"
                if (!subTask.getCustomFieldValue('Решение')) {
                    workDescription = "Нет выполненной работы. Планируемая работа - [" + subTask.getCustomFieldValue('Планируемая работа')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + subTask.getCustomFieldValue('Планируемая работа')[0].getId() + "]"
                } else {
                    workDescription = "[" + subTask.getCustomFieldValue('Решение')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + subTask.getCustomFieldValue('Решение')[0].getId() + "]"
                }
                infosysTypeValue = iqlFacade.findObjects(2,'objectTypeId = 152 and object HAVING inboundReferences(key = ' + subTask.getCustomFieldValue('Инфосистема')[0].getObjectKey() + ')') 
                infosysTypeDescription = "[" + infosysTypeValue[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/CMDB-" + infosysTypeValue[0].getName().toString() + "]"
                infosysDescription = "[" + subTask.getCustomFieldValue('Инфосистема')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/CMDB-" + subTask.getCustomFieldValue('Инфосистема')[0].getId() + "]"
                if (subTask.getCustomFieldValue('Инфосистема')[0].getReference('Договор')) {
                    infosysContractValue = '[Да|https://help.it-russia.com/secure/insight/assets/CRM-' + subTask.getCustomFieldValue('Инфосистема')[0].getReference('Договор').getId() + "]"
                } else {
                    infosysContractValue = 'Нет'
                }
                if (subTask.timeSpent == 0 || subTask.timeSpent == null) {worklogSumDescription = 0} else {worklogSumDescription = subTask.timeSpent.div(60).toString()} 
                if (subTask.assignee) {
                    issueAssignee = subTask.getAssignee().getDisplayName()
                } else {
                    issueAssignee = 'robot'
                }
                summaryDescription = summaryDescription + "|${subTask.getKey()}|${subTask.getResolution().getName()}|${organizationDescription}|${officeDescription}|${contactDescription}|${workDescription}|${infosysTypeDescription}|${infosysDescription}|${infosysContractValue}|${issueAssignee}|${worklogSumDescription}|${subTask.getCustomFieldValue('Платная задача').toString()}|${subTask.getCustomFieldValue('Требуется выезд').toString()}|${subTask.getCustomFieldValue('Доп материалы').toString()}|" + '\n'
            }
            //adding incidents to task summary
            //if (!issue.getCustomFieldValue('Инциденты').isEmpty()) {
            if (issue.getCustomFieldValue('Инциденты')) {
                summaryDescription = summaryDescription + """    
                        h3. Зафиксированные инциденты по запросу:
                        ||Тикет||Организация||Офис||Контакт||Платность||Выезд||Доп материалы||
                """
                for (i=0; i < issue.getCustomFieldValue('Инциденты').size(); i++) {
                    def incident = Issues.getByKey(issue.getCustomFieldValue('Инциденты')[i].getKey()) as MutableIssue
                    organizationDescription = "[" + incident.getCustomFieldValue('Организация')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + incident.getCustomFieldValue('Организация')[0].getId() + "]"
                    officeDescription = "[" + incident.getCustomFieldValue('Офис_Подразделение')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + incident.getCustomFieldValue('Офис_Подразделение')[0].getId() + "]"
                    contactDescription = "[" + incident.getCustomFieldValue('Контакт')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + incident.getCustomFieldValue('Контакт')[0].getId() + "]"
                    summaryDescription = summaryDescription + "|${incident.getKey()}|${organizationDescription}|${officeDescription}|${contactDescription}|${incident.getCustomFieldValue('Платная задача').toString()}|${incident.getCustomFieldValue('Требуется выезд').toString()}|${incident.getCustomFieldValue('Доп материалы').toString()}|" + '\n'
                }
            }
            //adding projects to task summary
            //if (!issue.getCustomFieldValue('Проекты').isEmpty()) {
            if (issue.getCustomFieldValue('Проекты')) {
                summaryDescription = summaryDescription + """    
                        h3. Зафиксированные проекты по запросу:
                        ||Тикет||Организация||Контакт||
                """
                for (i=0; i < issue.getCustomFieldValue('Проекты').size(); i++) {
                    def projects = Issues.getByKey(issue.getCustomFieldValue('Проекты')[i].getKey()) as MutableIssue
                    organizationDescription = "[" + projects.getCustomFieldValue('Организация')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + projects.getCustomFieldValue('Организация')[0].getId() + "]"
                    contactDescription = "[" + projects.getCustomFieldValue('Контакт')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + projects.getCustomFieldValue('Контакт')[0].getId() + "]"
                    summaryDescription = summaryDescription + "|${projects.getKey()}|${organizationDescription}|${contactDescription}|" + '\n'
                }
            }
            issue.update { 
                setCustomFieldValue(12600, summaryDescription)
                setEventDispatchOption(EventDispatchOption.DO_NOT_DISPATCH)  
            }
        } else {
            summaryDescription =  """
                h2. *Корневой запрос ИТ отклонён.*
                Текущая дата формирования отчёта - ${LocalDateTime.now().format("dd.MM.yyyy - HH:mm:ss")}.

                h3. Запланированных работ по запросу нет.
                """
            
            //adding incidents to task summary
            //if (!issue.getCustomFieldValue('Инциденты').isEmpty()) {
            if (issue.getCustomFieldValue('Инциденты')) {
                summaryDescription = summaryDescription + """    
                        h3. Зафиксированные инциденты по запросу:
                        ||Тикет||Организация||Офис||Контакт||Платность||Выезд||Доп материалы||
                """
                for (i=0; i < issue.getCustomFieldValue('Инциденты').size(); i++) {
                    def incident = Issues.getByKey(issue.getCustomFieldValue('Инциденты')[i].getKey()) as MutableIssue
                    organizationDescription = "[" + incident.getCustomFieldValue('Организация')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + incident.getCustomFieldValue('Организация')[0].getId() + "]"
                    officeDescription = "[" + incident.getCustomFieldValue('Офис_Подразделение')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + incident.getCustomFieldValue('Офис_Подразделение')[0].getId() + "]"
                    contactDescription = "[" + incident.getCustomFieldValue('Контакт')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + incident.getCustomFieldValue('Контакт')[0].getId() + "]"
                    summaryDescription = summaryDescription + "|${incident.getKey()}|${organizationDescription}|${officeDescription}|${contactDescription}|${incident.getCustomFieldValue('Платная задача').toString()}|${incident.getCustomFieldValue('Требуется выезд').toString()}|${incident.getCustomFieldValue('Доп материалы').toString()}|" + '\n'
                }
            }
            //adding projects to task summary
            //if (!issue.getCustomFieldValue('Проекты').isEmpty()) {
            if (issue.getCustomFieldValue('Проекты')) {
                summaryDescription = summaryDescription + """    
                        h3. Зафиксированные проекты по запросу:
                        ||Тикет||Организация||Контакт||
                """
                for (i=0; i < issue.getCustomFieldValue('Проекты').size(); i++) {
                    def projects = Issues.getByKey(issue.getCustomFieldValue('Проекты')[i].getKey()) as MutableIssue
                    organizationDescription = "[" + projects.getCustomFieldValue('Организация')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + projects.getCustomFieldValue('Организация')[0].getId() + "]"
                    contactDescription = "[" + projects.getCustomFieldValue('Контакт')[0].getName().toString() + "|https://help.it-russia.com/secure/insight/assets/ID-" + projects.getCustomFieldValue('Контакт')[0].getId() + "]"
                    summaryDescription = summaryDescription + "|${projects.getKey()}|${organizationDescription}|${contactDescription}|" + '\n'
                }
            }
            issue.update { 
                setCustomFieldValue(12600, summaryDescription)
                setEventDispatchOption(EventDispatchOption.DO_NOT_DISPATCH)  
            }
        }
        ////

        ////Clear issue fields
        
        issue.setCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(10501), null)
        issue.setCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11105), null)
        issue.setCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12503), null)
        issue.setCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12501), null)
        issue.setCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11101), null)
        issue.setCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11106), null)
        issue.setComponent(null)
        ComponentAccessor.issueManager.updateIssue(adminUser, issue, EventDispatchOption.DO_NOT_DISPATCH, false)
        
        ////

        ////end for cyrcle of creating subtasks
        //return issue
    }
    ////end of IF cyrcle
}
return true
