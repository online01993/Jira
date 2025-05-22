import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder


import com.adaptavist.hapi.jira.assets.Assets
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean

List<Map> callZabbixApi(Map args) {
    /**
    * Универсальная функция для выполнения запросов к Zabbix API
    * @param method Метод API (по умолчанию "trigger.get")
    * @param params Параметры запроса (по умолчанию пустой Map)
    * @param apiUrl URL Zabbix API (по умолчанию из примера curl)
    * @param authToken Токен аутентификации (по умолчанию из примера curl)
    * @param requestId ID запроса (по умолчанию 1)
    * @param jsonrpc Версия JSON-RPC (по умолчанию "2.0")
    * @return Ответ от API в виде Map
    */
    // Проверка обязательного параметра
    if (!args.triggerIds) {
        throw new IllegalArgumentException("Параметр triggerIds является обязательным")
    }
    // Устанавливаем значения по умолчанию
    Map<String, ?> config = [
        method: "trigger.get",
        params: [
                "triggerids": args.triggerIds,
                "selectTriggerDiscovery": "extend",
                "output": "extend",
                "selectFunctions": "extend",
                "selectDiscoveryRule": "extend"
            ],
        apiUrl: "https://zabbix.it-russia.com/api_jsonrpc.php",
        authToken: "04df13a082d46b701f64b9c6f6dc88305c71d5831e91da52fb988d56c8781a37",
        //user zabbix-robot
        requestId: 1,
        jsonrpc: "2.0"
    ] + args // Объединяем с переданными аргументами
    try {
        // Формируем тело запроса
        Map<String, ?> requestBody = [
            jsonrpc: config.jsonrpc,
            method: config.method,
            params: config.params ?: [:],
            id: config.requestId,
            auth: config.authToken
        ]
        // Конвертируем в JSON
        def jsonBody = JsonOutput.toJson(requestBody)
        // Создаем HTTP-клиент
        def httpClient = HttpClientBuilder.create().build()
        def httpPost = new HttpPost(config.apiUrl as String)        
        // Устанавливаем заголовки
        httpPost.setHeader("Content-Type", "application/json-rpc")
        httpPost.setEntity(new StringEntity(jsonBody))
        // Выполняем запрос
        def response = httpClient.execute(httpPost)
        String responseBody = response.getEntity().getContent().text        
        // Парсим ответ
        def jsonResponse = new JsonSlurper().parseText(responseBody) as Map<String, Object>
        // Закрываем соединение
        response.close()
        httpClient.close()        
        //return jsonResponse.get('result') as Map<String, Object>
        return jsonResponse.get('result') as List<Map>
    } catch (Exception e) {
        log.error("Ошибка при выполнении запроса к Zabbix API: ${e.message}")
        throw e
    }
}

List<ObjectBean> assetsTriggerSearch(String triggerId, Map args = [:]) {
    def config = [
        schemeId: 25,
        triggerId: triggerId,
        triggerType: "trigger"
        //triggerType: "triggerTemplate"
    ] << args // Объединяем с переданными аргументами
    @WithPlugin("com.riadalabs.jira.plugins.insight")
    @PluginModule IQLFacade iqlFacade
    try {
        def objects
        switch (config.triggerType) {
            case "trigger": {
                objects = iqlFacade.findObjectsByIQLAndSchema(config.schemeId as Integer, "objecttypeid = 427 and TriggerID = ${config.triggerId}")
                if (!objects.isEmpty()) {
                    return objects
                } else {
                    return null
                }
            }
            case "triggerTemplate": {
                objects = iqlFacade.findObjectsByIQLAndSchema(config.schemeId as Integer, "objecttypeid = 428 and TriggerID = ${config.triggerId}")
                if (!objects.isEmpty()) {
                    return objects
                } else {
                    return null
                }
            }
        }        
    } catch (Exception e) {
        log.error("Ошибка при выполнении запроса к поиску Assets Jira: ${e.message}")
        throw e
    }
}

