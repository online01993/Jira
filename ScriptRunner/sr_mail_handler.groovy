import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.util.JiraHome
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.attachment.CreateAttachmentParamsBean
import com.atlassian.jira.service.services.file.FileService
import com.atlassian.jira.service.util.ServiceUtils
import com.atlassian.jira.service.util.handler.MessageUserProcessor
import com.atlassian.mail.MailUtils
import com.atlassian.mail.MailUtils.Attachment
import javax.mail.Message
import javax.mail.Address
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import java.util.regex.Pattern
import java.util.regex.Matcher



//This requires the name of the user that will trigger the Mail Handler
final def username = 'robot'

//This requires the Project's Key that will be used for the Mail Handler
final def projectKey = 'SUP'

//This requires the name of the Issue Type that will be used
final def issueTypeName = 'Поддержка'

//This requires the temporary file name. The file name should end with the .eml extension
final def temporaryAttachmentFileNameOfEml = 'оригинал_письма_tmp.eml'

//This requires the actual file name. The file name should end with the .eml extension
final def actualAttachmentFileNameOfEml = 'оригинал_письма.eml'

//This requires the hostname for IMAP / POP server that is being used
final def mailHost = 'imap.yandex.ru'

//This requires the Email Port that is being used
final def mailPort = '993'

// The name of the custom field to alter
final customFieldName = "Email"

//
/* initialisation vars and func */
//
def userManager = ComponentAccessor.userManager
def projectManager = ComponentAccessor.projectManager
def attachmentManager = ComponentAccessor.attachmentManager
def issueFactory = ComponentAccessor.issueFactory
def messageUserProcessor = ComponentAccessor.getComponent(MessageUserProcessor)
def jiraHome = ComponentAccessor.getComponent(JiraHome)
def commentManager = ComponentAccessor.commentManager
def user = userManager.getUserByName(username)
def reporter = messageUserProcessor.getAuthorFromSender(message) ?: user
def project = projectManager.getProjectObjByKey(projectKey)
def subject = message.subject
def from = message.from.join(',')
Address[] to = message.getRecipients(Message.RecipientType.TO)
Address[] cc = message.getRecipients(Message.RecipientType.CC)
def messageBody = MailUtils.getBody(message)
static Message createMessage(String from, Address[] to, Address[] cc, String subject, String content, String mailHost, String mailPort) {
    def properties = System.properties
    properties.setProperty('mail.smtp.host', mailHost)
    properties.setProperty('mail.smtp.port', mailPort)
    def session = Session.getDefaultInstance(properties)
    def msg = new MimeMessage(session)
    msg.setFrom(new InternetAddress(from))
	//cheking not empty to email recipients
	if (to != null) {
		msg.setRecipients(Message.RecipientType.TO, to)
	}
	//cheking not empty cc email recipients
	if (cc != null) {
		msg.setRecipients(Message.RecipientType.CC, cc)
	}
    msg.setSubject(subject)
    def body = new MimeBodyPart()
    body.setText(content)
    msg.setContent(new MimeMultipart(body))
    msg
}
// 
/* End of initialisation */
//

//
/* Search for current issue or create new issue */
//
def issue = ServiceUtils.findIssueObjectInString(subject) as MutableIssue
if (issue) {   
        commentManager.create(issue, reporter, nonQuotedMessageBody, true)
		def  MailUtils.Attachment[] attachment = MailUtils.getAttachments(message)
			for (def i = 0; i < attachment.length; i++)
				{
					def destinationAttach = new File(jiraHome.home, FileService.MAIL_DIR).absoluteFile
					def fileAttach = new File("${destinationAttach}/" + attachment[i].getFilename())
					fileAttach.createNewFile()
					def outAttach = new FileOutputStream(fileAttach)
					def attachContent = attachment[i].getContents()
					outAttach.write(attachContent)                		
					def attachmentParamsAttach = new CreateAttachmentParamsBean.Builder("${destinationAttach}/" + attachment[i].getFilename() as File, attachment[i].getFilename(), '', user, issue).build()
					attachmentManager.createAttachment(attachmentParamsAttach)
					fileAttach.delete()
            }
		return
}
def issueObject = issueFactory.issue
issueObject.setProjectObject(project)
issueObject.setSummary(subject)
issueObject.setDescription(messageBody)
issueObject.setIssueTypeId(project.issueTypes.find { it.name == issueTypeName }.id)
issueObject.setReporter(reporter)
issue = messageHandlerContext.createIssue(user, issueObject)  as MutableIssue
// 
/* End of searcning or creating issue */
//


//
/* Set email to custom field */
//
def customField = ComponentAccessor.customFieldManager.customFieldObjects.findByName(customFieldName)
assert customField : "Could not find custom field with name ${customFieldName}"
// The new value to set
def newValue = (String)message.getFrom()
def String newValueRegExp
//regEx for serach email in format <email>
String regex = "(?<=\\<).+?(?=\\>)"
Pattern pattern = Pattern.compile(regex)
Matcher matcher = pattern.matcher(newValue)
if (matcher.find()) {
		newValueRegExp = newValue.substring(matcher.start(), matcher.end())
	}
	else
	{
		//regEx for serach email in format [email]
		regex = "(?<=\\[).+?(?=\\])"
		pattern = Pattern.compile(regex)
		matcher = pattern.matcher(newValue)
		if (matcher.find()) {
			newValueRegExp = newValue.substring(matcher.start(), matcher.end())
		}
		else
		{
			newValueRegExp = "Error email parsing!"
			commentManager.create(issue, user, newValue, true)
		}
	}
customField.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(customField), newValueRegExp), new DefaultIssueChangeHolder())	
// 
/* End of email custom field */
//

//
/* attach current email as Jira attach */
//
def destination = new File(jiraHome.home, FileService.MAIL_DIR).absoluteFile
def file = new File("${destination}/${temporaryAttachmentFileNameOfEml}")
file.createNewFile()
def out = new FileOutputStream(file)
def emailContent = createMessage(from, to, cc, subject, messageBody, mailHost, mailPort)
emailContent.writeTo(out)
def attachmentParams = new CreateAttachmentParamsBean.Builder("${destination}/${file.absoluteFile.name}" as File, actualAttachmentFileNameOfEml, '', user, issue).build()
attachmentManager.createAttachment(attachmentParams)
file.delete()

//
/* End of  attach current email as Jira attach */
//

//
/* attach email attachment as Jira attach */
//
def  MailUtils.Attachment[] attachment = MailUtils.getAttachments(message)
for (def i = 0; i < attachment.length; i++)
            {
				def destinationAttach = new File(jiraHome.home, FileService.MAIL_DIR).absoluteFile
				def fileAttach = new File("${destinationAttach}/" + attachment[i].getFilename())
				fileAttach.createNewFile()
				def outAttach = new FileOutputStream(fileAttach)
				def attachContent = attachment[i].getContents()
				outAttach.write(attachContent)                		
				def attachmentParamsAttach = new CreateAttachmentParamsBean.Builder("${destinationAttach}/" + attachment[i].getFilename() as File, attachment[i].getFilename(), '', user, issue).build()
				attachmentManager.createAttachment(attachmentParamsAttach)
				fileAttach.delete()
            }
//
/* End of  attach email attachment as Jira attach */
//
