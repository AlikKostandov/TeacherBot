import org.checkerframework.checker.units.qual.A;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Connection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Bot extends TelegramLongPollingBot {

    private static Connection connection;
    private static Statement stmt;
    private static PreparedStatement psInsert;


    private static String lastWord;

    public static void main(String[] args) {

        try {
            connect();
        } catch (Exception e) {
            e.printStackTrace();
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
            setButtons(sendMessage);
            execute(sendMessage);
        }catch (TelegramApiException e){
            e.printStackTrace();
        }
    }

    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            String inText = message.getText();
            switch (inText) {
                case "Привет":
                    sendMsg(message, "Привет! Нужна помощь с переводом? Да/Нет");
                    break;
                case "Да":
                    sendMsg(message,"Какое слово тебя интересует?");
                    break;
                case "Нет":
                    sendMsg(message, "Тогда увидимся позже");
                    break;
                case "Other":
                    sendMsgWithWords(message);
                    break;
                case "Clear":
                    try {
                        clearUserWords(message.getChatId());
                        sendMsg(message,"Словарь очищен");
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                    break;
                case "+":
                    try {
                        insertWord(message.getChatId(),lastWord);
                        sendMsg(message,"Слово у вас в списке");
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                    break;
                case "-":
                    try {
                        deleteUsersWord(message.getChatId(),lastWord);
                        sendMsg(message,"Слово удалено");
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                    break;
                default:
                    String answer = translator.getTranslate(inText.toLowerCase());
                            if (answer != null) {
                                lastWord = answer;
                                sendMsg(message, answer);
                                break;
                            }
                            else{
                                sendMsg(message, "Такого слова не существует или слово введено некорректно");
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

    public static void connect() throws ClassNotFoundException, SQLException {//соединение с БД
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:myDB.db");
        stmt = connection.createStatement();
    }

    private static void createWordMap()throws SQLException{ //создание таблицы
        stmt.executeUpdate("CREATE TABLE WordMap(id INT NOT NULL, Word VARCHAR(60) NOT NULL, PRIMARY KEY(id, Word))");
    }

    private static void clearUserWords(Long id)throws SQLException{//очистка таблицы
        stmt.executeUpdate(String.format("DELETE FROM WordMap WHERE id = ('%s');",id));
    }

    private static void insertWord(Long id, String word)throws SQLException{//добавление слова
        stmt.executeUpdate(String.format("INSERT INTO WordMap (id, Word) VALUES ('%s','%s');", id, word ));
    }

    private static void deleteUsersWord(Long id, String word) throws SQLException {//удаление слова
        stmt.executeUpdate(String.format("DELETE FROM WordMap WHERE id = ('%s') AND Word = ('%s');",id, word));
    }

    private static ArrayList<String> wordsList(Long id)throws SQLException{//выборка слов
        ArrayList<String> words = new ArrayList<>();
        ResultSet rs = stmt.executeQuery(String.format("SELECT Word FROM WordMap WHERE id = '%s';",id));
        while(rs.next()){
            words.add(rs.getString("Word"));
        }
        return words;
    }

    private void sendMsgWithWords(Message message){//сортировка слов и разделение на десятки
        String msg = "";
        try {
            ArrayList<String> userWords = wordsList(message.getChatId());
            if(!userWords.isEmpty()){
            for(int i = 0; i <= userWords.size() / 10; i++){
                if(userWords.size() > 10 * (i + 1)){
                    msg = userWords.subList(10 * i, 10 * (i + 1)).stream().collect(Collectors.joining("\n"));
                }else {
                    msg = userWords.subList(10 * i, userWords.size()).stream().collect(Collectors.joining("\n"));
                }
                sendMsg(message,msg);
            }}else {
                sendMsg(message,"Ваш словарь пуст");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public  void setButtons(SendMessage sendMessage){ //специальная клавиатура со специальными командами
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboardRowList = new ArrayList<>();
        KeyboardRow keyboardFistRow = new KeyboardRow();

        keyboardFistRow.add(new KeyboardButton("Other"));
        keyboardFistRow.add(new KeyboardButton("+"));
        keyboardFistRow.add(new KeyboardButton("-"));
        keyboardFistRow.add(new KeyboardButton("Clear"));


        keyboardRowList.add(keyboardFistRow);
        replyKeyboardMarkup.setKeyboard(keyboardRowList);
    }


    public static void disconnect() { //разрыв соединения с БД
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