ObjectBean assetsTriggerCreate(String triggerType, String parentTrigger, String infoSys, String name, String triggerId, String severityTemplate, String description, String ingType, String url) {
    try {
        def objectNew
        switch (triggerType) {
            case "trigger": {                
                objectNew = Assets.create('ZID', 'Triggers') {
                    if (name) {
                        setAttribute('Название триггера', name)
                    }
                    setAttribute('TriggerID', triggerId)
                    switch (severityTemplate) {
                        case "5": setAttribute('Важность', 'Чрезвычайная')
                        case "4": setAttribute('Важность', 'Высокая')
                        case "3": setAttribute('Важность', 'Средняя')
                        case "2": setAttribute('Важность', 'Предупреждение')
                        case "1": setAttribute('Важность', 'Информация')
                        default : setAttribute('Важность', 'Чрезвычайная')
                    }
                    if (description) {
                        setAttribute('Описание', description)
                    }
                    setAttribute('Категория инженера', Assets.getByKey(ingType).getObjectKey())
                    if (infoSys != null) {
                        setAttribute('Инфосистема', Assets.getByKey(infoSys).getObjectKey())                        
                    }                    
                    if (url != null) {
                        setAttribute('Zabbix URL', url)
                    } else {
                        setAttribute('Zabbix URL', "https://zabbix.it-russia.com/zabbix.php?show=2&name=&severities%5B0%5D=0&severities%5B1%5D=1&severities%5B2%5D=2&severities%5B3%5D=3&severities%5B4%5D=4&severities%5B5%5D=5&show_symptoms=1&show_suppressed=1&inventory%5B0%5D%5Bfield%5D=type&inventory%5B0%5D%5Bvalue%5D=&evaltype=0&tags%5B0%5D%5Btag%5D=&tags%5B0%5D%5Boperator%5D=0&tags%5B0%5D%5Bvalue%5D=&show_tags=3&tag_name_format=0&tag_priority=&show_opdata=2&show_timeline=1&details=1&filter_name=&filter_show_counter=0&filter_custom_time=0&sort=clock&sortorder=DESC&age_state=0&unacknowledged=0&compact_view=0&highlight_row=0&action=problem.view&triggerids%5B%5D=${triggerId}")
                    }                    
                    if (parentTrigger != null) {
                        setAttribute('Категория триггера', parentTrigger)
                    }
                }
                return objectNew
            }
            case "triggerTemplate": {
                objectNew = Assets.create('ZID', 'Trigger Template') {
                    if (name) {
                        setAttribute('Название триггера', name)
                    }                    
                    setAttribute('TriggerID', triggerId)
                    switch (severityTemplate) {
                        case "5": setAttribute('Важность', 'Чрезвычайная')
                        case "4": setAttribute('Важность', 'Высокая')
                        case "3": setAttribute('Важность', 'Средняя')
                        case "2": setAttribute('Важность', 'Предупреждение')
                        case "1": setAttribute('Важность', 'Информация')
                        default : setAttribute('Важность', 'Чрезвычайная')
                    }
                    if (description) {
                        setAttribute('Описание', description)
                    }                    
                    setAttribute('Категория инженера', Assets.getByKey(ingType).getObjectKey())
                    if (url != null) {
                        setAttribute('Zabbix URL', url)
                    }    
                    if (parentTrigger) {                        
                        setAttribute('Категория триггера', parentTrigger)
                    }
                }
                return objectNew
            }
        }        
    } catch (Exception e) {
        log.error("Ошибка при выполнении запроса к созданию Assets Jira: ${e.message}")
        throw e
    }
}

