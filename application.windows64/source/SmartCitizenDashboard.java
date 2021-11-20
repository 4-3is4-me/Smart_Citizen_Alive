import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.io.*; 
import java.net.*; 
import java.net.URL; 
import javax.net.ssl.HttpsURLConnection; 
import javax.net.ssl.SSLPeerUnverifiedException; 
import java.util.Calendar; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class SmartCitizenDashboard extends PApplet {

/*
Dashboard for Smart Citizen kit data by Tim Wornell,
with tutoring, code and inspiration from Lee at Plymouth College of Art as part of the Smart Citizen Programme June 2021.

# Click your mouse to see the data overlay and to change kit ID and the number of days history drawn by the hills, default is 3.
# Type a kit ID and use your up and down arrow keys to increase or decrease the number of days.
# Hit enter to confirm the choices.
# If a bad kit ID or one that is offline is entered, the kit will remain the same and only the number of days will be entered.
# Click your mouse again to hide the overlay.
# Enter authcode as first line of "cnfig.txt" in "data" folder, get this from your profile on smartcitizen.me
# Enter defaulty kitId as second line of "cnfig.txt" in data folder, get this from your profile on smartcitizen.me
*/

//// API class SmartCitizen
int kitID;
int lastkitID;
String Oauthcode;
int sensorID;
int rollup;
int numberofdays;
//// Object to pull in latest sensor readings
JSONObject readingslatest = new JSONObject();
//// dictionary for storing latest sensor readings from object
FloatDict sensorlatst = new FloatDict();

//// y points arrays for historical readings and x points array to draw the hills
IntDict sensornums = new IntDict();
int[] lightarr = new int[0];
int[] batteryarr = new int[0];
//int[] noisearr = new int[0];
int[] pressurearr = new int[0];
int[] humidityarr = new int[0];
int[] temperaturearr = new int[0];
int[] xpointsarr = new int[0];

//// for the sea movement doesn't like being local
float seay;
//// for the draw functions
boolean day = true;
boolean rain = false;
int skyred;
int skygreen;
int skyblue;
int skycol;

//// sensor variables, global.
float humidity;

/// Time
int updateinterval = 20;
int lastupdate;
String lastupdatelocal;
int sunupx;
int sundownx;
int hour;

///sun position for flare
float sunX;
float sunY;
float ex1 = 0;
float ey1 = 0;
float ex2 = 0;
float ey2 = 0;
float ex3 = 0;
float ey3 = 0;
float ex4 = 0;
float ey4 = 0;
float targetX = width*0.5f;
float targetY = height*0.66f;

//// infos
String city;
String country;

/// mouse! and input
boolean mouse = false;
String typing = "";
int newdays = 3;

public void setup(){
  
  
  /// edit this list for new sensors or if sensor ids ever change
  sensornums.set("TVOC", 113);
  sensornums.set("CO2", 112);
  sensornums.set("Light", 14);
  sensornums.set("Battery", 10);
  sensornums.set("Noise", 53);
  sensornums.set("Pressure", 58);
  sensornums.set("PM1", 89);
  sensornums.set("PM2.5", 87);
  sensornums.set("PM10", 88);
  sensornums.set("Humidity", 56);
  sensornums.set("Temperature", 55);
  
  
  String datapath = dataPath("");
  String fullpath = datapath + "\\cnfig.txt";
  String[] config = loadStrings(fullpath);
  Oauthcode = config[0];
  kitID = PApplet.parseInt(config[1]);
  rollup = 4; // average every 4 hours for history data
  numberofdays = 3;
  
  // for the flare
  ex1 = targetX;
  ey1 = targetY-5;
  ex2 = targetX;
  ey2 = targetY-5;  
  ex3 = targetX;
  ey3 = targetY-5;
  ex4 = targetX;
  ey4 = targetY-5;
  
  fill(0);
  rect(0, 0, width, height);
  fill(255);
  textSize(height*0.10f);
  textAlign(CENTER);
  text("Smart Citizen Data Loading...", width*0.5f, height*0.5f);
  
  getlatst();
  getinfo();
  gethills();
}

public void draw(){
  
  if (millis() - lastupdate > (updateinterval*1000)) {
    getlatst();
    getinfo();
    gethills();
  }
 
  drawsky();
  humidity = sensorlatst.get(str(sensornums.get("Humidity")));
  //println(humidity);
  //humidity = 99;
  if (humidity < 92){
    rain = false;
    drawsunormoon();
  }else{
    rain = true;
  }
 //// drawhill(sensor array, colour, colour, colour) -- Order is back to front, lightest to darkest
 //// drawgas(sensor, red, green, blue, mod)  -- mod changes the amount of screen it takes up
  drawhill(batteryarr, 175, 175, 175);  
  drawgas("CO2", 0, 255, 0, 30.0f); //green
  drawgas("TVOC", 255, 255, 255, 10.0f); // white
  drawgas("PM1", 255, 0, 255, 0.15f);  // pink
  drawgas("PM2.5", 255, 255, 0, 0.15f); // yellow
  drawgas("PM10", 0, 255, 255, 0.15f); //cyan
 
  drawhill(lightarr, 90, 130, 95);
  drawhill(temperaturearr, 60, 140, 85);
  drawhill(humidityarr, 30, 130, 55);
  drawhill(pressurearr, 25, 155, 47);
  drawsea();
 
  if (rain){
    drawrain();
  }else if(day){
  drawflare();
  }
  
  if (mouse){
    drawlabels();
  }
}

////// Function to call the lastest sensor readings from the api and put them in sensorlatst dict as (id:value) //////////
public void getlatst(){
    SmartCitizen myinfo = new SmartCitizen(kitID, Oauthcode);
    readingslatest = myinfo.getlatest();
    JSONObject data = readingslatest.getJSONObject("data");
    JSONArray sensors = data.getJSONArray("sensors");
    int len = sensors.size();
    for (int i = 0; i < len; i++){
      JSONObject obj = sensors.getJSONObject(i);
      sensorlatst.set(str(obj.getInt("id")), obj.getFloat("value"));
    }
  //println(sensorlatst.get(str(sensornums.get("Temperature"))));  // to get a sensor value
}
/////////// get info ///////////////
public void getinfo(){
  JSONObject data = readingslatest.getJSONObject("data");
  JSONObject location = data.getJSONObject("location");
  Float lat = location.getFloat("latitude");
  Float lon = location.getFloat("longitude");
  
  SmartCitizen myinfo = new SmartCitizen(kitID, Oauthcode);
  JSONObject times = myinfo.gettimes(lat, lon);  // from metaweather API
  String localtime = times.getString("time");
  String localsunrise = times.getString("sun_rise"); 
  String localsunset = times.getString("sun_set");
  
  city = location.getString("city");
  country  = location.getString("country");
  //tags in here
  
  String timestamp = data.getString("recorded_at");
  createtimes(timestamp, localtime, localsunrise, localsunset);

}

///////////////// create times //////////////
public void createtimes(String timestamp, String localtime, String localsunrise, String localsunset){
  //// taking the time difference out of the localtime from weather api
  String defchar;
  char tadjustplus_minus;
  String tadjust;
  try {
    tadjustplus_minus = localtime.charAt(26);
    char tadjust1 = localtime.charAt(27);
    char tadjust2 = localtime.charAt(28);
    tadjust = ""+tadjust1  + tadjust2;
  }
  catch (Exception Error) {
    // will fail if there is no time adjustment eg. in the UK in winter, so we make it add nothing.
    //println("error");
    defchar = "+";
    tadjustplus_minus = defchar.charAt(0);
    tadjust = "0";
  }  
  
  //// taking hours out of sunrise and sunset to set min and max sun x values
  char first = localsunrise.charAt(11);
  char second = localsunrise.charAt(12);
  String both = ""+first+second;
  sunupx = PApplet.parseInt(both);
  
  first = localsunset.charAt(11);
  second = localsunset.charAt(12);
  both = ""+first+second;
  sundownx = PApplet.parseInt(both);
  
  //// taking the hours and minutes out of the UTC time of last recorded data from smart citizen api
  first = timestamp.charAt(11);
  second = timestamp.charAt(12);
  both = ""+first+second;
  char firstmin = timestamp.charAt(14);
  char secondmin = timestamp.charAt(15);
  String bothmin = ""+firstmin+secondmin;
  int hrcheck;
  
  if (first+second == 23){
    hrcheck = 0;
  }else{
    hrcheck = PApplet.parseInt(both);
  }
  if (tadjustplus_minus=='+'){
    hour = hrcheck + PApplet.parseInt(tadjust);
    //println("adjust is plus");
  }else{
    hour = hrcheck - PApplet.parseInt(tadjust);
    //println("adjust is minus");
  }

  lastupdatelocal = hour+":"+bothmin;
  
  // checking if it's daytime
  if (hour >= sunupx && hour <= sundownx){
    day = true;
  }else{
    day = false;
  }
  //println(lastupdatelocal);
  //println(sunupx);
  //println(sundownx);
  //day = false;
  //hour = 22;
}

////////////////////// function to get historical api data for each sensor and make y arrays and an x array //////////////
public void gethills(){
    SmartCitizen myinfo = new SmartCitizen(kitID, Oauthcode);
    //// makeys(sensor, rollup, numberofdays, map(low value, high value) maps to  >> height*value, height*value
    lightarr = makeys("Light", rollup, numberofdays, 0, 15000, 0.65f, 0.55f);
    batteryarr = makeys("Battery", rollup, numberofdays, 20, 100, 0.65f, 0.50f);  
    //noisearr = makeys("Noise", rollup, numberofdays, 0, 100, 0.95, 0.75);
    pressurearr = makeys("Pressure", rollup, numberofdays, 95, 104, 1, 0.90f);
    humidityarr = makeys("Humidity", rollup, numberofdays, 50, 100, 0.95f, 0.65f);
    temperaturearr = makeys("Temperature", rollup, numberofdays, -10, 30, 0.90f, 0.55f);
    
    //making the array of x values
    int len = batteryarr.length;
    xpointsarr = expand(xpointsarr, len);
    xpointsarr[0] = -(width/len);  ///first curve vertex point left of the screen by width / number of points in arrray to try to flatten the curve at the edges
    for ( int i = 1; i < (len-1); i++){
      xpointsarr[i] = (width/len)*i;
    }
    xpointsarr[len-1] = width+20;
    lastupdate = millis();
}
//////////////// function to make y values /////////////
public int[] makeys(String sensorq, int rollupq, int numberofdaysq, int low, int high, float hhigh, float hlow){
  int[] returnys = new int[0];
  
  // Pulling the readings from API
  SmartCitizen myinfo = new SmartCitizen(kitID, Oauthcode);
  JSONArray hist = myinfo.gethistory(sensornums.get(sensorq), rollupq, numberofdaysq);
  int len = hist.size();
  returnys = expand(returnys, len);
  
  // making the array of y values 
  for (int i = 0; i < len; i++){
    JSONArray readingi = hist.getJSONArray(i);
    Float apoint = readingi.getFloat(1);
    Float mappedpoint = map(apoint, low, high, height*hhigh, height*hlow);  //needs a different map for each sensor
    returnys[i] = PApplet.parseInt(mappedpoint);    
  }
  returnys = reverse(returnys);
  return returnys;
}

/////////////// draw the sky ////////////////
public void drawsky(){
  float pressure = sensorlatst.get(str(sensornums.get("Pressure")));
  float light = sensorlatst.get(str(sensornums.get("Light")));
  //float pressure = 100;
  //println(pressure);
  //float light = 0;
  int darkness = PApplet.parseInt(light);
  skyred = 120;
  skygreen = 225;
  skyblue = 240 + darkness/4;
  
  if (pressure < 101.8f){
    // grey sky
    skyred = skygreen;
  }
  if (!day){
    // night time
    darkness = 200 - darkness;
    skyred = skyred - darkness;
    skygreen = skygreen - darkness;
    skyblue = skyblue - darkness;
  }
  skycol = color(skyred, skygreen, skyblue);
  background(skycol);
}
//////////// draw sun or moon ////////////
public void drawsunormoon(){
  if (day){
    drawsun();
  }else{
    drawmoon();
  }
    
}

//////////// draw sun /////////////
public void drawsun() {
  int hrsinday = sundownx - sunupx;
  float light = sensorlatst.get(str(sensornums.get("Light")));
  if (hour <= (sundownx - hrsinday*0.5f)){  //midday
    //sun going up
    sunY = (height*0.5f) - (hour-sunupx)*(height*0.5f/hrsinday);
  }else{
    // sun going down
    sunY = (height*0.5f) - (hrsinday-(hour-sunupx))*(height*0.5f/hrsinday);
  }
  sunX = (hour - sunupx) * (width / hrsinday);
                                              // consider splitting screen by 16 and using sunrise and sunset to reflect different rise and set positions for x??
  
  //// sun rings, numbers change the colours
  int  sun1 = color(225, 225, 255);
  int  sun2 = color(skyred + 20, skygreen + 20, skyblue + 20);  /// +40 is darker
  //color  sun2 = color(skycol);
  int  sun3 = color(skycol + 150, 0); //  + is lighter
  float sunshine = PApplet.parseInt(light)/1000;

  noStroke();
  PImage circle_grad = createImage(width, height, ARGB);
  circle_grad.loadPixels();
  int rgb;

  for (int y=0; y<circle_grad.height; y++) {
    for (int x=0; x<circle_grad.width; x++) {

    float dis = dist(x, y, sunX, sunY);
    dis = map(dis, 0, circle_grad.width, 0, 7 - sunshine);  // n - sunshine is size of blurry circle

   if (dis<2) {
       if (dis>1) {
          rgb = lerpColor(sun2, sun3, dis-1);
        } else {
          rgb = lerpColor(sun1, sun2, dis);
        }
       } else {
        rgb=0x00000000;
       }
      circle_grad.pixels[x+circle_grad.width*y]=rgb;
    }
  }

  image(circle_grad, 0, 0);
  
  //// sun temp
  float temperature = sensorlatst.get(str(sensornums.get("Temperature")));
  float red = temperature - 25;
  int  moon1 = color(255,255-(red*25),255-(temperature*10),150);
  int  moon2 = color(255,255,255,0);
  noStroke();
  float radius = height/10;

  for (float r = radius; r > 0; r--) {
   float inter = map(r, 0,height/10, 0, 1); 
   int c = lerpColor(moon1, moon2, inter); 
   fill(c,50);
   ellipse(sunX, sunY, r+random(60), r+random(60));
  }
  
  //// sun elipse
  noStroke();
  fill(255, 80);
  float sunsize = 0.06f;
  ellipse(sunX, sunY, height*sunsize, height*sunsize);
}

///////////// draw moon - only shows till midnight as it moves quicker than the sun!! ////////////
public void drawmoon(){
  int moonhrs = (sunupx + 24 - sundownx)/2; // half the night
  float moonY;
  if (hour <= 24 - moonhrs/2){  // before  halfway to midnight
    //moon going up
    moonY = (height*0.5f) - (hour - sundownx)*(height*0.5f/moonhrs);   ///to do!
  }else if (hour < 24){
    // moon going down
    moonY = (height*0.5f) - (hour - sundownx)*(height*0.5f/moonhrs);
  }else{  //after midnight
    return;  // exit drawmoon()
  }
  float moonX = (hour - sundownx) * (width*0.75f / moonhrs);
  float temperature = sensorlatst.get(str(sensornums.get("Temperature")));
  float red = temperature - 25;
  
  /// moon 
  noStroke();
  fill(255, 255-(red*25),255-(temperature*8),255);  //255 - g*90
  ellipse(moonX, moonY, height/25, height/25);
  fill(skycol);
  ellipse(moonX-width/140, moonY-3, height/25, height/25);   //-width/140

/*  //// moon temp
  color  moon1 = color(255,255-(red*25),255-(temperature*10),150);
  color  moon2 = color(skycol,0);
   
  noStroke();

  float radius = (height/10);//*g;

  for (float r = radius; r > 0; r--) {
   float inter = map(r, 0,(height/10)*g, 0, 1); 
   color c = lerpColor(moon1, moon2, inter); 
   fill(c,10-(g*2));
   ellipse(moonX+height/100, moonY+height/100, r, r);
    
  }*/
  
}
/////////// draw gas ////////////////////
public void drawgas(String sensorname, int red, int green, int blue, float mod){
  float gas = sensorlatst.get(str(sensornums.get(sensorname)));
  int gas0 = color(red, green, blue);
  int gas2 = color(red, green, blue,0);
  float gradientSteps = 30;
  float gradientHeight = (gas/mod)/gradientSteps;
  
  noStroke();
  for(int i = 0; i < gradientSteps; i++){
    float t = map(i,0,gradientSteps,0.0f,1.0f);
    int interpolatedColor = lerpColor(gas0,gas2,t); 
    fill(interpolatedColor, 10);
    rect(0,(height*0.67f)-i*gradientHeight,width,(height/200)-(gas/mod)); // 200
  }
}

/////////// function to draw the hills ///////////////
public void drawhill(int[] ys, int red, int green, int blue){
  fill(red, green, blue);
  beginShape();
  noStroke();
  int numberofvertex = ys.length;
   
  beginShape();
  curveVertex(-(width/numberofvertex),height);  //first is to be duplicated as per instructions...
  curveVertex(-(width/numberofvertex),height);
  for (int i = 0; i < numberofvertex; i++){
    curveVertex(xpointsarr[i], ys[i]);
  }
  curveVertex(width+(width/numberofvertex),height);  //last is to be duplicated too
  curveVertex(width+(width/numberofvertex),height);
  endShape();
}

//// function to draw the 'sea' from noise value ////////
public void drawsea(){
  float noiseadj = sensorlatst.get(str(sensornums.get("Noise")))/1000;  // this one is the noise value not the noise function...
  noStroke();
  fill(30,90,255);  // sea colour 30,120,230
  beginShape();
  float seax = seay;     
  for (float x = 0; x <= width+20; x += 10) {
    float y = map(noise(seax,seay), 0, 1, height*0.99f, height*0.90f);  // this one is the noise function
    vertex(x, y); 
    seax += noiseadj;
  }
  seay += noiseadj/10;       //0.008;
  vertex(width, height);
  vertex(0, height);
  endShape(CLOSE);
   
}

/////////// draw rain ///////////
public void drawrain(){
  strokeWeight(random(2));
  float rain = humidity - 80;
 
  for (int i = 0; i < rain; i++) {
   float rX = random(width);
   float rY = random(height);
   float rL = rY+random(rain*8)+rain;
   stroke(random(255), random(255));
   line(rX, rY, rX, rL);
  } 
}

/////////////// draw flare /////////////
public void drawflare(){
  float speed1 = 0.01f;
  float speed2 = 0.01f;
  float speed3 = 0.01f;
  float speed4 = 0.01f;
  float size1 = 2;
  float size2 = 4;
  float size3 = 10;
  int  flare1 = color(0,255,0,100);
  int  flare2= color(100+random(150), 100+random(150), 100+random(150),100);
  int  flare3 = color(255, 255, 100,0);

  ex1 = lerp(ex1,targetX, speed1);
  ey1 = lerp(ey1,targetY, speed1);
  ex2 = lerp(ex2,targetX, speed2/2);
  ey2 = lerp(ey2,targetY, speed2/2);
  ex3 = lerp(ex3,targetX, speed3/3);
  ey3 = lerp(ey3,targetY, speed3/3);
  ex4 = lerp(ex4,targetX, speed4/4);
  ey4 = lerp(ey4,targetY, speed4/4);

  //stroke(150+random(100), 150+random(100), 0, 20);
  fill(200+random(50), 200+random(50), 255, 15);
  //strokeWeight(3);
  ellipse(ex1,ey1,ey1/size1,ey1/size1);
  
  float radius = (height/20);

  for (float r = radius; r > 0; r--) {
    float inter = map(r, 0, (height/20), 0, 1);
    int c = lerpColor(flare2, flare3, inter); 
    int c2 = lerpColor(flare1, flare1, inter);
    noStroke();
    fill(c, 30);
    ellipse(ex2,ey2,ey2/r/1.4f,ey2/r/1.4f);
    fill(c2, 20);
    ellipse(ex3,ey3,ey3/r/8,ey3/r/8);
    fill(c2, 25);
    ellipse(ex4,ey4,ey4/r/8,ey4/r/8);
  }
  
  if (ey1<sunY+50 || ey2<sunY+50){
   targetX = width/2;
   targetY = height;
   size1 =random(10)+5;
   size2 =random(20)+5;
   speed1=random(0.01f);
   speed2=random(0.05f);
   speed3=random(0.01f);
   speed4=random(0.05f);
   flare1 = color(200+random(50), 200+random(50), 0, 150);
   flare2= color(100+random(150), 100+random(150), 100+random(150), 10+random(100));
  }
  
 if (ey1>height-5 || ey2>height-15){
   targetX = sunX;
   targetY = sunY;
   size1 =random(10)+5;
   size2 =random(20)+5;
   speed1=random(0.01f);
   speed2=random(0.01f);
   speed3=random(0.05f);
   speed4=random(0.01f);
   flare1 = color(0, 200+random(50), 200+random(50), 100); 
   flare2= color(100+random(150), 100+random(150), 100+random(150), 10+random(100));
  }
  
   if (ey3<sunY+70 || ey3<sunY+50){
   size1 =random(10)+5;
   size2 =random(20)+5;
   speed1=random(0.05f);
   speed2=random(0.01f);
   speed3=random(0.05f);
   speed4=random(0.05f);
   flare1 = color(150+random(100), 150+random(100), 150+random(100), 100);
   flare2= color(100+random(150), 100+random(150), 100+random(150), 10+random(100));
  }
  
 if (ey3>height-5 || ey4>height-5){
   size2 =random(10)+5;
   speed1=random(0.01f);
   speed2=random(0.05f);
   speed3=random(0.01f);
   speed4=random(0.01f);
   flare1 = color(200+random(50), 100+random(50),100+random(50), 100);
   flare2= color(100+random(150), 100+random(150), 100+random(150), 10+random(100));
  }

}
///////////// called when the mouse is pressed //////////////
public void mousePressed(){
  if (!mouse){
    mouse = true;
  }else{
    mouse = false;
  }
}

///////// called when a key is typed ////////////
public void keyPressed() {
  if (mouse){
    // If the return key is pressed, save the String and clear it
    if (key == ENTER) {
      lastkitID = kitID;
      kitID = PApplet.parseInt(typing);
      if (kitID == 0) {
        kitID = lastkitID;
      }  
      numberofdays = newdays;
      try{
        getlatst();
        getinfo();
        gethills();
      }catch (Exception error){
        text("***********", width*0.05f,height*0.05f);
        kitID = lastkitID;
        getlatst();
        getinfo();
        gethills();
      }
      // A String can be cleared by setting it equal to ""
      typing = ""; 
    }else if(key == DELETE || key  == BACKSPACE){
      typing = "";
    }else if(keyCode == UP){
      newdays = newdays +2;
      if (newdays > 10){
        newdays = 3;
      }
    }else if (keyCode == DOWN){
      newdays = newdays -2;
      if (newdays < 3){
        newdays = 3;
      }
    }else{
      // Otherwise, concatenate the String
      // Each character typed by the user is added to the end of the String variable.
      typing = typing + key;
      if ( typing.length() > 5){
        typing = "";
      }
    }
  }
}
/////////// draws the labels and text ////////
public void drawlabels(){
  String period;
  if (hour >= 12){
    period = "pm";
  }else{
    period = "am";
  }
  textSize(height*0.02f);
  fill(255);
  textAlign(CENTER);
  text(city+", "+country, width/2, height*0.05f);
  text("Latest Data from : "+lastupdatelocal+" "+period+" : CO2 "+sensorlatst.get(str(sensornums.get("CO2")))+" ppm"+" : VOC "+sensorlatst.get(str(sensornums.get("TVOC")))+" ppb"+" : PM10 "+sensorlatst.get(str(sensornums.get("PM10")))+" ug/m3"+" : PM2.5 "+sensorlatst.get(str(sensornums.get("PM2.5")))+" ug/m3"+" : PM1 "+sensorlatst.get(str(sensornums.get("PM1")))+" ug/m3", width/2, height*0.07f);
  textAlign(LEFT);
  text("Noise", width*0.92f, height*0.97f);
  text(sensorlatst.get(str(sensornums.get("Noise")))+" Db", width*0.92f, height*0.99f);
  text("Pressure", width*0.92f, height*0.89f);
  text(sensorlatst.get(str(sensornums.get("Pressure")))*10+" mPa", width*0.92f, height*0.91f);
  text("Humidity", width*0.92f, height*0.81f);
  text(sensorlatst.get(str(sensornums.get("Humidity")))+" %", width*0.92f, height*0.83f);
  text("Temperature", width*0.92f, height*0.73f);
  text(sensorlatst.get(str(sensornums.get("Temperature")))+" C", width*0.92f, height*0.75f);
  text("Light", width*0.92f, height*0.65f);
  text(sensorlatst.get(str(sensornums.get("Light")))+" Lux", width*0.92f, height*0.67f);
  text("Battery", width*0.92f, height*0.57f);
  text(sensorlatst.get(str(sensornums.get("Battery")))+" %", width*0.92f, height*0.59f);
  
  text("Enter a Kit ID : "+typing, width*0.05f, height*0.05f);
  text("Number of days : "+newdays, width*0.05f, height*0.07f);
}








class SmartCitizen { 
  String myauthtoken; 
  int mykitID;
  int mysensorID;
  int myrollup;
  int mynumberofdays;

  SmartCitizen(int kitID, String authtoken){
    myauthtoken = authtoken;
    mykitID = kitID;
  }
  
  
/////////////////////// gets the history of a sensor averages every rollup hours over the last numberofdays //////////////////////////

  public JSONArray gethistory(int sensorID, int rollup, int numberofdays){
    mysensorID = sensorID;
    myrollup = rollup;
    mynumberofdays = numberofdays;
    
    //object for the calendar  - See https://docs.oracle.com/javase/7/docs/api/java/util/Calendar.html
    Calendar mycal = Calendar.getInstance();
    // setting the dates as the Calendar is unreliable - giving the wrong dates sometimes!
    mycal.set(Calendar.YEAR, year());
    mycal.set(Calendar.MONTH, month());
    mycal.set(Calendar.DAY_OF_MONTH, day());
    
    // making the enddate string for tomorrow - which calls the latest readings from today
    int chkday = mycal.get(Calendar.DAY_OF_MONTH);
    mycal.roll(Calendar.DAY_OF_MONTH, 1);   // increment the day by 1 to get to tomorrow
    int tomorrow = mycal.get(Calendar.DAY_OF_MONTH);
    if (tomorrow == 1){                // if tomorrow is a new month
      mycal.roll(Calendar.MONTH, 1);   // increment the month by 1
      int newmonth = mycal.get(Calendar.MONTH);
      if (newmonth == 1){               // if new month is January
        mycal.roll(Calendar.YEAR, 1);  // increment the year by 1
      }
    }
    String myenddate = str((mycal.get(Calendar.YEAR)))+"-"+str((mycal.get(Calendar.MONTH)))+"-"+str((mycal.get(Calendar.DAY_OF_MONTH)));
    //println("End date "+myenddate);
    
    // Making the start date string for today less numberofdays
    mycal.roll(Calendar.DAY_OF_MONTH, -(numberofdays+1));  // taking an extra 1 away as we already incremented the calendar day above
    int strday = mycal.get(Calendar.DAY_OF_MONTH);
    if ( strday > chkday){            // if we have rolled the calender back to the end of the last month
      mycal.roll(Calendar.MONTH, -1);  // decrement the month by 1
      int oldmonth = mycal.get(Calendar.MONTH);
      if (oldmonth == 12){              // if we have rolled back to Decemeber
        mycal.roll(Calendar.YEAR, -1);    // decrement the year by 1
      }
    }
    String mystartdate = str((mycal.get(Calendar.YEAR)))+"-"+str((mycal.get(Calendar.MONTH)))+"-"+str((mycal.get(Calendar.DAY_OF_MONTH)));
    //println("start date "+mystartdate);
    
    
    JSONObject src = getObjectFromAPI("https://api.smartcitizen.me/v0/devices/"+mykitID+"/readings?sensor_id="+mysensorID+"&rollup="+myrollup+"h&from="+mystartdate+"&to="+myenddate+myauthtoken);
    JSONArray reply = src.getJSONArray("readings");
    return reply;
  }
  
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

///////////////////////////////// gets the latest readings from all sensors /////////////////////////////////////////////////  

  public JSONObject getlatest(){
    JSONObject dataobject = getObjectFromAPI("https://api.smartcitizen.me/v0/devices/"+mykitID+myauthtoken);
    return dataobject;
  }
  
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  
  public JSONObject gettimes(float lat, float lon){
    JSONArray query = getArrayFromAPI("https://www.metaweather.com/api/location/search/?lattlong="+lat+","+lon);
    JSONObject result = query.getJSONObject(0);
    int woe = result.getInt("woeid");
    JSONObject request = getObjectFromAPI("https://www.metaweather.com/api/location/"+woe);
    return request;
  }


///////////////////////// Below are the calls to the API ///////////////////////////  
  
  public JSONArray getArrayFromAPI(String urlsite){
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
          BufferedReader in = new BufferedReader
          (new InputStreamReader(connection.getInputStream()));
        }
        catch (IOException e) {
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
  
  public JSONObject getObjectFromAPI(String urlsite){
    JSONObject srcreadings = new JSONObject();
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
          BufferedReader in = new BufferedReader
          (new InputStreamReader(connection.getInputStream()));
        }
        catch (IOException e) {
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
  public void settings() {  fullScreen(); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "--present", "--window-color=#666666", "--hide-stop", "SmartCitizenDashboard" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}