import com.atlassian.jira.event.type.EventDispatchOption
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import com.atlassian.jira.component.ComponentAccessor
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder

// 1. Получаем текущий issue и историю сообщений
def customFieldManager = ComponentAccessor.customFieldManager

// Поле с вопросом пользователя
def inputField = customFieldManager.getCustomFieldObjectsByName("Новый ИИ-запрос").first()
def userQuestion = issue.getCustomFieldValue(inputField)?.toString()

// Поле для хранения истории диалога (должно быть текстовым полем в Jira)
def historyField = customFieldManager.getCustomFieldObjectsByName("История ИИ-запросов").first()
def conversationHistory = issue.getCustomFieldValue(historyField)?.toString() ?: "[]"

try {
    // 2. Парсим историю предыдущих сообщений
    def messages = new JsonSlurper().parseText(conversationHistory) as List<Map>
    
    // 3. Добавляем новый вопрос пользователя
    messages.add([role: "user", content: userQuestion])
    
    // 4. Формируем запрос с полной историей
    def apiUrl = "https://api.deepseek.com/v1/chat/completions"
    def apiKey = "api-key"
    
    def httpClient = HttpClientBuilder.create().build()
    def httpPost = new HttpPost(apiUrl)
    
    httpPost.setHeader("Content-Type", "application/json")
    httpPost.setHeader("Authorization", "Bearer ${apiKey}")
    
    def requestBody = [
        model: "deepseek-chat",
        messages: messages,
        temperature: 0.7,
        max_tokens: 2048
    ]
    
    httpPost.setEntity(new StringEntity(JsonOutput.toJson(requestBody)))
    
    // 5. Отправляем запрос
    def response = httpClient.execute(httpPost)
    
    if (response.statusLine.statusCode == 200) {
        try {
            // 1. Парсим ответ и явно приводим типы
            def responseText = response.entity.content.text
            Map<String, Object> jsonResponse = (Map<String, Object>) new JsonSlurper().parseText(responseText)
            
            // 2. Безопасное извлечение данных с проверкой типов
            List<Map> choices = (List<Map>) jsonResponse.getOrDefault("choices", [])
            Map firstChoice = choices ? (Map) choices[0] : [:]
            Map message = (Map) firstChoice.getOrDefault("message", [:])
            String assistantResponse = (String) message.getOrDefault("content", "Не получилось извлечь ответ")
            
            // 3. Добавляем ответ ассистента в историю
            List<Map> updatedMessages = new ArrayList<>(messages)
            updatedMessages.add([role: "assistant", content: assistantResponse])
            
            // 4. Сохраняем обновленную историю (не более 10 последних сообщений)
            if (updatedMessages.size() > 10) {
                updatedMessages = updatedMessages[-10..-1]
            }
            issue.setCustomFieldValue(historyField, JsonOutput.toJson(updatedMessages))

            // 5. Сохраняем последний ответ в отдельное поле
            def outputField = customFieldManager.getCustomFieldObjectsByName("Текущий ИИ-запрос").first()
            issue.setCustomFieldValue(outputField, assistantResponse)

            // 6. Сохраняем изменения в Jira issue
            ComponentAccessor.issueManager.updateIssue(
                ComponentAccessor.jiraAuthenticationContext.loggedInUser,
                issue, 
                EventDispatchOption.ISSUE_UPDATED, 
                false
            )
            log.info("Диалог успешно обновлен. Ответ: ${assistantResponse}")
            
        } catch (ClassCastException e) {
            log.error("Неправильная структура ответа API: ${e.message}")
        } catch (Exception e) {
            log.error("Ошибка обработки ответа: ${e.message}")
        }
    }
} catch (Exception e) {
    log.error("Ошибка при обработке диалога: ${e.message}", e)
}
