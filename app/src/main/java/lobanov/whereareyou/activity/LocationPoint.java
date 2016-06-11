package lobanov.whereareyou.activity;

/**
 * Created by Aero on 11.06.2016.
 */
public class LocationPoint {
    public String name;
    public Double lat;
    public Double lon;
    public String date;
    public  LocationPoint(){
        name = "";
        lat = 0.0;
        lon = 0.0;
        date = "";
    }
    public LocationPoint(String _name,Double _lat, Double _lon, String _date){
        name = _name;
        lat = _lat;
        lon = _lon;
        date = _date;
    }
}
