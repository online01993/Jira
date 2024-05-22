import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.Issue;
import com.opensymphony.workflow.InvalidInputException;


//def issue = Issues.getByKey('ITS-89790')
/* Custom field with the value to filter on (blocking)*/
CustomField cfblocking = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12401);
def valueCFcfblocking = issue.getCustomFieldValue(cfblocking);        
/* Custom field for the waitng field */
CustomField cfwaitinginsight = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12400);
def valuecfwaitinginsight = issue.getCustomFieldValue(cfwaitinginsight);               
/* Get Insight Object Facade from plugin accessor */
Class objectFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade");  
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectFacadeClass);
/* Get Insight IQL Facade from plugin accessor */
Class iqlFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade");
def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(iqlFacadeClass);

/*exit if valuecfwaitinginsight is empty      */
if (valuecfwaitinginsight == null) {
    throw new InvalidInputException("Ожидание", "Ожидание должно быть заполнено")
}

/* Get IQL search and return true?false       */
def objects = iqlFacade.findObjectsByIQLAndSchema(9, "Блокируемые = true and Key = ID-" + valuecfwaitinginsight[0].getId()); 
if ((!objects.isEmpty()) && (valueCFcfblocking == null)) {
    throw new InvalidInputException("Блокируется", "Для данной категории ожидающих резолюции требуется указание блокирующих задач к текущей из-за которых невозможно дальнейшее продолжение работ")
   } else
	   if ((!objects.isEmpty()) && (valueCFcfblocking != null)) {
			/* Set boolean varriable to false for matching valueCFcfblocking and valuecfwaitinginsight*/
			def waitingMissMatch = false
			/* Set Project Dept KeyID varriable for matching projectBlocking and waitingBlocking*/
			def waitingIssueProject
			/* Set Project Dept Resolution varriable for getting open blocked by issues only*/
			def waitingIssueResolution
			/* Start for loop to check matching valueCFcfblocking and valuecfwaitinginsight*/ 
			for (int i = 0; i < valueCFcfblocking.size(); i++) {
				/* Get ProjectName for input issue*/
				waitingIssueProject = Issues.getByKey(valueCFcfblocking[i].toString()).getProjectObject().id
				waitingIssueResolution = Issues.getByKey(valueCFcfblocking[i].toString()).getResolution()
				if (waitingIssueResolution != null) {
					throw new InvalidInputException("Блокируется", "Нужно выбирать ТОЛЬКО открытую задачу. А выбранная задача - " + Issues.getByKey(valueCFcfblocking[i].toString()) + " не является таковой.")
					break
				}
				switch (waitingIssueProject) {
					case 10005:
					//case "SALE":
						//log.warn("case SALE")
						//log.warn("WaitResolutionID is - " + valuecfwaitinginsight[0].getId())
						if (valuecfwaitinginsight[0].getId() != 120781) {//Ожидание ответа от коммерческой службы, ID-120781
							waitingMissMatch = true  
							break          
						} else
						if (valuecfwaitinginsight[0].getId() == 120781) {
							waitingMissMatch = false
							//log.warn("exit loop from SALE")
							break
						}
					case 10010:
					//case "OTO":
						//log.warn("case OTO")
						//log.warn("WaitResolutionID is - " + valuecfwaitinginsight[0].getId())
						if (valuecfwaitinginsight[0].getId() != 121041) {//Ожидание выполнения заявки отдела автоматизации, ID-121041
							waitingMissMatch = true 
							break           
						} else
						if (valuecfwaitinginsight[0].getId() == 121041) {
							waitingMissMatch = false
							//log.warn("exit loop from OTO")
							break
						}
					case 10301:
					//case "SUP":
						//log.warn("case SUP")
						//log.warn("WaitResolutionID is - " + valuecfwaitinginsight[0].getId())
						if (valuecfwaitinginsight[0].getId() != 121042) {//Ожидание выполнения заявки отдела сопровождения клиентов, ID-121042
							waitingMissMatch = true  
							break          
						} else
						if (valuecfwaitinginsight[0].getId() == 121042) {
							waitingMissMatch = false
							//log.warn("exit loop from SUP")
							break
						}
					case 10007:
					//case "TRANSPORT":
						//log.warn("case TRANSPORT")
						//log.warn("WaitResolutionID is - " + valuecfwaitinginsight[0].getId())
						if (valuecfwaitinginsight[0].getId() != 121043) {//Ожидание выполнения заявки отдела логистики, ID-121043
							waitingMissMatch = true  
							break          
						} else
						if (valuecfwaitinginsight[0].getId() == 121043) {
							waitingMissMatch = false
							//log.warn("exit loop from TRANSPORT")
							break
						}        
					case 10009:
					//case "ITS":
						//log.warn("case ITS")
						//log.warn("WaitResolutionID is - " + valuecfwaitinginsight[0].getId())            
						if (valuecfwaitinginsight[0].getId() != 120785) {//Ожидание реализации проекта отдела ИТ, ID-120785                
							waitingMissMatch = true 
							break           
						} else
						if (valuecfwaitinginsight[0].getId() == 120785) {
							waitingMissMatch = false
							//log.warn("exit loop from ITS")
							break
						}
					default:
						log.warn("Unknown Project dept key ID")
						waitingMissMatch = true
						break       
				}
				if (waitingMissMatch == false) {
					//log.warn("break for circle")
					break
				}
			}
			/* Return error exception if not matched*/
			if (waitingMissMatch == true) {
				switch (valuecfwaitinginsight[0].getId()) {
					case 120781:
					//case "SALE":
						//log.warn("case SALE")
						//log.warn("WaitResolutionID is - " + valuecfwaitinginsight[0].getId())
						throw new InvalidInputException("Блокируется", "Не корректное указание блокирующей резолюции к данным блокирующим задачам -- Нужно выбрать открытую задачу отдела продаж")
						break
					case 121041:
					//case "OTO":
						//log.warn("case OTO")
						//log.warn("WaitResolutionID is - " + valuecfwaitinginsight[0].getId())
						throw new InvalidInputException("Блокируется", "Не корректное указание блокирующей резолюции к данным блокирующим задачам -- Нужно выбрать открытую задачу отдела автоматизации")
						break
					case 121042:
					//case "SUP":
						//log.warn("case SUP")
						//log.warn("WaitResolutionID is - " + valuecfwaitinginsight[0].getId())
						throw new InvalidInputException("Блокируется", "Не корректное указание блокирующей резолюции к данным блокирующим задачам -- Нужно выбрать открытую задачу отдела сопровождения")
						break
					case 121043:
					//case "TRANSPORT":
						//log.warn("case TRANSPORT")
						//log.warn("WaitResolutionID is - " + valuecfwaitinginsight[0].getId())
						throw new InvalidInputException("Блокируется", "Не корректное указание блокирующей резолюции к данным блокирующим задачам -- Нужно выбрать открытую задачу отдела логистики")        
						break
					case 120785:
					//case "ITS":
						//log.warn("case ITS")
						//log.warn("WaitResolutionID is - " + valuecfwaitinginsight[0].getId())            
						throw new InvalidInputException("Блокируется", "Не корректное указание блокирующей резолюции к данным блокирующим задачам -- Нужно выбрать открытую задачу отдела ИТ")
						break
					default:
						log.warn("Unknown Project dept key ID")
						waitingMissMatch = true
						break
				}
			}
	   } else 
		   if ((objects.isEmpty()) && (valueCFcfblocking != null)) {
				throw new InvalidInputException("Блокируется", "Нельзя выбирать блокирующую задачу когда выбрана не блокирующая резолюция ожидания")				
		   }
return true
