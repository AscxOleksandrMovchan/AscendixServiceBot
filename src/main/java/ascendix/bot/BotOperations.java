package ascendix.bot;

import ascendix.data.Plate;
import ascendix.data.Step;
import ascendix.sheets.SheetsController;
import ascendix.storage.Context;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

public class BotOperations {
    private TelegramLongPollingBot bot;
    private List<List<Object>> menu;
    private char commandSeparator = '★';
    private Stack<String> backingCommands = new Stack<String>();
    private SheetsController sheets = new SheetsController();

    public BotOperations(TelegramLongPollingBot bot) {
        this.bot = bot;
        fillMenu();
    }

    private void fillMenu() {
        String id = "1l-9BqVckWlAKjgYGZxj8W2KjsLnUBwnrEGodnu7FCo8";
        String range = "!A2:D162";
        menu = sheets.readSheets(id, range);
    }

    public boolean operateCallbackUpdate(CallbackQuery query, Context context) {
        String messageText = query.getData();
        String chatId = query.getMessage().getChatId().toString();

        if (messageText.equals("/submit")){
            String username = query.getFrom().getFirstName() + " " + query.getFrom().getLastName();
            submitOrder(username, context);
            return true;
        }

        if (messageText.equals("/mailTo")){
            submitMailto(chatId);
            return true;
        }

        if (messageText.equals("/lunch")) {
            createButtonsForLunch(chatId);
            return true;
        }

        if (messageText.contains("/selectCategory")){
            createButtonsForCategory(chatId, messageText);
            return true;
        }

        if (messageText.contains("/selectDishes")) {
            createMessagesForDishes(chatId, messageText);
            return true;
        }

        if (messageText.contains("/addToBucket")) {
            addToBucket(messageText, context);
            return true;
        }

        if (messageText.contains("/goToBucket")) {
            createBucketMessage(chatId, context);
            return true;
        }

        return false;
    }

    public void respondToUser(String chatId, Context context, boolean isValid) {
        if (isValid) {
            context.reinit();

            sendStartMessage(chatId, context);
        } else {
            sendSimpleMessage(chatId, "Invalid request. Try again");
        }
    }

    private synchronized void sendSimpleMessage(String chatId, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);

        try {
            bot.sendMessage(sendMessage); //TODO replace
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }

    public synchronized void sendStartMessage(String chatId, Context context) {
        String message = "Chose what to do?";
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);

        List<List<InlineKeyboardButton>> buttonRows = new ArrayList<List<InlineKeyboardButton>>();

        for (Step step :  context.getCurrentStep().getSteps()) {
            List<InlineKeyboardButton> row = new ArrayList<InlineKeyboardButton>();
            row.add(new InlineKeyboardButton().setText(step.getName()).setCallbackData(step.getCommand()));
            buttonRows.add(row);
        }

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(buttonRows);

        sendMessage.setReplyMarkup(keyboardMarkup);

        try {
            bot.sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }

    private synchronized void submitOrder(String userName, Context context) {
        List<Plate> dishes = context.getBucket();
        if (dishes.isEmpty()) return;

        StringBuilder builder = new StringBuilder();
        builder.append(userName).append(" ordered next dishes: \n");
        buildBucketOrder(builder, dishes);
        String message = builder.toString();

        SendMessage channelMessage = new SendMessage();
        channelMessage.enableMarkdown(true);
        channelMessage.setChatId("@ascendix_food_chat");
        channelMessage.setText(message);

        try {
            bot.sendMessage(channelMessage);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }

    private void createButtonsForLunch(String chatId) {
        backingCommands.push("/start");

        String message = "Chose your food provider?";
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);

        List<List<InlineKeyboardButton>> buttonRows = new ArrayList<List<InlineKeyboardButton>>();

        HashSet<String> providers = new HashSet<String>();

        for (List<Object> orderRow : menu) {
            providers.add(orderRow.get(0).toString());
        }

        for (String provider : providers) {
            List<InlineKeyboardButton> row = new ArrayList<InlineKeyboardButton>();
            row.add(new InlineKeyboardButton().setText(provider).setCallbackData("/selectCategory" + commandSeparator + provider));
            buttonRows.add(row);
        }

        addStandardButtons(buttonRows);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(buttonRows);

        sendMessage.setReplyMarkup(keyboardMarkup);

        try {
            bot.sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }

    private void createButtonsForCategory(String chatId, String request) {
        String[] parsedRequest = request.split(String.valueOf(commandSeparator));

        String message = "Chose food category in " + parsedRequest[1] ;
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);

        List<List<InlineKeyboardButton>> buttonRows = new ArrayList<List<InlineKeyboardButton>>();

        HashSet<String> categories = new HashSet<String>();

        for (List<Object> orderRow : menu) {
            if (orderRow.get(0).equals(parsedRequest[1]))
                categories.add(orderRow.get(1).toString());
        }

        for (String category : categories) {
            List<InlineKeyboardButton> row = new ArrayList<InlineKeyboardButton>();
            row.add(new InlineKeyboardButton().setText(category).setCallbackData("/selectDishes" + commandSeparator + parsedRequest[1] + commandSeparator + category));
            buttonRows.add(row);
        }

        backingCommands.push("/lunch");
        addStandardButtons(buttonRows);
        backingCommands.push(request);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(buttonRows);

        sendMessage.setReplyMarkup(keyboardMarkup);

        try {
            bot.sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }

    private void createMessagesForDishes(String chatId, String request) {
        String[] parsedRequest = request.split(String.valueOf(commandSeparator));

        String message = "Chose your plates";
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        try {
            bot.sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }

        HashSet<List<Object>> plates = new HashSet<List<Object>>();

        for (List<Object> orderRow : menu) {
            if (orderRow.get(0).equals(parsedRequest[1]) && orderRow.get(1).equals(parsedRequest[2]))
                plates.add(orderRow);
        }

        for (List<Object> orderRow : plates) {
            String dishName = orderRow.get(2) + "\n" + orderRow.get(3) + " грн.";
            SendMessage dishMessage = new SendMessage();
            dishMessage.enableMarkdown(true);
            dishMessage.setChatId(chatId);
            dishMessage.setText(dishName);

            int index = findIndexOfChosen(orderRow);
            if (index == -1) continue;

            List<List<InlineKeyboardButton>> buttonRow = new ArrayList<List<InlineKeyboardButton>>();
            List<InlineKeyboardButton> row = new ArrayList<InlineKeyboardButton>();
            row.add(new InlineKeyboardButton().setText("Add to bucket").setCallbackData("/addToBucket" + commandSeparator + index));
            buttonRow.add(row);

            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            keyboardMarkup.setKeyboard(buttonRow);

            dishMessage.setReplyMarkup(keyboardMarkup);

            try {
                bot.sendMessage(dishMessage);
            } catch (TelegramApiException e) {
                System.out.println(e.getMessage());
            }
        }
        List<List<InlineKeyboardButton>> buttonRows = new ArrayList<List<InlineKeyboardButton>>();
        addStandardButtons(buttonRows);
        backingCommands.push(request);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(buttonRows);

        String msg = "Or you don't want anything?";
        SendMessage finalMsg = new SendMessage();
        finalMsg.enableMarkdown(true);
        finalMsg.setChatId(chatId);
        finalMsg.setReplyMarkup(keyboardMarkup);
        finalMsg.setText(msg);

        try {
            bot.sendMessage(finalMsg);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }

    private void addToBucket(String request, Context context) {
        String[] parsedRequest = request.split(String.valueOf(commandSeparator));

        List<Object> plateRow = menu.get(Integer.parseInt(parsedRequest[1]));
        double price = Double.parseDouble(plateRow.get(3).toString().replace(",", "."));
        context.getBucket().add(new Plate(plateRow.get(0).toString(), plateRow.get(2).toString(), price));
    }

    private void createBucketMessage(String chatId, Context context) {
        List<Plate> dishes = context.getBucket();
        if (dishes.isEmpty()) return;

        StringBuilder builder = new StringBuilder();
        builder.append("You have ordered next dishes: \n");
        buildBucketOrder(builder, dishes);
        String message = builder.toString();

        SendMessage confirmMessage = new SendMessage();
        confirmMessage.enableMarkdown(true);
        confirmMessage.setChatId(chatId);
        confirmMessage.setText(message);

        List<List<InlineKeyboardButton>> buttonRow = new ArrayList<List<InlineKeyboardButton>>();
        List<InlineKeyboardButton> row = new ArrayList<InlineKeyboardButton>();
        row.add(new InlineKeyboardButton().setText("Confirm").setCallbackData("/submit"));
        row.add(new InlineKeyboardButton().setText("Cancel").setCallbackData("/lunch"));
        buttonRow.add(row);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(buttonRow);

        confirmMessage.setReplyMarkup(keyboardMarkup);

        try {
            bot.sendMessage(confirmMessage);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }

    private void buildBucketOrder(StringBuilder builder, List<Plate> dishes) {
        double totalPrice = 0;

        for(int i = 0; i < dishes.size(); i++) {
            if (i < dishes.size() - 1)
                builder.append(dishes.get(i).getProvider())
                        .append(": ")
                        .append(dishes.get(i).getName())
                        .append(", ")
                        .append(dishes.get(i).getPrice())
                        .append("грн.,\n");
            else
                builder.append(dishes.get(i).getProvider())
                        .append(": ")
                        .append(dishes.get(i).getName())
                        .append(", ")
                        .append(dishes.get(i).getPrice())
                        .append(" грн.\n");

            totalPrice += dishes.get(i).getPrice();
        }

        builder.append("Total price: ")
                .append(totalPrice)
                .append("грн.");
    }

    private int findIndexOfChosen(List<Object> orderRow) {
        for (int i = 0; i < menu.size(); i++)
            if (menu.get(i).equals(orderRow))
                return i;

        return -1;
    }

    private void addStandardButtons(List<List<InlineKeyboardButton>> buttonRow) {
        List<InlineKeyboardButton> row = new ArrayList<InlineKeyboardButton>();
        row.add(new InlineKeyboardButton().setText("Go to bucket").setCallbackData("/goToBucket"));
        row.add(new InlineKeyboardButton().setText("Back").setCallbackData(backingCommands.pop()));
        buttonRow.add(row);
    }

    private synchronized void submitMailto(String chatId) {
        String message = "Chose what to do?";
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);

        List<List<InlineKeyboardButton>> buttonRows = new ArrayList<List<InlineKeyboardButton>>();

        List<InlineKeyboardButton> row = new ArrayList<InlineKeyboardButton>();
        row.add(new InlineKeyboardButton().setText("Send").setSwitchInlineQuery("Not implemented yet \uD83D\uDE2D"));
        buttonRows.add(row);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(buttonRows);

        sendMessage.setReplyMarkup(keyboardMarkup);

        try {
            bot.sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }
}
