package jarloader;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    ListView<JarData> jarListView;
    @FXML
    GridPane grid;
    @FXML
    Label jarNameLbl;
    @FXML
    Hyperlink jarLinkLbl;
    @FXML
    Hyperlink jarDownloadLbl;
    @FXML
    Button redownloadBtn;
    @FXML
    Button runBtn;
    @FXML
    Hyperlink combinatorLink;
    @FXML
    TextField searchBar;

    private ObservableList<JarData> jarData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //coloring and outlines
        grid.setGridLinesVisible(true);
        setLinkListener(jarLinkLbl);
        setLinkListener(jarDownloadLbl);
        setLinkListener(combinatorLink);
        setSearchBar(searchBar, jarListView);
        setListListener();
        rePull();
    }

    public void rePull(){
        //clear current listview and labels
        searchBar.setText("");
        jarListView.getItems().clear();
        jarNameLbl.setText("");
        jarLinkLbl.setText("");
        jarDownloadLbl.setText("");
        jarData = FXCollections.observableArrayList();
        try {
            URL url = new URL("https://api.github.com/repos/dandaman2/Solitaire_Jars/contents");

            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
            for (String line; (line = reader.readLine()) != null;) {
                //System.out.println(line);
                //got the jars, now parse into json
                JSONParser parser = new JSONParser();
                JSONArray array = (JSONArray)parser.parse(line);
                //parse JSON objects into JarDatas
                for (Object o : array) {
                    JSONObject obtainedJSON = (JSONObject) o;
                    String rawUrlString = obtainedJSON.get("download_url").toString();
                    String jarName = obtainedJSON.get("name").toString();
                    String locationUrl = obtainedJSON.get("html_url").toString();
                    if(rawUrlString.endsWith(".jar")){
//                        System.out.println(jarName);
//                        System.out.println(rawUrlString);
//                        System.out.println(locationUrl);
                        jarData.add(new JarData(jarName, locationUrl, rawUrlString));
                    }
                }
                //add JarDatas to list
                for(JarData jar : jarData){
                    jarListView.getItems().add(jar);
                }
            }
        }catch(Exception e){
            System.out.println("err in getting info");
            e.printStackTrace();
        }
    }

    public void startJar(){
        if(!jarDownloadLbl.getText().equals("") || jarDownloadLbl.getText() != null){
            try {
                System.setSecurityManager(new PreventExitSecurityManager());
                JarRunner j = new JarRunner(jarDownloadLbl.getText());
            }catch(SecurityException s){
                s.printStackTrace();
            }
        }
    }

    private void setLinkListener(Hyperlink link){
        link.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                String text;
                if(link.getText().equals("Combinators.org")){
                    text = "https://github.com/combinators";
                }else{
                    text = link.getText();
                }
                link.setVisited(false);
                Desktop d = Desktop.getDesktop();
                try {
                    d.browse(new URI(text));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }

        });
    }

    private void setListListener(){
        jarListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<JarData>() {
            @Override
            public void changed(ObservableValue<? extends JarData> observable, JarData oldValue, JarData newValue) {
                try {
                    // update labels
                    jarNameLbl.setDisable(false);
                    jarNameLbl.setText(newValue.getName());
                    jarLinkLbl.setDisable(false);
                    jarLinkLbl.setText(newValue.getLocationUrl());
                    jarDownloadLbl.setDisable(false);
                    jarDownloadLbl.setText(newValue.getDownloadUrl());
                }catch(NullPointerException npe){
                    //Recallibrate for searching
                }


            }
        });
    }

    private void setSearchBar(TextField searchbar, ListView<JarData> listView) {
        searchbar.textProperty().addListener(new ChangeListener() {
            public void changed(ObservableValue observable, Object oldVal,
                                Object newVal) {
                search(listView, (String) oldVal, (String) newVal);
            }
        });
    }


    public void search(ListView<JarData> list, String oldVal, String newVal) {
        if (oldVal != null && (newVal.length() < oldVal.length())) {
            list.setItems(jarData);
        }
        String value = newVal.toLowerCase();
        ObservableList<JarData> subentries = FXCollections.observableArrayList();
        for (JarData entry : list.getItems()) {
            String entryText = entry.getName().toLowerCase();
            if (entryText.contains(value) || findEditDistance(value, entryText)<3) {
                subentries.add(entry);
            }
        }
        list.setItems(subentries);
    }

    //Finds the edit distance between two strings
    private int findEditDistance(String searchedValue, String listEntryName){
        String regexString = "(?<=\\G.{";
        regexString += searchedValue.length() + "})";
        int min = -1;
        for(String chunk : listEntryName.split(regexString)){
            int newVal = Math.abs(EditDist.editDist(searchedValue, chunk,
                    searchedValue.length(), chunk.length()));
            if(min < 0 || newVal < min){
                min = newVal;
            }
        }
        return min;
    }
}
