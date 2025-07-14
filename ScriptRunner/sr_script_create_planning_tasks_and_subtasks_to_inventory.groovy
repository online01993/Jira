import com.atlassian.jira.issue.IssueInputParametersImpl
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.issue.context.IssueContextImpl
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.Issue
import com.opensymphony.workflow.InvalidInputException
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean
import java.time.temporal.ChronoUnit
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
@WithPlugin("com.riadalabs.jira.plugins.insight")
@PluginModule IQLFacade iqlFacade

Issue createIssueObject (Issue parentIssue, String issueTypeIn,String summary, String description, String organization, String componetsTypeIn) {
    def projectManager = ComponentAccessor.projectManager
    def authenticationContext = ComponentAccessor.jiraAuthenticationContext
    def project = projectManager.getProjectByCurrentKey('ITS')
    def adminUser = ComponentAccessor.getUserManager().getUserByName('robot')
    def issueService = ComponentAccessor.getComponent(IssueService)    
    def issueType = project.issueTypes.find { it.name == issueTypeIn }
    def issueContext = new IssueContextImpl(project, issueType)    
    def optionPayNo = ComponentAccessor.optionsManager.getOptions(ComponentAccessor.customFieldManager.getCustomFieldObjectByName("Платная задача").getRelevantConfig(issueContext)).find { it.value == "Нет" }.optionId
    def optionExitworkNo = ComponentAccessor.optionsManager.getOptions(ComponentAccessor.customFieldManager.getCustomFieldObjectByName("Требуется выезд").getRelevantConfig(issueContext)).find { it.value == "Нет" }.optionId
    def optionMaterialsNo = ComponentAccessor.optionsManager.getOptions(ComponentAccessor.customFieldManager.getCustomFieldObjectByName("Доп материалы").getRelevantConfig(issueContext)).find { it.value == "Нет" }.optionId
    switch (issueTypeIn) {
        case "Плановая задача": {
            def issueInputParameters = new IssueInputParametersImpl()
                .setSummary(summary)
                .setDescription(description)
                .setComponentIds(project.getComponents().find() {it.name == componetsTypeIn}.getId())
                .setReporterId(adminUser.name)
                .setIssueTypeId(issueType.id)
                .setProjectId(project.id)
                .setPriorityId('3')  
                .addCustomFieldValue(10301, organization) 
                .addCustomFieldValue(10501, "ID-4152") //id of office contact
                .addCustomFieldValue(10300, "ID-4574") //id of robot contact
                .addCustomFieldValue(10401, optionPayNo as String)
                .addCustomFieldValue(10400, optionMaterialsNo as String)
                .addCustomFieldValue(10402, optionExitworkNo as String)
            def validationResult = issueService.validateCreate(adminUser, issueInputParameters)
            if (validationResult.valid) {
                def creationResult = issueService.create(adminUser, validationResult)
                if (creationResult.valid) {                    
                    return creationResult.issue
                } else {
                    throw new InvalidInputException ("Issue create failed for ${organization} - " + creationResult.getErrorCollection())
                }
            } else {
                throw new InvalidInputException ("Issue create validation failed for ${organization} - " + validationResult.getErrorCollection())
            }
        }
        case "Подзадача": {
            if (parentIssue != null) {
                def issueInputParameters = new IssueInputParametersImpl()
                    .setSummary(summary)
                    .setDescription(description)
                    .setComponentIds(project.getComponents().find() {it.name == componetsTypeIn}.getId())
                    .setReporterId(adminUser.name)
                    .setIssueTypeId(issueType.id)
                    .setProjectId(project.id)
                    .setPriorityId('3')  
                    .addCustomFieldValue(10301, organization) 
                    .addCustomFieldValue(10501, "ID-4152") //id of office contact
                    .addCustomFieldValue(10300, "ID-4574") //id of robot contact
                    .addCustomFieldValue(10401, optionPayNo as String)
                    .addCustomFieldValue(10400, optionMaterialsNo as String)
                    .addCustomFieldValue(10402, optionExitworkNo as String)
                    .addCustomFieldValue(11105, "CMDB-4186") 
                    .addCustomFieldValue(12503, "ID-4125") 
                    .addCustomFieldValue(12501, "ID-117070")
                    .addCustomFieldValue(11101, "ID-4125") 
                    .addCustomFieldValue(11106, "ID-117070")  
                def validationResult = issueService.validateSubTaskCreate(adminUser, parentIssue.id, issueInputParameters)
                if (validationResult.valid) {
                    def creationResult = issueService.create(adminUser, validationResult)
                    if (creationResult.valid) {                    
                        ComponentAccessor.subTaskManager.createSubTaskIssueLink(parentIssue, creationResult.issue, adminUser) //link subtask to parent task
                        return creationResult.issue
                    } else {
                        throw new InvalidInputException ("Issue subtask create failed for ${organization} (paretIssue ${parentIssue}) - " + creationResult.getErrorCollection())
                    }
                } else {
                    throw new InvalidInputException ("Issue subtask create validation failed for ${organization} (paretIssue ${parentIssue}) - " + validationResult.getErrorCollection())
                }
            } else {
                throw new InvalidInputException ("Issue subtask create validation failed for ${organization}, paretIssue is null")
            }            
        }
    }
}

