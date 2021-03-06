import groovy.time.TimeCategory

def date = new Date(); // Текущая дата (+ время)
def serviceTime = utils.get('servicetime$101450623'); // Элемент справочника "Классы обслуживания"
def timeZone = utils.get('timezone$131801'); // Элемент справочника "Часовые пояса"

/*
* Следующая дата/время напоминания
*
*/
def nextDateTimeReminder =
{
	currentDateTime, nearestDateTimeService_ ->
	
  	def period = nearestDateTimeService_ - currentDateTime;
  
	use(TimeCategory)
	{
		return period < 2 ? (currentDateTime + 1.day - 1.minute) : (currentDateTime + period.day - 1.minute);
	}
}

/*
* Ближайшая дата/время обслуживания
*
*/
def nearestDateTimeService = 
{
	currentDateTime ->
		
	return api.timing.serviceStartTime(currentDateTime, serviceTime, timeZone); 
}

/*
* Текущий день является днем обслуживания?
*
*/
def currentDayIsService = 
{
	currentDateTime ->
	
	def currentDate = utils.formatters.strToDate(utils.formatters.formatDate(currentDateTime));
	def nearestDateTimeService_ = nearestDateTimeService(currentDate);
	def period = nearestDateTimeService_ - currentDate;
	
	return period == 0;
}

def newDate = nextDateTimeReminder(date, nearestDateTimeService(date));
utils.edit(subject, ['dateReminder' : newDate]); 

/*
* Переменные для формирования оповещения
* 
*/
def initiator = '';
def currentPerson = '';
def newPerson = '';

/*
* Метод добавления сотрудника в получатели оповещения
* 
*/
def addToEmployeeToNotification =
{
	employee ->
	
	if (api.mail.isValidEmail(employee.email))
    {
        notification.toEmployee << employee;
        logger.info("Оповещение о голосовании: ${employee.title}")
  	}
    else 
    {
        logger.info("Сотруднику '${employee.title}' оповещение не отправлено: некорректный адрес электронной почты \"${employee.email}\".")
    }
}

/*
* Метод добавления сотрудника в копию оповещения
* 
*/
def addCcEmployeeToNotification =
{
	employee ->
	
	if (api.mail.isValidEmail(employee.email))
    {
        notification.ccEmployee << employee;
        logger.info("Оповещение о голосовании: ${employee.title}")
  	}
    else 
    {
        logger.info("Сотруднику '${employee.title}' оповещение не отправлено: некорректный адрес электронной почты \"${employee.email}\".")
    }
}

def process =
{
	def employee = subject?.voter_em; // Получатель (соглсовант) оповещения
	def accountant = utils.get('employee$33881292'); // Бухгалтер KOVALENKO Olga_F Fedori employee$104032
	def otherEmployees = [employee.head, subject.source.clientEmployee, accountant]; // Другие получатели (руководитель согласованта, инициатор запроса, бухгалтер) оповещения
	
	addToEmployeeToNotification(employee); // Добавление согласованта в оповещение
	
	if (subject.countReminders >= 3) // Проверка на количество напоминаний-оповещений 
	{
		for (def i = 0; i < otherEmployees.size(); i++)
		{
			addCcEmployeeToNotification(otherEmployees[i]); // Добавление других получателей в оповещение
		}
	}
	
	utils.edit(subject, ['countReminders' : subject.countReminders + 1]);
	
	initiator = subject.source.clientEmployee != null ? subject.source.clientEmployee?.title : '';
	currentPerson = subject.source.currentPerson != null ? (subject.source.currentPerson.title + ' (' + subject.source.currentPerson.privateCode + ')') : '';
	newPerson = subject.source.newPerson != null ? (subject.source.newPerson.title + ' (' + subject.source.newPerson.privateCode + ')') : '';
}

if (currentDayIsService(date))
{
	process();	
}

/*
* Добавление переменных в оповещение
* 
*/

notification.scriptParams['initiator'] = initiator;
notification.scriptParams['currentPerson'] = currentPerson;
notification.scriptParams['newPerson'] = newPerson;