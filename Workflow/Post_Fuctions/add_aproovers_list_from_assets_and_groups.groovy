import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.util.json.JSONObject

/* Get Insight Object Facade from plugin accessor */
Class objectFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade");  
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(objectFacadeClass);           
/* Get Insight IQL Facade from plugin accessor */
Class iqlFacadeClass = ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade");
def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(iqlFacadeClass);
//def projectChief = iqlFacade.findObjectsByIQLAndSchema(3, "objectTypeId = 56 and Проект = " + issue.getProjectObject().getKey()); 
def projectChiefSearch = iqlFacade.findObjects("objectTypeId = 56 and Проект = " + issue.getProjectObject().getKey())
def projectChiefSearchAttrBean = objectFacade.loadObjectAttributeBean(projectChiefSearch[0].getId(), 'Руководитель направления')
def projectChiefSearchAttr = projectChiefSearchAttrBean.getObjectAttributeValueBeans()[0].getValue() 
issue.set {
        //setCustomFieldValue('Заверители', "n.nikitashin, d.bazekin")        
        def approoversList = ''
        def approoversListComment = """h3. *Для закрытия данного запроса требуется подтверждения одного из сотрудников отдела сопровождения или руководителя отдела.*
         Список сотрудников отдела сопровождения, кто может закрыть запрос:
        """
        def approoversListCount = ComponentAccessor.getGroupManager().getUsersInGroupCount("support-dept-common")
        for (int i = 0; i < approoversListCount; i++) {            
            approoversList = approoversList + ComponentAccessor.getGroupManager().getUsersInGroup("support-dept-common",false)[i].getName()
            approoversListComment = approoversListComment + "-- *" + ComponentAccessor.getGroupManager().getUsersInGroup("support-dept-common",false)[i].getDisplayName() + "*"
            approoversList = approoversList + ", "
            approoversListComment = approoversListComment + '\n'
        }
        setCustomFieldValue('Заверители', approoversList, projectChiefSearchAttr)        
        approoversListComment = approoversListComment + "h3. *Руководитель отдела* - " + ComponentAccessor.getUserManager().getUserByKey(projectChiefSearchAttr).getDisplayName()
        final SD_PUBLIC_COMMENT = "sd.public.comment"
        def properties = [(SD_PUBLIC_COMMENT): new JSONObject(["internal": true] as Map)]
        ComponentAccessor.getCommentManager().create(issue,ComponentAccessor.getUserManager().getUserByName('robot'), approoversListComment, null, null, new Date(),properties,false)     
    }
