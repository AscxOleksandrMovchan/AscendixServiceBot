package ascendix.bot;

import ascendix.data.Step;
import ascendix.storage.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.io.File;
import java.util.HashMap;

public class AscendixBot extends TelegramLongPollingBot {

    private HashMap<String, Context> sessions = new HashMap<String, Context>();
    private BotOperations operations;

    public void init() {
        operations = new BotOperations(this);
    }

    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()){
            CallbackQuery query = update.getCallbackQuery();
            String messageText = query.getData();
            String chatId = query.getMessage().getChatId().toString();

            Context context = sessions.get(chatId);
            if (context == null) return;
            if (operations.operateCallbackUpdate(query, context)) return;

            context.invokeCommand(messageText);
            operations.sendStartMessage(chatId, context);
        } else {
            Message message = update.getMessage();
            String messageText = message.getText();
            long chatId = message.getChatId();
            if (!sessions.containsKey(String.valueOf(chatId)))
                createContextForUser(chatId);

            operations.respondToUser(String.valueOf(chatId), sessions.get(String.valueOf(chatId)), messageText.equals("/start"));
        }
    }

    private void createContextForUser(long chatId) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            ClassLoader cLoader = getClass().getClassLoader();
            File file = new File(cLoader.getResource("command.json").getFile());
            Step step = mapper.readValue(file, Step.class);

            sessions.put(String.valueOf(chatId), new Context(step));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public String getBotUsername() {
        return "acx_service_bot";
    }

    public String getBotToken() {
        return "883028548:AAGSuGX3CYe-kUga6BAsGuN-i1G8OZBPHyQ";
    }
}



