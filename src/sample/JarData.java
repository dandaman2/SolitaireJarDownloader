package sample;

public class JarData {
    private String name;
    private String lUrl;
    private String dUrl;

    public JarData(String n, String lu, String du){
        name = n;
        lUrl = lu;
        dUrl = du;
    }

    public String getName(){
        return name;
    }

    public String getLocationUrl(){
        return lUrl;
    }

    public String getDownloadUrl(){
        return dUrl;
    }

    public String toString(){
        return name;
    }
}
