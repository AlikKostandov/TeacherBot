import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.util.Locale;
import java.sql.*;

public class Bot extends TelegramLongPollingBot {

    private static Connection connection;
    private static Statement stmt;
    private static PreparedStatement psInsert;

    public static void main(String[] args) {

        try {
            connect();
            insertEx();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }

        ApiContextInitializer.init();
        TelegramBotsApi botsApi = new TelegramBotsApi();
        try{
            botsApi.registerBot(new Bot());
        } catch (TelegramApiException e){
            e.printStackTrace();
        }

    }

    Parser translator = new Parser();

    public void sendMsg(Message message, String text){
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(text);
        try{
            execute(sendMessage);
        }catch (TelegramApiException e){
            e.printStackTrace();
        }
    }

    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()){
            String msgTxt;
            String inText = message.getText();
                switch (inText) {
                    case "Привет":
                        sendMsg(message, "Привет! Нужна помощь с переводом?");
                        break;
                    case "Да":
                        sendMsg(message, "Какое слово тебя интересует?");
                        break;
                    default:
                        String answer = translator.getTranslate(inText.toLowerCase());
                        if (answer != null) {
                            sendMsg(message, answer);
                            break;
                        }
                        else{
                        sendMsg(message, "Такого слова не существует или слово введенно некорректно");
                        break;
                        }
                }
            }
        }


    @Override
    public String getBotUsername() {
        return "YourEnglandBot";
    }

    @Override
    public String getBotToken() {
        return "1774622879:AAGyZEgHnl5DojIEAs5ZxGs-Xt9OhfkEvxU";
    }

    public static void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:myDB.db");
        stmt = connection.createStatement();
        // DatabaseMetaData dmd = connection.getMetaData();
    }

    private static void clearTableEx() throws SQLException {
        stmt.executeUpdate("DELETE FROM users;");
    }

    private static void deleteEx() throws SQLException {
        System.out.println(stmt.executeUpdate("DELETE FROM users WHERE name = 'Alik';"));
    }

    private static void insertEx() throws SQLException {
        System.out.println(stmt.executeUpdate("INSERT INTO users (name) VALUES ('Alik');"));
    }


    public static void disconnect() {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (psInsert != null) {
                psInsert.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }}