package transista;

import com.gtranslate.Translator;
import static javafx.application.Application.launch;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import static java.lang.Thread.sleep;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Locale;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Lighting;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.JSONArray;

public class TransISTA extends Application {
    
    static int entries = 0;
    static int twords = 0;
    static int words = 0;
    static int dead = 0;
    static int done = 0; 
    static int max = 42*1024;
    static int cap = 20;
    
    static Connection con;
    static Connection don;
    static String file;
    static String nick;
    static String game;
    static Stage stage;
    static Label label;
//    static Translator translate;
    static Rectangle2D screen;
    static boolean nocard = true;
    static Group root = new Group();
    static Bloom bloom = new Bloom();
    static TextFlow tfR = new TextFlow();
    static TextFlow tfL = new TextFlow();
    static Lighting light = new Lighting();    
    static ArrayList<Thread> th = new ArrayList();
    static DropShadow shadow = new DropShadow();
    static ArrayList<Long> id = new ArrayList();
    static ArrayList<Long> xmlI = new ArrayList();
    static ArrayList<Long> ftsI = new ArrayList();
    static ArrayList<Long> xmlD = new ArrayList();
    static ArrayList<Long> ftsD = new ArrayList();
    static ArrayList<String> data = new ArrayList();
    
    public static void main(String[] args) throws Exception {
        if (args.length != 0 && !args[0].isEmpty())
            max = Integer.parseInt(args[1]);
            cap = Integer.parseInt(args[0]);
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Locale.setDefault(new Locale("el", "GR"));
        System.setProperty("file.encoding", "UTF-8");
        screen = Screen.getPrimary().getVisualBounds();
        stage.setTitle("Μεταφραστής");
        this.stage = stage;
        
//        translate = Translator.getInstance();        
        
        Class.forName("org.sqlite.JDBC");
        con = DriverManager.getConnection("jdbc:sqlite:C:/dbTrans/xmlvalueprimitive_ENGB.sqlite");
        don = DriverManager.getConnection("jdbc:sqlite:C:/dbTrans/translated.sqlite");
        
        don.createStatement().execute("CREATE TABLE IF NOT EXISTS fts (id LONG)");
        don.createStatement().execute("CREATE TABLE IF NOT EXISTS xmlvalueprimitive (id LONG)");
              
        Scene scene = new Scene(root);
        label = new Label("Έναρξη μετάφρασης...");
        label.setFont(Font.font("Calibri", FontWeight.BOLD, 32));
        label.setTextFill(Color.web("#65709d"));
        label.setTextAlignment(TextAlignment.CENTER);
        
        scene.setFill(Color.TRANSPARENT);
        tfL.setTextAlignment(TextAlignment.CENTER);
        tfR.setTextAlignment(TextAlignment.CENTER);
        tfL.setStyle("-fx-background-color: transparent;");
        tfR.setStyle("-fx-background-color: transparent;");
        root.setStyle("-fx-background-color: transparent;");
        label.setStyle("-fx-background-color: transparent;"
                       + "-fx-padding: 20 20 10 20;");
        
        bloom.setInput(light);
        shadow.setSpread(0.5);
        shadow.setHeight(20);
        shadow.setWidth(20);
        shadow.setInput(bloom);
        label.setEffect(shadow);
        
        BorderPane bPane = new BorderPane();
        bPane.setLeft(tfL);
        bPane.setRight(tfR);
        bPane.setTop(label);
        bPane.setAlignment(label,Pos.CENTER);
        
        root.getChildren().add(bPane);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setOnCloseRequest((WindowEvent t) -> {
            Platform.exit();
            System.exit(0); });
        stage.setScene(scene);
        stage.setAlwaysOnTop(false);
        stage.show();
        
//        Thread blink = new Thread() {
//            @Override public void run() {
//                double b = 1; while (true) {
//                    try { ((Lighting)((Bloom)shadow.getInput())
//                            .getInput()).setDiffuseConstant(1.5 + Math.sin(b)/2);
//                        b += 0.06; sleep(40);
//                    } catch (InterruptedException ex) { ex.printStackTrace(); } } }};
//        blink.setPriority(Thread.MIN_PRIORITY);
//        blink.start();

        Statement stmt = con.createStatement();
        String sql = "SELECT * FROM xmlvalueprimitive";
        ResultSet res = stmt.executeQuery(sql);
        while(res.next())
            xmlI.add(res.getLong("id"));
        stmt.close();

        stmt = con.createStatement();
        sql = "SELECT * FROM fts";
        res = stmt.executeQuery(sql);
        while(res.next())
            ftsI.add(res.getLong("id"));
        stmt.close();
        
        sql = "SELECT * FROM xmlvalueprimitive";
        stmt = don.createStatement();
        res = stmt.executeQuery(sql);
        while(res.next())
            xmlD.add(res.getLong("id"));
        stmt.close();
        
        sql = "SELECT * FROM fts";
        stmt = don.createStatement();
        res = stmt.executeQuery(sql);
        while(res.next())
            ftsD.add(res.getLong("id"));
        stmt.close();
        
        Thread entryCount = new Thread() {
            @Override public void run() { try {
                    sleep(5000);
                    done += Integer.parseInt(don.createStatement().executeQuery(
                            "SELECT COUNT(id) FROM xmlvalueprimitive").getString(1));
                    done += Integer.parseInt(don.createStatement().executeQuery(
                            "SELECT COUNT(id) FROM fts").getString(1));
                    entries += Integer.parseInt(con.createStatement().executeQuery(
                            "SELECT COUNT(id) FROM xmlvalueprimitive").getString(1));
                    entries += Integer.parseInt(con.createStatement().executeQuery(
                            "SELECT COUNT(id) FROM fts").getString(1));
                } catch (Exception ex) { ex.printStackTrace(); }}};
        
        Thread statusDis = new Thread() {
            @Override public void run() { try {
                sleep(3000);
                while (true) {
                    int aliveThreads;
                    synchronized (th) {
                        aliveThreads = 0;
                        for (Thread tha : th)
                            if (tha.isAlive())
                                aliveThreads++;
                    } int at = aliveThreads;
                    
                    Platform.runLater(()->{
                        if (entries == 0)
                            label.setText("Νήματα: " + at + "/" + dead +
                                "      Λέξεις: " + words + "/" + twords +
                                "      Υπολογισμός κατάστασης...");
                        else label.setText("Νήματα: " + at + "/" + dead +
                                "      Λέξεις: " + words + " σε " + twords +
                                "      Κατάσταση: " + done + "/" + entries +
                                "      (" + ((float)Math.round((float)done
                                        /entries*10000)/100) + "%) "); });
                    sleep(500); }}
            catch (InterruptedException ex) { ex.printStackTrace(); }}};

        Thread threadLoader = new Thread() {
            @Override public void run() { try {
                for (int i = 0; i < max/2; i++) {
                    synchronized (th) {
                        th.add(createThread("xmlvalueprimitive",i,max)); }
                    threadLimit(); }
                for (int i = 0; i < max/2; i++) {
                    synchronized (th) {
                        th.add(createThread("fts",i,max)); }
                    threadLimit(); }
                System.out.println("All threads started!"); }
            catch (InterruptedException ex) { ex.printStackTrace(); }}};
        
        Thread bgTask = new Thread() {
            @Override public void run() { try {                
                while (true) if (dead != max) sleep(60000);
            } catch (Exception ex) { ex.printStackTrace(); }
            Platform.runLater(()->{ System.exit(0); }); }};
        
        tfR.setMinHeight((screen.getHeight()*.3));
        tfL.setMinHeight((screen.getHeight()*.3));
        tfL.setMinWidth((screen.getWidth()*.3));
        tfR.setMinWidth((screen.getWidth()*.3));
        tfR.setMaxHeight((screen.getHeight()*.3));
        tfL.setMaxHeight((screen.getHeight()*.3));
        tfL.setMaxWidth((screen.getWidth()*.3));
        tfR.setMaxWidth((screen.getWidth()*.3));
        stage.setMinWidth((screen.getWidth()*.8));
        stage.setMinHeight((screen.getHeight()*.8));
        stage.setMaxWidth((screen.getWidth()*.8));
        stage.setMaxHeight((screen.getHeight()*.8));
        stage.setY((screen.getHeight()*.2));
        stage.setX((screen.getWidth()*.2));
        
        threadLoader.setPriority(Thread.MIN_PRIORITY);
        entryCount.setPriority(Thread.MIN_PRIORITY);
        statusDis.setPriority(Thread.MIN_PRIORITY);
        bgTask.setPriority(Thread.MIN_PRIORITY);        
        entryCount.start();
        threadLoader.start();
        statusDis.start();
        bgTask.start();
    }
    
    void threadLimit() throws InterruptedException {
        int at = 0;
        for (Thread tha : th)
            if (tha.isAlive()) at++;
        sleep(5);
        if (at > cap) sleep(2*60000);
        if (at > (2*cap)) sleep(2*60000);
        if (at > (3*128)) sleep(2*60000);
        if (at > (4*128)) sleep(2*60000);
        if (at > (5*128)) sleep(2*60000);
    }
    
    static Document treeWalk(Document document) throws InterruptedException, Exception {
        Element element = treeWalk(document.getRootElement());
        document.remove(document.getRootElement());
        document.add(element);
        return document;
    }
    
    Thread createThread(String db, int instance, int max) {
        Thread th = new Thread() {
            @Override public void run() { try {
                
                int index = 0;
                SAXReader reader = new SAXReader();
                ArrayList<Long> dbD = new ArrayList();
                ArrayList<Long> dbI = new ArrayList();
                
                if (db.equals("fts")) {
                    dbD = ftsD;
                    dbI = ftsI;
                }
                else {
                    dbD = xmlD;
                    dbI = ftsI;
                }
                
                while(index < dbI.size()) {
                    if ((index % max) == instance) {
                        Long id = dbI.get(index);
                        if (!dbD.contains(id)) {
                            String sql = "SELECT * FROM " + db + " WHERE id IS ?";
                            PreparedStatement stmt = con.prepareStatement(sql);
                            stmt.setLong(1, id);
                            
                            ResultSet res = stmt.executeQuery();
                            String xml = res.getString("data");
                            stmt.close();
                        
                            Document doc = reader.read(new StringReader(xml));
                            doc = treeWalk(doc);

                            sql = "UPDATE " + db + " SET data = ? WHERE id = ?";
                            stmt = con.prepareStatement(sql);
                            stmt.setString(1, doc.asXML());
                            stmt.setLong(2, id);
                            stmt.executeUpdate();
                            stmt.close();

                            sql = "INSERT INTO " + db + " VALUES(?)";
                            stmt = don.prepareStatement(sql);
                            stmt.setLong(1, id);
                            stmt.executeUpdate();
                            stmt.close();
                            done++;
                        }  
//                        } sleep(50);
                    } index++; }}
            catch (Exception ex) { ex.printStackTrace(); }
            dead++; }};
        th.setPriority(Thread.NORM_PRIORITY);
        th.start();
        return th;
    }
    
    static Element treeWalk(Element element) throws InterruptedException, Exception {
        for (int i = 0, size = element.nodeCount(); i < size; i++) {
            Node node = element.node(i);
            if (node instanceof Element) {
                String trans;
                String text = ((Element) node).getTextTrim();
                String strip = text.replaceAll(" ", "").replaceAll("\n","");
                
                if (!strip.equals("") && !isNumeric(strip) && text.length() > 5
                        && ((Element) node).elements().isEmpty()) {
                    trans = translate("en","el",text);
                    node.setText(trans);
                    
                    words += text.split(" ").length;
                    twords += trans.split(" ").length;
                    
                    text(trans, tfR);
                    text(text, tfL);
                }
                node = treeWalk((Element) node);
            } sleep(100);
        }
        return element;
    }
    
    static private String translate(String langFrom,
            String langTo, String word) throws Exception {
        String url = "https://translate.googleapis.com/translate_a/single?"+
                "client=gtx&"+
                "sl=" + langFrom + 
                "&tl=" + langTo + 
                "&dt=t&q=" + URLEncoder.encode(
                        word.replace(".", ""), "UTF-8");
        
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        
        in.close();
        return parseResult(response.toString());
    }
 
    static private String parseResult(String inputJson) throws Exception {
        JSONArray jsonArray = new JSONArray(inputJson);
        JSONArray jsonArray2 = (JSONArray) jsonArray.get(0);
        JSONArray jsonArray3 = (JSONArray) jsonArray2.get(0);
        return jsonArray3.get(0).toString();
    }
    
    static void text(String text, TextFlow tf) {
        Platform.runLater(()->{
            Text less = new Text(System.lineSeparator()+" "+text+" ");
            less.setFont(Font.font("Calibri", FontWeight.BOLD, 18));
            less.setFill(Color.web("#65709d"));
            less.setEffect(shadow);
            
            if (tf.getChildren().size()>20) tf.getChildren().remove(0);
            tf.getChildren().add(less);
        });
    }
    
    static boolean isNumeric(String s) {  
        return s != null && s.matches("[-+]?\\d*\\.?\\d+");  
    }  
}
    