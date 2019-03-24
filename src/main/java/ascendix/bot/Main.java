package ascendix.bot;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

public class Main {
    public static void main(String[] args) {

        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();

        try {
            AscendixBot bot = new AscendixBot();
            bot.init();
            telegramBotsApi.registerBot(bot);
        } catch (TelegramApiRequestException e) {
            System.out.println(e.getMessage());
        }

    }
}
