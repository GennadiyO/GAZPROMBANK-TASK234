package com.gazprombank.task4;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Main {
    public static void main(String[] args) {
        System.out.println("Please, input first date in format dd/MM/yyyy");
        Scanner sc = new Scanner(System.in);
        String frstDate = sc.nextLine();
        System.out.println("Please, input second date in format dd/MM/yyyy");
        String scndDate = sc.nextLine();
        System.out.println("The list of avialable curency is: AUD, GBP, BYR, DKK, USD, EUR, ISK, KZT, CAD, NOK, XDR, SGD, TRL, UAH, SEK, CHF, JPY");
        System.out.println("Please, input required currencys in format 'USD, EUR,...'");
        String currencyStr = sc.nextLine();
        System.out.println("Please, input additional currencys in format 'USD/EUR, AUD/USD,...'");
        String currencyAddStr = sc.nextLine();
        System.out.println("Loading... (it may takes up to few minutes)");
        sc.close();
        List<String> currencyArr = Arrays.asList(currencyStr.split("\\s*,\\s*"));
        List<String> currencyAddArr = Arrays.asList(currencyAddStr.split("\\s*,\\s*"));
        List<AddValute> currencyAddList = new ArrayList<>();
        for (String curAddArr: currencyAddArr){
            currencyAddList.add(new AddValute(curAddArr));
        }
        int daysBetween;
        List<Valute> valute;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        String urlFrstPart = "http://www.cbr.ru/scripts/XML_daily.asp?date_req=";
        Calendar cal = Calendar.getInstance();
        try {
            Date date1 = sdf.parse(frstDate);
            cal.setTime(date1);
            Date date2 = sdf.parse(scndDate);
            cal.add(Calendar.DATE, -1);
            daysBetween = (int)Math.round((date2.getTime() - date1.getTime()) / (double)86400000);
            for (int day = 0; day < (daysBetween + 1); day++) {
                cal.add(Calendar.DATE, 1);
                date1 = cal.getTime();
                URL url = new URL(urlFrstPart + sdf.format(date1));
                valute = getValutes(url);
                printToFile(valute, currencyArr, currencyAddList, date1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Valute> getValutes(URL url) throws IOException, ParserConfigurationException, SAXException {

        List<Valute> valutes = new ArrayList<>();
        String id;
        int numCode;
        String charCode;
        int nominal;
        String name;
        String value;
        URLConnection connection = url.openConnection();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(connection.getInputStream());
        doc.getDocumentElement().normalize();
        NodeList nList = doc.getElementsByTagName("Valute");
        for (int node = 0; node < nList.getLength(); node++) {
            Node nNode = nList.item(node);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element)nNode;
                id = eElement.getAttribute("ID");
                numCode = Integer.valueOf(eElement.getElementsByTagName("NumCode").item(0).getTextContent());
                charCode = eElement.getElementsByTagName("CharCode").item(0).getTextContent();
                nominal = Integer.valueOf(eElement.getElementsByTagName("Nominal").item(0).getTextContent());
                name = eElement.getElementsByTagName("Name").item(0).getTextContent();
                value = eElement.getElementsByTagName("Value").item(0).getTextContent();
                valutes.add(new Valute(id, numCode, charCode, nominal, name, value));
            }
        }
        return valutes;
    }

    public static void printToFile(List<Valute> valute, List<String> currencyArr, List<AddValute> currencyAddList, Date date) throws ParseException {
        DecimalFormat df = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.US));
        StringBuilder uprLine = new StringBuilder();
        StringBuilder line = new StringBuilder();
        uprLine.append("Date                          |  ");
        line.append(date + " | ");
        List<Valute> uprValuteBuffer = new ArrayList<>();
        List<Valute> lwrValuteBuffer = new ArrayList<>();
        for (Valute val : valute) {
            for (String currency : currencyArr) {
                if (currency.equals(val.getCharCode())) {
                    line.append(val.getValue() + " | ");
                }
            }
            for (AddValute currencyAdd: currencyAddList){
                if (currencyAdd.getUprValute().equals(val.getCharCode())){
                    uprValuteBuffer.add(val);
                }
                if (currencyAdd.getLwrValute().equals(val.getCharCode())){
                    lwrValuteBuffer.add(val);
                }
            }
        }
        for (int i = 0; i<currencyAddList.size(); i++){
            String uprBuffer = uprValuteBuffer.get(i).getValue();
            String lwrBuffer = lwrValuteBuffer.get(i).getValue();
            Double val1 = df.parse(uprBuffer).doubleValue()/df.parse(lwrBuffer).doubleValue();
            line.append(val1+" | ");
        }
        if (!Files.exists(Paths.get("valutes.txt"))) {
            try {
                Files.createFile(Paths.get("valutes.txt"));
                Path valutesFile = Paths.get("valutes.txt");
                for (Valute val : valute) {
                    for (String currency : currencyArr) {
                        if (currency.equals(val.getCharCode())) {
                            uprLine.append(val.getCharCode() + "   |   ");
                        }
                    }
                }
                for (AddValute val: currencyAddList){
                    uprLine.append(val.getUprValute()+"/"+val.getLwrValute()+" | ");
                }
                Files.write(valutesFile, (uprLine.toString() + "\r\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Path valutesFile = Paths.get("valutes.txt");
        try {
            Files.write(valutesFile, (line.toString() + "\r\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