Issue createIssues (ObjectBean objectOrg, Map<String, Boolean> boolPassportflags) {
    def today = new java.sql.Timestamp(new Date().getTime()).toLocalDateTime().format('yyyy-MM')
    def orgPreviousTask = "_нет такой задачи_"
    if (!objectOrg.getAttributeValues('Актуализация ИТ').empty) {
        if (ComponentAccessor.getIssueManager().isExistingIssueKey(objectOrg.getAttributeValues('Актуализация ИТ')[0].getValue().toString())) {
            orgPreviousTask = objectOrg.getAttributeValues('Актуализация ИТ')[0].getValue()
        }
    }
    String summary = "Плановая актуализация паспорта объекта ${objectOrg.getName()}. ${today}."
    String description =  """
        h2. *Плановая задача актуализации паспортов клиентов*                    
        
        Данная задача создаётся с регулярностью раз в пол-года с момента последней актуализации.
        Организация -- [${objectOrg.getName()}|https://help.it-russia.com/secure/insight/assets/${objectOrg.getObjectKey()}]
        Предыдущая задача актуализации, если была -- ${orgPreviousTask}
        Регамент описывающий данное поведение актуализации паспортов разрабатывался в задаче ITS-41921.
        
        В рамках данной задачи на актуализацию, в соответствии с карчтокой клиента и услуг на обслуживание, нужно выполнить актуализацию следующих разделов базы знаний:

        """
    String descriptionSubtasks = ""
    boolPassportflags.each { flagname, isActive ->
        if (isActive) {
            switch (flagname) {
                case "boolPassportInet" : {
                    descriptionSubtasks = descriptionSubtasks + "-- Раздел Интернет\n"
                    break
                }
                case "boolPassportLogicInet" : {
                    descriptionSubtasks = descriptionSubtasks + "-- Раздел Логическая карта сетей\n"
                    break
                }
                case "boolPassportPhisicsInet" : {
                    descriptionSubtasks = descriptionSubtasks + "-- Раздел Физическая карта сетей\n"
                    break
                }
                case "boolPassportWifiInet" : {
                    descriptionSubtasks = descriptionSubtasks + "-- Раздел Wi-Fi сети\n"
                    break
                }
                case "boolPassportPhones" : {
                    descriptionSubtasks = descriptionSubtasks + "-- Раздел Телефония\n"
                    break
                }
                case "boolPassport1c" : {
                    descriptionSubtasks = descriptionSubtasks + "-- Раздел 1С\n"
                    break
                }
                case "boolPassportAD" : {
                    descriptionSubtasks = descriptionSubtasks + "-- Раздел Active Directory\n"
                    break
                }
                case "boolPassportFiles" : {
                    descriptionSubtasks = descriptionSubtasks + "-- Раздел Файловая служба\n"
                    break
                }
                case "boolPassportPrinting" : {
                    descriptionSubtasks = descriptionSubtasks + "-- Раздел Сервис печати и сканирования\n"
                    break
                }
                case "boolPassportRemotes" : {
                    descriptionSubtasks = descriptionSubtasks + "-- Раздел Удаленный доступ\n"
                    break
                }
                case "boolPassportServerRooms" : {
                    descriptionSubtasks = descriptionSubtasks + "-- Раздел Серверные\n"
                    break
                }
                case "boolPassportServers" : {
                    descriptionSubtasks = descriptionSubtasks + "-- Раздел Схема работы серверов\n"
                    break
                }
                case "boolPassportServices" : {
                    descriptionSubtasks = descriptionSubtasks + "-- Раздел Сервисы\n"
                    break
                }
                case "boolPassportLicenses" : {
                    descriptionSubtasks = descriptionSubtasks + "-- Раздел Лицензии\n"
                    break
                }
                case "boolPassportBackups" : {
                    descriptionSubtasks = descriptionSubtasks + "-- Раздел Резервное копирование\n"
                    break
                }
                case "boolPassportPasswords" : {
                    descriptionSubtasks = descriptionSubtasks + "-- Раздел Пароли\n"
                    break
                }
            }
        }
    }
    // create main task
    Issue newMainIssue = createIssueObject(null, "Плановая задача", summary, description + descriptionSubtasks, objectOrg.getObjectKey().toString(), "Первая линия ТП")
    if (newMainIssue) {
        Assets.getByKey(objectOrg.getObjectKey()).update { 
            setAttribute('Актуализация ИТ', newMainIssue)
        }
        // create subtasks to main task newMainIssue
        description =  """
            h2. *Плановая задача актуализации паспортов клиентов*                    
            
            Данная задача создаётся с регулярностью раз в пол-года с момента последней актуализации.
            Организация -- [${objectOrg.getName()}|https://help.it-russia.com/secure/insight/assets/${objectOrg.getObjectKey()}]
            Предыдущая задача актуализации, если была -- ${orgPreviousTask}
            Регамент описывающий данное поведение актуализации паспортов разрабатывался в задаче ITS-41921.
            
            В рамках текущей подзадачи обрабатывается следующий раздел паспорта объекта:

            """
        descriptionSubtasks = ""
        Issue newMainIssueSubtask
        boolPassportflags.each { flagname, isActive ->
            if (isActive) {
                switch (flagname) {
                    case "boolPassportInet" : {
                        descriptionSubtasks = descriptionSubtasks + "-- Раздел Интернет\n"
                        newMainIssueSubtask = createIssueObject(newMainIssue, "Подзадача", summary + " Раздел Интернет.", description + descriptionSubtasks, objectOrg.getObjectKey().toString(), "Первая линия ТП")
                        descriptionSubtasks = ""
                        break
                    }
                    case "boolPassportLogicInet" : {
                        descriptionSubtasks = descriptionSubtasks + "-- Раздел Логическая карта сетей\n"
                        newMainIssueSubtask = createIssueObject(newMainIssue, "Подзадача", summary + " Раздел Логическая карта сетей.", description + descriptionSubtasks, objectOrg.getObjectKey().toString(), "Первая линия ТП")
                        descriptionSubtasks = ""
                        break
                    }
                    case "boolPassportPhisicsInet" : {
                        descriptionSubtasks = descriptionSubtasks + "-- Раздел Физическая карта сетей\n"
                        newMainIssueSubtask = createIssueObject(newMainIssue, "Подзадача", summary + " Раздел Физическая карта сетей.", description + descriptionSubtasks, objectOrg.getObjectKey().toString(), "Первая линия ТП")
                        descriptionSubtasks = ""
                        break
                    }
                    case "boolPassportWifiInet" : {
                        descriptionSubtasks = descriptionSubtasks + "-- Раздел Wi-Fi сети\n"
                        newMainIssueSubtask = createIssueObject(newMainIssue, "Подзадача", summary + " Раздел Wi-Fi сети.", description + descriptionSubtasks, objectOrg.getObjectKey().toString(), "Первая линия ТП")
                        descriptionSubtasks = ""
                        break
                    }
                    case "boolPassportPhones" : {
                        descriptionSubtasks = descriptionSubtasks + "-- Раздел Телефония\n"
                        newMainIssueSubtask = createIssueObject(newMainIssue, "Подзадача", summary + " Раздел Телефония.", description + descriptionSubtasks, objectOrg.getObjectKey().toString(), "Первая линия ТП")
                        descriptionSubtasks = ""
                        break
                    }
                    case "boolPassport1c" : {
                        descriptionSubtasks = descriptionSubtasks + "-- Раздел 1С\n"
                        newMainIssueSubtask = createIssueObject(newMainIssue, "Подзадача", summary + " Раздел 1С.", description + descriptionSubtasks, objectOrg.getObjectKey().toString(), "Первая линия ТП")
                        descriptionSubtasks = ""
                        break
                    }
                    case "boolPassportAD" : {
                        descriptionSubtasks = descriptionSubtasks + "-- Раздел Active Directory\n"
                        newMainIssueSubtask = createIssueObject(newMainIssue, "Подзадача", summary + " Раздел Active Directory.", description + descriptionSubtasks, objectOrg.getObjectKey().toString(), "Первая линия ТП")
                        descriptionSubtasks = ""
                        break
                    }
                    case "boolPassportFiles" : {
                        descriptionSubtasks = descriptionSubtasks + "-- Раздел Файловая служба\n"
                        newMainIssueSubtask = createIssueObject(newMainIssue, "Подзадача", summary + " Раздел Файловая служба.", description + descriptionSubtasks, objectOrg.getObjectKey().toString(), "Первая линия ТП")
                        descriptionSubtasks = ""
                        break
                    }
                    case "boolPassportPrinting" : {
                        descriptionSubtasks = descriptionSubtasks + "-- Раздел Сервис печати и сканирования\n"
                        newMainIssueSubtask = createIssueObject(newMainIssue, "Подзадача", summary + " Раздел Сервис печати и сканирования.", description + descriptionSubtasks, objectOrg.getObjectKey().toString(), "Первая линия ТП")
                        descriptionSubtasks = ""
                        break
                    }
                    case "boolPassportRemotes" : {
                        descriptionSubtasks = descriptionSubtasks + "-- Раздел Удаленный доступ\n"
                        newMainIssueSubtask = createIssueObject(newMainIssue, "Подзадача", summary + " Раздел Удаленный доступ.", description + descriptionSubtasks, objectOrg.getObjectKey().toString(), "Первая линия ТП")
                        descriptionSubtasks = ""
                        break
                    }
                    case "boolPassportServerRooms" : {
                        descriptionSubtasks = descriptionSubtasks + "-- Раздел Серверные\n"
                        newMainIssueSubtask = createIssueObject(newMainIssue, "Подзадача", summary + " Раздел Серверные.", description + descriptionSubtasks, objectOrg.getObjectKey().toString(), "Первая линия ТП")
                        descriptionSubtasks = ""
                        break
                    }
                    case "boolPassportServers" : {
                        descriptionSubtasks = descriptionSubtasks + "-- Раздел Схема работы серверов\n"
                        newMainIssueSubtask = createIssueObject(newMainIssue, "Подзадача", summary + " Раздел Схема работы серверов.", description + descriptionSubtasks, objectOrg.getObjectKey().toString(), "Первая линия ТП")
                        descriptionSubtasks = ""
                        break
                    }
                    case "boolPassportServices" : {
                        descriptionSubtasks = descriptionSubtasks + "-- Раздел Сервисы\n"
                        newMainIssueSubtask = createIssueObject(newMainIssue, "Подзадача", summary + " Раздел Сервисы.", description + descriptionSubtasks, objectOrg.getObjectKey().toString(), "Первая линия ТП")
                        descriptionSubtasks = ""
                        break
                    }
                    case "boolPassportLicenses" : {
                        descriptionSubtasks = descriptionSubtasks + "-- Раздел Лицензии\n"
                        newMainIssueSubtask = createIssueObject(newMainIssue, "Подзадача", summary + " Раздел Лицензии.", description + descriptionSubtasks, objectOrg.getObjectKey().toString(), "Первая линия ТП")
                        descriptionSubtasks = ""
                        break
                    }
                    case "boolPassportBackups" : {
                        descriptionSubtasks = descriptionSubtasks + "-- Раздел Резервное копирование\n"
                        newMainIssueSubtask = createIssueObject(newMainIssue, "Подзадача", summary + " Раздел Резервное копирование.", description + descriptionSubtasks, objectOrg.getObjectKey().toString(), "Первая линия ТП")
                        descriptionSubtasks = ""
                        break
                    }
                    case "boolPassportPasswords" : {
                        descriptionSubtasks = descriptionSubtasks + "-- Раздел Пароли\n"
                        newMainIssueSubtask = createIssueObject(newMainIssue, "Подзадача", summary + " Раздел Пароли.", description + descriptionSubtasks, objectOrg.getObjectKey().toString(), "Первая линия ТП")
                        descriptionSubtasks = ""
                        break
                    }
                }
            }
        }
        return newMainIssue
    } else {
        throw new InvalidInputException ("Issue maintask create is null. Op has failed for ${objectOrg.getName()}.")
    }    
}

// objectsOrg of companies to audit passports
def objectsOrg = iqlFacade.findObjectsByIQLAndSchema(9, 'objectTypeId = 38 and Договора is not EMPTY and Обслуживание not in ("Стоп обслуживание", "Предоплата") and object HAVING outboundReferences(objectTypeId = 150 and "Дата окончания договора" >= startOfDay() and object HAVING outboundReferences(objectTypeId = 423 and object HAVING outboundReferences(objectTypeId = 148 and object HAVING outboundReferences(objectTypeId = 33 and key in (CRM-121555, CRM-182, CRM-175, CRM-123776, CRM-2545, CRM-121601, CRM-179, CRM-2551, CRM-2552, CRM-121584, CRM-121572, CRM-178, CRM-123226, CRM-2555, CRM-181) and object HAVING outboundReferences(objectTypeId = 56 and key = CRM-363)))))')
if (!objectsOrg.isEmpty()) {
    for (objectOrg in objectsOrg) {
        // objectsServices of payment services of each companie in objectsOrg           
        def objectsServices = iqlFacade.findObjectsByIQLAndSchema(3,"objectTypeId = 33 and object HAVING inboundReferences(objectTypeId = 148 and object HAVING inboundReferences(objectTypeId = 423 and object HAVING inboundReferences(objectTypeId = 150 and object HAVING inboundReferences(objectTypeId = 38 and key = ${objectOrg.getObjectKey()}))))")
        if (!objectsServices.isEmpty()) {
            // Check resolution date of last inventory issue and six month to now for objectOrg
            Boolean timeTocreateNewIssue = false
            if (!objectOrg.getAttributeValues('Актуализация ИТ').empty) {
                if (ComponentAccessor.getIssueManager().isExistingIssueKey(objectOrg.getAttributeValues('Актуализация ИТ')[0].getValue().toString())) {
                    Issue orgPreviousTaskIssue = ComponentAccessor.getIssueManager().getIssueByCurrentKey(objectOrg.getAttributeValues('Актуализация ИТ')[0].getValue().toString())
                    if (orgPreviousTaskIssue) {
                        if (orgPreviousTaskIssue.resolutionDate) {
                            def orgPreviousTaskLastResolutionDate = orgPreviousTaskIssue.getResolutionDate()
                            def today = new java.sql.Timestamp(new Date().getTime()).toLocalDate()
                            def monthsBetween = ChronoUnit.MONTHS.between(orgPreviousTaskLastResolutionDate.toLocalDate(), today)
                            if (monthsBetween >= 6) {
                                timeTocreateNewIssue = true
                            } else {
                                timeTocreateNewIssue = false
                            }
                        } else {
                            timeTocreateNewIssue = false
                        }                        
                    } else {
                        timeTocreateNewIssue = true
                    }                    
                } else {
                    timeTocreateNewIssue = true
                }
            } else {
                timeTocreateNewIssue = true
            }            
            // create issues if it`s time
            if (timeTocreateNewIssue = true) {
                //
                // Create Map of Boolean values
                Map<String, Boolean> boolPassportflags = [
                    "boolPassportInet": false,
                    "boolPassportLogicInet": false,
                    "boolPassportPhisicsInet": false,  
                    "boolPassportWifiInet": false,
                    "boolPassportPhones": false,
                    "boolPassport1c": false,
                    "boolPassportAD": false,
                    "boolPassportFiles": false,  
                    "boolPassportPrinting": false,
                    "boolPassportRemotes": false,
                    "boolPassportServerRooms": false,
                    "boolPassportServers": false,
                    "boolPassportServices": false,  
                    "boolPassportLicenses": false,
                    "boolPassportBackups": false,
                    "boolPassportPasswords": false
                ]
                // get Passports types 
                for (objectServices in objectsServices) {
                    switch (objectServices.getObjectKey()) {
                        // Обслуживание аппаратных IP-телефонов
                        case 'CRM-121555': {
                            boolPassportflags.boolPassportPhones = true
                            break
                        }
                        // Обслуживание оргтехники                    
                        case 'CRM-182': {
                            boolPassportflags.boolPassportPrinting = true
                            break
                        }
                        // Обслуживание ПК
                        case 'CRM-175': {
                            boolPassportflags.boolPassportInet = true
                            boolPassportflags.boolPassport1c = true
                            boolPassportflags.boolPassportPrinting = true
                            boolPassportflags.boolPassportRemotes = true
                            boolPassportflags.boolPassportServices = true
                            boolPassportflags.boolPassportLicenses = true
                            boolPassportflags.boolPassportPasswords = true
                            break
                        }
                        // Обслуживание различного сетевого оборудования
                        case 'CRM-123776': {
                            boolPassportflags.boolPassportInet = true
                            boolPassportflags.boolPassportLogicInet = true
                            boolPassportflags.boolPassportPhisicsInet = true
                            boolPassportflags.boolPassportWifiInet = true
                            boolPassportflags.boolPassportRemotes = true
                            boolPassportflags.boolPassportPasswords = true
                            break
                        }
                        // Обслуживание сервера IP-телефонии
                        case 'CRM-2545': {
                            boolPassportflags.boolPassportPhones = true
                            boolPassportflags.boolPassportServers = true
                            boolPassportflags.boolPassportBackups = true
                            boolPassportflags.boolPassportPasswords = true
                            break
                        }
                        // Обслуживание сервера Linux
                        case 'CRM-121601': {
                            boolPassportflags.boolPassportServers = true
                            boolPassportflags.boolPassportServices = true
                            boolPassportflags.boolPassportLicenses = true
                            boolPassportflags.boolPassportBackups = true
                            boolPassportflags.boolPassportPasswords = true
                            break
                        }
                        // Обслуживание сервера Windows
                        case 'CRM-179': {
                            boolPassportflags.boolPassportServers = true
                            boolPassportflags.boolPassportServices = true
                            boolPassportflags.boolPassportLicenses = true
                            boolPassportflags.boolPassportBackups = true
                            boolPassportflags.boolPassportPasswords = true
                            break
                        }
                        // Обслуживание сервера виртуализации
                        case 'CRM-2551': {
                            boolPassportflags.boolPassportServers = true
                            boolPassportflags.boolPassportServices = true
                            boolPassportflags.boolPassportLicenses = true
                            boolPassportflags.boolPassportBackups = true
                            boolPassportflags.boolPassportPasswords = true
                            break
                        }
                        // Обслуживание сервера СХД (система хранения данных)
                        case 'CRM-2552': {
                            boolPassportflags.boolPassportServers = true
                            boolPassportflags.boolPassportServices = true
                            boolPassportflags.boolPassportLicenses = true
                            boolPassportflags.boolPassportBackups = true
                            boolPassportflags.boolPassportPasswords = true
                            break
                        }
                        // Обслуживание сетевого хранилища NAS
                        case 'CRM-121584': {
                            boolPassportflags.boolPassportInet = true
                            boolPassportflags.boolPassportLogicInet = true
                            boolPassportflags.boolPassportPhisicsInet = true
                            boolPassportflags.boolPassportWifiInet = true
                            boolPassportflags.boolPassportRemotes = true
                            boolPassportflags.boolPassportPasswords = true
                            break
                        }
                        // Обслуживание управляемого коммутатора
                        case 'CRM-121572': {
                            boolPassportflags.boolPassportInet = true
                            boolPassportflags.boolPassportLogicInet = true
                            boolPassportflags.boolPassportPhisicsInet = true
                            boolPassportflags.boolPassportWifiInet = true
                            boolPassportflags.boolPassportRemotes = true
                            boolPassportflags.boolPassportPasswords = true
                            break
                        }
                        // Обслуживание управляемого маршрутизатора
                        case 'CRM-178': {
                            boolPassportflags.boolPassportInet = true
                            boolPassportflags.boolPassportLogicInet = true
                            boolPassportflags.boolPassportPhisicsInet = true
                            boolPassportflags.boolPassportWifiInet = true
                            boolPassportflags.boolPassportRemotes = true
                            boolPassportflags.boolPassportPasswords = true
                            break
                        }
                        // Поддержка облачных продуктов
                        case 'CRM-123226': {
                            boolPassportflags.boolPassportServices = true
                            boolPassportflags.boolPassportLicenses = true
                            boolPassportflags.boolPassportBackups = true
                            boolPassportflags.boolPassportPasswords = true
                            break
                        }
                        // Поддержка специализированных информационных систем
                        case 'CRM-2555': {
                            boolPassportflags.boolPassportServices = true
                            boolPassportflags.boolPassportLicenses = true
                            boolPassportflags.boolPassportBackups = true
                            boolPassportflags.boolPassportPasswords = true
                            break
                        }
                        // Удаленное абонентское обслуживание ПК
                        case 'CRM-181': {
                            boolPassportflags.boolPassportInet = true
                            boolPassportflags.boolPassport1c = true
                            boolPassportflags.boolPassportPrinting = true
                            boolPassportflags.boolPassportRemotes = true
                            boolPassportflags.boolPassportServices = true
                            boolPassportflags.boolPassportLicenses = true
                            boolPassportflags.boolPassportPasswords = true
                            break
                        }                                      
                    }
                }
                // create task and subtasks for objectOrg
                if (boolPassportflags.values().any { it }) {
                    Issue issueMain = createIssues(objectOrg, boolPassportflags)
                    log.warn(issueMain)
                }
            }            
        }        
    }
} else {
    return null
}
