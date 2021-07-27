import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;

public class Parser {


    public static final String HTTPS_WOOORDHUNT_RU_DIC_LIST_EN_RU = "https://wooordhunt.ru/dic/list/en_ru/";
    public static final String WORD = "/word/";

    public String getTranslate(String key){
        try {
            String translate = getWordsTranslate(getPage(getUrl(key)),key);
            return translate;
        }catch (IOException e){
            e.printStackTrace();
            return null;}
    }

    public static String getWordsTranslate(Document page, String key) throws IOException{
        Elements fields = page.select("p");     //получение перевода слова из страницы
        String value = null;
        String open = WORD+key;
        for (Element field:fields) {
            if(field.select("a").attr("href").toLowerCase().equals(open)){
                System.out.println(field.select("a").attr("href"));
                value = field.text();
            }
        }
        return value;
    }

    public static Document getPage(String url) throws IOException{ // получение страницы из сслыки
        Document page = Jsoup.parse(new URL(url),5000);
        return page;
    }

    public static String getUrl(String word){ //получение ссылки с аомощью двух букв слова
        String keySymbols = word.substring(0,2).toLowerCase();
        String Url = HTTPS_WOOORDHUNT_RU_DIC_LIST_EN_RU + keySymbols;
        return Url;
    }


}