ObjectBean assetsTriggerTemplatesCreate(String templateType, String name, String triggerId, String severityTemplate, String description, String ingType) {
    try {
        def objectTriggerTemplateIDNew = triggerId
        def objectTriggerTemplateNew
        def zabbixTriggerTemplateNewGet = callZabbixApi(method: templateType, triggerIds: [objectTriggerTemplateIDNew])
        def objectTriggerTemplateParentID = zabbixTriggerTemplateNewGet[0].templateid as String
        def url
        // Определяем генерацию URL триггера в соответствии с типом шаблона триггера - динамический или статический
        if (templateType == "trigger.get") {
            url = "https://zabbix.it-russia.com/triggers.php?form=update&triggerid=${objectTriggerTemplateIDNew}&context=template"
        }
        if (templateType == "triggerprototype.get") {
            def discoveryRule = zabbixTriggerTemplateNewGet[0].discoveryRule as Map
            def discoverid = discoveryRule?.itemid as String
            //def discoverid = zabbixTriggerTemplateNewGet[0].discoveryRule.itemid.toString()
            url = "https://zabbix.it-russia.com/trigger_prototypes.php?form=update&parent_discoveryid=${discoverid}&triggerid=${objectTriggerTemplateIDNew}&context=template"
        }
        if (!objectTriggerTemplateParentID) {
            throw new IllegalArgumentException("Ошибка обработки функции, не удалось определить параметр родителя objectTriggerTemplateParentID")
        }        
        if (objectTriggerTemplateParentID == "0") {
            // текущий триггер является верхоуровневым, обход дальнейший не нужен
            objectTriggerTemplateNew = assetsTriggerCreate("triggerTemplate", null, null, zabbixTriggerTemplateNewGet[0].description as String, objectTriggerTemplateIDNew, zabbixTriggerTemplateNewGet[0].priority as String, zabbixTriggerTemplateNewGet[0].comments as String, ingType, url)
            return objectTriggerTemplateNew                    
        } else {
            // текущий триггер является дочерним. нужно построить дерево зависимых триггеров
            // запускаем цикл проверки наличия родительского триггера в базе и создания его если он там отсутствует
            def objectsTemplateTemplate = assetsTriggerSearch(objectTriggerTemplateParentID, [triggerType: "triggerTemplate"])
            if (objectsTemplateTemplate != null) {
                // карточка родителя есть -- создаём текущий триггер с указанием на родителя
                objectTriggerTemplateNew = assetsTriggerCreate("triggerTemplate", objectsTemplateTemplate[0].getObjectKey(), null, zabbixTriggerTemplateNewGet[0].description as String, objectTriggerTemplateIDNew, zabbixTriggerTemplateNewGet[0].priority as String, zabbixTriggerTemplateNewGet[0].comments as String, ingType, url)
                return objectTriggerTemplateNew
            } else {
                // карточка родителя отсутствует - запускаем цикл создания родителей
                def objectTriggerTemplateParentNew = assetsTriggerTemplatesCreate(templateType, zabbixTriggerTemplateNewGet[0].description as String, objectTriggerTemplateParentID, zabbixTriggerTemplateNewGet[0].priority as String, zabbixTriggerTemplateNewGet[0].comments as String, ingType)
                // теперь запускаем создание промежуточного темплейта
                objectTriggerTemplateNew = assetsTriggerCreate("triggerTemplate", objectTriggerTemplateParentNew.getObjectKey(), null, zabbixTriggerTemplateNewGet[0].description as String, objectTriggerTemplateIDNew, zabbixTriggerTemplateNewGet[0].priority as String, zabbixTriggerTemplateNewGet[0].comments as String, ingType, url)
                // возвращаем промежуточный темплейт
                return objectTriggerTemplateNew
            }
        } 
    } catch (Exception e) {
        log.error("Ошибка при выполнении запроса к созданию Assets Jira: ${e.message}")
        throw e
    }
}

///
/// Main script
///
def objects
def triggerIdZabbix = issue.getCustomFieldValue('triggerID_Zabbix')
if (triggerIdZabbix != null) {
    objects = assetsTriggerSearch(triggerIdZabbix, [triggerType: "trigger"])
    if (objects != null) {
        issue.update { 
            setCustomFieldValue('triggerID_Jira',objects[0].getObjectKey())
        }
        Assets.getByKey(objects[0].getObjectKey()).update { 
            setAttribute('Задача', issue)
            setAttribute('Последнее срабатывание', issue.getCreated().toLocalDateTime().format('dd.MM.yyyy - HH:mm:ss'))
        }
    } else {
        def triggerIdChield = triggerIdZabbix
        def triggerType
        def ingType
        def objectNew        
        def zabbixTriggerGetTemp = callZabbixApi(triggerIds: [triggerIdChield])
        //// Проверка на одиночную цепочку триггера
        if (zabbixTriggerGetTemp[0].triggerDiscovery == []) {
            if (zabbixTriggerGetTemp[0].templateid as String == '0') {
                //Триггер без родителя, создаём одиночный триггер
                ingType = "ID-607"
                triggerType = "trigger"                
                objectNew = assetsTriggerCreate(triggerType, null, issue.getCustomFieldValue('Инфосистема')[0].getObjectKey().toString(), zabbixTriggerGetTemp[0].description as String, triggerIdChield.toString(), zabbixTriggerGetTemp[0].priority as String, zabbixTriggerGetTemp[0].comments as String, ingType, null)
                // Обнволяем поле задачи у добавляем созданную карточку триггера
                issue.update { 
                    setCustomFieldValue('triggerID_Jira', objectNew.getObjectKey())
                }
                return objectNew
            }            
        }
        // Создаём карточку и триггера и входим в цикил поиска/создания родительских триггеров
        // Узнаём категорию триггера -- static или dynamic 
        if (zabbixTriggerGetTemp[0].triggerDiscovery == []) {
            //// Обработка статического триггера
            triggerType = "triggerTemplate"
            def objectTemplate = assetsTriggerSearch(zabbixTriggerGetTemp[0].templateid as String, [triggerType: triggerType])
            if (objectTemplate != null) {
                // В базе есть такой родительский триггер, поэтому создаем карточку дочернего триггера
                // Определим категорию инженера для данного триггера исходя из найденной записи родительского шаблона                
                if (objectTemplate[0].getReference('Категория инженера').getObjectKey()) {
                    if (objectTemplate[0].getReference('Категория инженера').getObjectKey() == "ID-606") {
                        ingType = "ID-606" // Первая линия технической поддержки
                    } else {
                        if (objectTemplate[0].getReference('Категория инженера').getObjectKey() == "ID-607") {
                            ingType = "ID-607" // Вторая линия технической поддержки
                        } else {
                            ingType = "ID-607" // По-умолчанию установим вторая линия технической поддержки
                        }
                    }                
                } else {
                    ingType = "ID-607" // По-умолчанию установим вторая линия технической поддержки
                }
                /*
                switch (objectTemplate[0].getReference('Категория инженера').getObjectKey()) {
                    case "ID-606": ingType = "ID-606" // Первая линия технической поддержки
                    case "ID-607": ingType = "ID-607" // Вторая линия технической поддержки
                    default : ingType = "ID-607" // По-умолчанию установим вторая линия технической поддержки
                }
                */
                // Создаём карточку дочернего триггера с привязкой к инфосистеме
                triggerType = "trigger"                
                objectNew = assetsTriggerCreate(triggerType, objectTemplate[0].getObjectKey().toString(), issue.getCustomFieldValue('Инфосистема')[0].getObjectKey().toString(), zabbixTriggerGetTemp[0].description as String, triggerIdChield.toString(), zabbixTriggerGetTemp[0].priority as String, zabbixTriggerGetTemp[0].comments as String, ingType, null)
                // Обнволяем поле задачи у добавляем созданную карточку триггера
                issue.update { 
                    setCustomFieldValue('triggerID_Jira', objectNew.getObjectKey())
                }
            } else {
                // В базе данных нет записи о таком (zabbixTriggerGetTemp[0].templateid as String) родительском триггере, создаём родительский триггер
                // Инициируем цикл созданий цепочки родительских триггеров пока не дойдем до самого корневого "templateid": "0"
                triggerType = "triggerTemplate"
                def triggerIdTemplate = zabbixTriggerGetTemp[0].templateid as String
                def zabbixTriggerTemplateNewGet = callZabbixApi(triggerIds: [triggerIdTemplate])
                // Определим категорию инженера для данного триггера исходя из найденной записи родительского шаблона
                ingType = "ID-607" // По-умолчанию установим вторая линия технической поддержки                
                // Запускаем цикл создания родительских карточек триггеров
                def objectTemplateNew = assetsTriggerTemplatesCreate("trigger.get", zabbixTriggerTemplateNewGet[0].description as String, triggerIdTemplate, zabbixTriggerTemplateNewGet[0].priority as String, zabbixTriggerTemplateNewGet[0].comments as String, ingType)
                // Создаём карточку дочернего триггера с привязкой к инфосистеме
                triggerType = "trigger"
                objectNew = assetsTriggerCreate(triggerType, objectTemplateNew.getObjectKey().toString(), issue.getCustomFieldValue('Инфосистема')[0].getObjectKey().toString(), zabbixTriggerGetTemp[0].description as String, triggerIdChield.toString(), zabbixTriggerGetTemp[0].priority as String, zabbixTriggerGetTemp[0].comments as String, ingType, null)
                // Обнволяем поле задачи у добавляем созданную карточку триггера
                issue.update { 
                    setCustomFieldValue('triggerID_Jira', objectNew.getObjectKey())
                }
            }
        } else {
            //// Обработка динамического триггера
            triggerType = "triggerTemplate"
            def discoveryRule = zabbixTriggerGetTemp[0].triggerDiscovery as Map
            def parent_triggerid = discoveryRule?.parent_triggerid as String
            //def objectTemplate = assetsTriggerSearch(zabbixTriggerGetTemp[0].triggerDiscovery.parent_triggerid as String, [triggerType: triggerType])
            def objectTemplate = assetsTriggerSearch(parent_triggerid, [triggerType: triggerType])
            if (objectTemplate != null) {
                // В базе есть такой родительский триггер, поэтому создаем карточку дочернего триггера
                // Определим категорию инженера для данного триггера исходя из найденной записи родительского шаблона                
                if (objectTemplate[0].getReference('Категория инженера').getObjectKey()) {
                    if (objectTemplate[0].getReference('Категория инженера').getObjectKey() == "ID-606") {
                        ingType = "ID-606" // Первая линия технической поддержки
                    } else {
                        if (objectTemplate[0].getReference('Категория инженера').getObjectKey() == "ID-607") {
                            ingType = "ID-607" // Вторая линия технической поддержки
                        } else {
                            ingType = "ID-607" // По-умолчанию установим вторая линия технической поддержки
                        }
                    }                
                } else {
                    ingType = "ID-607" // По-умолчанию установим вторая линия технической поддержки
                }
                /*
                switch (objectTemplate[0].getReference('Категория инженера').getObjectKey()) {
                    case "ID-606": ingType = "ID-606" // Первая линия технической поддержки
                    case "ID-607": ingType = "ID-607" // Вторая линия технической поддержки
                    default : ingType = "ID-607" // По-умолчанию установим вторая линия технической поддержки
                }
                */
                // Создаём карточку дочернего триггера с привязкой к инфосистеме
                triggerType = "trigger"                
                objectNew = assetsTriggerCreate(triggerType, objectTemplate[0].getObjectKey().toString(), issue.getCustomFieldValue('Инфосистема')[0].getObjectKey().toString(), zabbixTriggerGetTemp[0].description as String, triggerIdChield.toString(), zabbixTriggerGetTemp[0].priority as String, zabbixTriggerGetTemp[0].comments as String, ingType, null)
                // Обнволяем поле задачи у добавляем созданную карточку триггера
                issue.update { 
                    setCustomFieldValue('triggerID_Jira', objectNew.getObjectKey())
                }
            } else {
                // В базе данных нет записи о таком (zabbixTriggerGetTemp[0].triggerDiscovery.parent_triggerid as String) родительском триггере, создаём родительский триггер
                // Инициируем цикл созданий цепочки родительских триггеров пока не дойдем до самого корневого "templateid": "0"
                triggerType = "triggerTemplate"
                //def triggerIdTemplate = zabbixTriggerGetTemp[0].triggerDiscovery.parent_triggerid as String
                def triggerIdTemplate = parent_triggerid
                def zabbixTriggerTemplateNewGet = callZabbixApi(method: "triggerprototype.get", triggerIds: [triggerIdTemplate])
                // Определим категорию инженера для данного триггера исходя из найденной записи родительского шаблона
                ingType = "ID-607" // По-умолчанию установим вторая линия технической поддержки                
                // Запускаем цикл создания родительских карточек триггеров
                def objectTemplateNew = assetsTriggerTemplatesCreate("triggerprototype.get", zabbixTriggerTemplateNewGet[0].description as String, triggerIdTemplate, zabbixTriggerTemplateNewGet[0].priority as String, zabbixTriggerTemplateNewGet[0].comments as String, ingType)
                // Создаём карточку дочернего триггера с привязкой к инфосистеме
                triggerType = "trigger"
                objectNew = assetsTriggerCreate(triggerType, objectTemplateNew.getObjectKey().toString(), issue.getCustomFieldValue('Инфосистема')[0].getObjectKey().toString(), zabbixTriggerGetTemp[0].description as String, triggerIdChield.toString(), zabbixTriggerGetTemp[0].priority as String, zabbixTriggerGetTemp[0].comments as String, ingType, null)
                // Обнволяем поле задачи у добавляем созданную карточку триггера
                issue.update { 
                    setCustomFieldValue('triggerID_Jira', objectNew.getObjectKey())
                }
            }
        }            
    }
} else {
    return "triggerID_Zabbix is Empty -- exit program"
}
