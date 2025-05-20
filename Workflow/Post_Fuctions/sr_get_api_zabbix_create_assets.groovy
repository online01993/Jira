import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder


import com.adaptavist.hapi.jira.assets.Assets

@WithPlugin("com.riadalabs.jira.plugins.insight")
@PluginModule IQLFacade iqlFacade

def objects
def objectsTemplates
if (issue.getCustomFieldValue('triggerID_Zabbix')) {
    //
    def triggerID_Zabbix_cf = issue.getCustomFieldValue('triggerID_Zabbix')
    objects = iqlFacade.findObjectsByIQLAndSchema(25, "objecttypeid = 427 and TriggerID = ${triggerID_Zabbix_cf}")
    if (!objects.isEmpty()) {
        issue.update { 
            setCustomFieldValue('triggerID_Jira',objects[0].getObjectKey())
        }
        Assets.getByKey(objects[0].getObjectKey()).update { 
            setAttribute('Задача', issue)
            setAttribute('Последнее срабатывание', issue.getCreated().toLocalDateTime().format('dd.MM.yyyy - HH:mm:ss'))
        }
    } else {
        //create trigger db record
        //check parent triggerDB record
        // Параметры запроса
        def url = 'https://zabbix-url/api_jsonrpc.php'
        def authToken = 'authtoken_string'
        def triggerId = triggerID_Zabbix_cf
        // Создаем тело запроса
        def requestBody = [
            "jsonrpc": "2.0",
            "method": "trigger.get",
            "params": [
                "triggerids": triggerId,
                "output": "extend",
                "selectFunctions": "extend"
            ],
            "id": 1,
            "auth": authToken
        ]
        // Преобразуем в JSON
        def jsonBody = JsonOutput.toJson(requestBody)
        // Создаем HTTP-запрос
        def httpClient = HttpClientBuilder.create().build()
        def httpPost = new HttpPost(url)
        // Устанавливаем заголовки и тело запроса
        httpPost.setHeader("Content-Type", "application/json-rpc")
        httpPost.setEntity(new StringEntity(jsonBody))
        // Выполняем запрос и получаем ответ
        def response = httpClient.execute(httpPost)
        def responseBody = response.getEntity().getContent().text
        // Парсим JSON-ответ
        def jsonResponse = new JsonSlurper().parseText(responseBody)
        // Закрываем соединение
        response.close()
        httpClient.close()        
        // Получаем нужные значения из ответа для заполнения новой карточки
        def templateid = ""
        def name = ""
        def description = ""
        def severity = ""
        def lastTime = ""
        if (jsonResponse.result && jsonResponse.result.size() > 0) {
            // Проверяем наличие в базе идентификатоар родительского триггера, если нет то создаём его
            templateid = jsonResponse.result[0].templateid
            objectsTemplates = iqlFacade.findObjectsByIQLAndSchema(25, "objecttypeid = 428 and TriggerID = ${templateid}")
            def objectsNew
            def objectsTemplateNew
            if (objectsTemplates.isEmpty()) {
                //
                def triggerTemplateId = templateid
                // Создаем тело запроса
                def requestBodyTemplate = [
                    "jsonrpc": "2.0",
                    "method": "trigger.get",
                    "params": [
                        "triggerids": triggerTemplateId,
                        "output": "extend",
                        "selectFunctions": "extend"
                    ],
                    "id": 1,
                    "auth": authToken
                ]
                // Преобразуем в JSON
                def jsonBodyTemplate = JsonOutput.toJson(requestBodyTemplate)
                // Создаем HTTP-запрос
                def httpClientTemplate = HttpClientBuilder.create().build()
                def httpPostTemplate = new HttpPost(url)
                // Устанавливаем заголовки и тело запроса
                httpPostTemplate.setHeader("Content-Type", "application/json-rpc")
                httpPostTemplate.setEntity(new StringEntity(jsonBodyTemplate))
                // Выполняем запрос и получаем ответ
                def responseTemplate = httpClientTemplate.execute(httpPostTemplate)
                def responseBodyTemplate = responseTemplate.getEntity().getContent().text
                // Парсим JSON-ответ
                def jsonResponseTemplate = new JsonSlurper().parseText(responseBodyTemplate)
                // Закрываем соединение
                responseTemplate.close()
                httpClientTemplate.close()        
                // Получаем нужные значения из ответа для заполнения новой карточки
                def nameTemplate = ""
                def descriptionTemplate = ""
                def severityTemplate = ""
                if (jsonResponseTemplate.result && jsonResponseTemplate.result.size() > 0) {
                    nameTemplate = jsonResponseTemplate.result[0].description
                    if (jsonResponseTemplate.result[0].comments != '') {
                        descriptionTemplate = jsonResponseTemplate.result[0].comments
                    } else {
                        descriptionTemplate = 'EMPTY'
                    }
                    severityTemplate = jsonResponseTemplate.result[0].priority
                    objectsTemplateNew = Assets.create('ZID', 'Trigger Template') {
                        setAttribute('Название триггера', nameTemplate.toString())
                        setAttribute('TriggerID', triggerTemplateId.toString())
                        switch (severityTemplate) {
                            case "5": setAttribute('Важность', 'Чрезвычайная')
                            case "4": setAttribute('Важность', 'Высокая')
                            case "3": setAttribute('Важность', 'Средняя')
                            case "2": setAttribute('Важность', 'Предупреждение')
                            case "1": setAttribute('Важность', 'Информация')
                            default : setAttribute('Важность', 'Чрезвычайная')
                        }
                        setAttribute('Описание', descriptionTemplate)
                        setAttribute('Категория инженера', Assets.getByKey('ID-607').getObjectKey())
                    }                    
                }
            }
            // создаём triggerId карточку
            name = jsonResponse.result[0].description
            if (jsonResponse.result[0].comments != '') {
                        description = jsonResponse.result[0].comments
                    } else {
                        description = 'EMPTY'
                    }
            severity = jsonResponse.result[0].priority
            objectsNew = Assets.create('ZID', 'Triggers') {
                setAttribute('Название триггера', name)
                setAttribute('TriggerID', triggerId.toString())
                switch (severity) {
                    case "5": setAttribute('Важность', 'Чрезвычайная')
                    case "4": setAttribute('Важность', 'Высокая')
                    case "3": setAttribute('Важность', 'Средняя')
                    case "2": setAttribute('Важность', 'Предупреждение')
                    case "1": setAttribute('Важность', 'Информация')
                    default : setAttribute('Важность', 'Чрезвычайная')
                }
                setAttribute('Описание', description)
                setAttribute('Категория инженера', Assets.getByKey('ID-607').getObjectKey())
                if (objectsTemplates.isEmpty()) {
                    setAttribute('Категория триггера', objectsTemplateNew.getObjectKey())
                } else {
                    setAttribute('Категория триггера', objectsTemplates[0].getObjectKey())
                }
                setAttribute('Инфосистема', issue.getCustomFieldValue('Инфосистема')[0].getObjectKey())
            }       
            // Обновляем поле Jiraзадачи и указываем вновь создаваемую карточку triggerId
            issue.update { 
                setCustomFieldValue('triggerID_Jira', objectsNew.getObjectKey())
            }
        }        
    }
} else {
    return "triggerID_Zabbix is Empty -- exit program"
}
