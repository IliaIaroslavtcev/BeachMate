# Topic Reply Support Changes

## Проблема
Бот отвечал в general чат вместо того же топика, где был отправлен запрос пользователя.

## Решение
Добавлена поддержка ответа в том же топике через `setReplyToMessageId()` во всех обработчиках команд.

## Изменения

### Основные обработчики команд:
1. **BeachNameHandler** - основной обработчик поиска пляжей
   - Добавлен `setReplyToMessageId()` для основного ответа
   - Обновлены методы `sendSearchingNotification()`, `sendTypoCorrectionMessage()`, `sendLocationMessage()`

2. **StartCommandHandler** - команда /start
   - Добавлен `setReplyToMessageId()` для ответа приветствия

3. **HelpCommandHandler** - команда /help
   - Добавлен `setReplyToMessageId()` для ответа справки

4. **LanguageCommandHandler** - команда /language
   - Добавлен `setReplyToMessageId()` для выбора языка

5. **TopicsCommandHandler** - команда /topics
   - Добавлен `setReplyToMessageId()` для конфигурации топиков
   - Обновлены методы `sendPrivateChatMessage()`, `sendNotAdminMessage()`

6. **UnknownCommandHandler** - неизвестные команды
   - Добавлен `setReplyToMessageId()` для сообщения об ошибке

## Техническая реализация
Во всех обработчиках добавлен следующий паттерн:

```java
// Reply to the original message to keep the response in the same topic
if (update.getMessage() != null) {
    message.setReplyToMessageId(update.getMessage().getMessageId());
}
```

## Результат
- Все ответы бота теперь появляются в том же топике, где был задан вопрос
- Сохраняется контекст разговора в группах с топиками
- Улучшена организация общения в группах

## Совместимость
- Работает с текущей версией Telegram Bot API (6.9.7.1)
- Обратная совместимость с обычными чатами без топиков
- Поддержка как команд, так и callback запросов