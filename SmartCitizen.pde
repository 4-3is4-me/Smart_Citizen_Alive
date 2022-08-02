import java.io.*;
import java.net.*;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.util.Calendar;


class SmartCitizen {  
  int mykitID;
  int mysensorID;
  int myrollup;
  int mynumberofdays;

  SmartCitizen(int kitID){
    mykitID = kitID;
  }
  
  
/////////////////////// gets the history of a sensor averages every rollup hours over the last numberofdays //////////////////////////

  JSONArray gethistory(int sensorID, int rollup, int numberofdays){
    mysensorID = sensorID;
    myrollup = rollup;
    mynumberofdays = numberofdays;
    
    //object for the calendar  - See https://docs.oracle.com/javase/7/docs/api/java/util/Calendar.html
    // DAY_OF_MONTH is 1 to 28, 29, 30 or 31
    // MONTH is 0 to 11
    Calendar mycal = Calendar.getInstance();

    // making the enddate string for tomorrow - which calls the latest readings from today
    int chkday = mycal.get(Calendar.DAY_OF_MONTH);
    mycal.roll(Calendar.DAY_OF_MONTH, 1);   // increment the day by 1 to get to tomorrow
    int tomorrow = mycal.get(Calendar.DAY_OF_MONTH);
    if (tomorrow == 1){                // if tomorrow is a new month
      mycal.roll(Calendar.MONTH, 1);   // increment the month by 1
      int newmonth = mycal.get(Calendar.MONTH);
      if (newmonth == 0){               // if new month is January (0)
        mycal.roll(Calendar.YEAR, 1);  // increment the year by 1
      }
    }
    // Add 1 to the month to change from 0-11 to 1-12
    String myenddate = str((mycal.get(Calendar.YEAR)))+"-"+str((mycal.get(Calendar.MONTH)+1))+"-"+str((mycal.get(Calendar.DAY_OF_MONTH)));
    //println("End date "+myenddate);
    
    // Making the start date string for today less numberofdays
    mycal.roll(Calendar.DAY_OF_MONTH, -(numberofdays+1));  // taking an extra 1 away as we already incremented the calendar day above
    int strday = mycal.get(Calendar.DAY_OF_MONTH);
    if (strday > chkday){            // if we have rolled the calender back to the end of the last month
      mycal.roll(Calendar.MONTH, -1);  // decrement the month by 1
      int oldmonth = mycal.get(Calendar.MONTH);
      if (oldmonth == 11){              // if we have rolled back to Decemeber
        mycal.roll(Calendar.YEAR, -1);    // decrement the year by 1
      }
    }
    // Add 1 to the month to change from 0-11 to 1-12
    String mystartdate = str((mycal.get(Calendar.YEAR)))+"-"+str((mycal.get(Calendar.MONTH)+1))+"-"+str((mycal.get(Calendar.DAY_OF_MONTH)));
    //println("start date "+mystartdate);
    
    
    JSONObject src = getObjectFromAPI("https://api.smartcitizen.me/v0/devices/"+mykitID+"/readings?sensor_id="+mysensorID+"&rollup="+myrollup+"h&from="+mystartdate+"&to="+myenddate);
    JSONArray reply = src.getJSONArray("readings");
    return reply;
  }
  
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

///////////////////////////////// gets the latest readings from all sensors /////////////////////////////////////////////////  

  JSONObject getlatest(){
    JSONObject dataobject = getObjectFromAPI("https://api.smartcitizen.me/v0/devices/"+mykitID);
    return dataobject;
  }
  
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  /*
  JSONObject gettimes(float lat, float lon){
    JSONArray query = getArrayFromAPI("https://www.metaweather.com/api/location/search/?lattlong="+lat+","+lon);
    JSONObject result = query.getJSONObject(0);
    int woe = result.getInt("woeid");
    JSONObject request = getObjectFromAPI("https://www.metaweather.com/api/location/"+woe);
    return request;
  }
  */
  JSONObject gettimezone(float lat, float lon){
    //long timestamp = cal.getTimeInMillis()/1000;
    //long timestamp = System.currentTimeMillis()/1000;
    JSONObject query = getObjectFromAPI(
      "https://timezones-api.datasette.io/timezones/by_point.json?longitude="+lon+"&latitude="+lat);
    return query;
  }
///////////////////////// Below are the calls to the API ///////////////////////////  
  
  JSONArray getArrayFromAPI(String urlsite){
    JSONArray srcreadings = new JSONArray();
    StringBuilder response = new StringBuilder();

    try {
      // Create a URL object
      URL url = new URL(urlsite);
      try {
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        try {
          connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
          connection.setRequestProperty("Accept", "application/json");
          connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.56 Safari/535.11"); // Read all of the text returned by the HTTP server
          //BufferedReader in = new BufferedReader
          //(new InputStreamReader(connection.getInputStream()));
        }
        catch (Error e) {
        println(e);
        }
      int status = connection.getResponseCode();
      InputStream in;
      if (status >= 400)
          in = connection.getErrorStream();
        else
          in = connection.getInputStream();
          BufferedReader rd = null;
          try {
            rd = new BufferedReader(new InputStreamReader(in));
            String responseSingle = null;
            while ((responseSingle = rd.readLine()) != null) {
              response.append(responseSingle);
            }
            JSONArray apiresponse = JSONArray.parse(response.toString());
            String message = apiresponse.getString(0, "No message");
          
            srcreadings = apiresponse;
          
            if (status >= 400) {
              println(" > Error (" + status + "): " + message);
            } else {
              //println(" > " + message);
          }
        }  
        catch (IOException e) {
          println(" > Error: " + e);
        }
      }  
      catch (IOException e) {
        println(" > Error: " + e);
      }
    }  
    catch (IOException e) {
      println(" > Error: " + e);
    }
    return srcreadings;
  }
  
  JSONObject getObjectFromAPI(String urlsite){
    JSONObject srcreadings = new JSONObject();
    StringBuilder response = new StringBuilder();

    try {
      // Create a URL object
      URL url = new URL(urlsite);
      try {
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        try {
          connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
          connection.setRequestProperty("Accept", "application/json; charset=utf-8");
          connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.56 Safari/535.11"); // Read all of the text returned by the HTTP server
          //BufferedReader in = new BufferedReader
          //(new InputStreamReader(connection.getInputStream()));
        }
        catch (Error e) {
        println(e);
        }
      int status = connection.getResponseCode();
      InputStream in;
      if (status >= 400)
          in = connection.getErrorStream();
        else
          in = connection.getInputStream();
          BufferedReader rd = null;
          try {
            rd = new BufferedReader(new InputStreamReader(in));
            String responseSingle = null;
            while ((responseSingle = rd.readLine()) != null) {
              response.append(responseSingle);
            }
            JSONObject apiresponse = JSONObject.parse(response.toString());
            String message = apiresponse.getString("message", "No message");
          
            srcreadings = apiresponse;
          
            if (status >= 400) {
              println(" > Error (" + status + "): " + message);
            } else {
              //println(" > " + message);
          }
        }  
        catch (IOException e) {
          println(" > Error: " + e);
          
        }
      }  
      catch (IOException e) {
        println(" > Error: " + e);
      }
    }  
    catch (IOException e) {
      println(" > Error: " + e);
    }
    return srcreadings;
  }
}
